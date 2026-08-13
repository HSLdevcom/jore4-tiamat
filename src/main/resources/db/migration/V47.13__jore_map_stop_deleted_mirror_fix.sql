CREATE OR REPLACE FUNCTION update_jore_quay_extensions_table(inputQuayId BIGINT, inputQuayNetexId TEXT DEFAULT NULL)
  RETURNS jore_quay_extensions
  LANGUAGE 'plpgsql'
  VOLATILE AS $$
DECLARE
  actualQuayId BIGINT;
  actualQuayNetextId TEXT;
  keyValues resolve_current_jore_stop_key_values%ROWTYPE;
  transportLoopRecord RECORD;
  transportModes JSONB := '[]'::JSONB;
  activeTransportModes JSONB := '[]'::JSONB;
  saved jore_quay_extensions%ROWTYPE;
BEGIN
  --- Resolve the real DB id of the primary quay.
  SELECT resolve_actual_quay_db_id(inputQuayId, inputQuayNetexId) INTO actualQuayId;

  --- Early return if the input IDs were null.
  IF actualQuayId IS NULL THEN
    RETURN NULL;
  END IF;

  --- Resolve the Netex ID of the quay. Could be inputQuayNetexId
  SELECT netex_id
  INTO actualQuayNetextId
  FROM quay
  WHERE id = actualQuayId;

  --- Resolve the raw varchar key value pairs of the quay.
  SELECT *
  INTO keyValues
  FROM resolve_current_jore_stop_key_values
  WHERE quayId = actualQuayId;

  --- Resolve all and active Transport Modes present on a Stop.
  FOR transportLoopRecord IN
    SELECT DISTINCT ON (sp.netex_id)
      sp.transport_mode AS mode,
      (COALESCE(viss.items, 'InOperation') = 'InOperation') AS active
    FROM stop_place AS sp
    INNER JOIN stop_place_max_version AS spmv ON spmv.id = sp.id --- Ignore deleted quays = on old version of StopPlace
    INNER JOIN stop_place_quays AS spq ON spq.stop_place_id = sp.id
    LEFT JOIN quay_key_values AS qkvm ON qkvm.quay_id = spq.quays_id AND qkvm.key_values_key = 'mirrors'
    LEFT JOIN value_items AS vim ON vim.value_id = qkvm.key_values_id
    LEFT JOIN quay_key_values AS qkvss ON qkvss.quay_id = spq.quays_id AND qkvss.key_values_key = 'stopState'
    LEFT JOIN value_items AS viss ON viss.value_id = qkvss.key_values_id
    WHERE spq.quays_id = actualQuayId OR vim.items = actualQuayNetextId
    ORDER BY sp.netex_id, sp.version DESC
  LOOP
    IF NOT transportModes ? transportLoopRecord.mode THEN
      transportModes := transportModes || TO_JSONB(transportLoopRecord.mode);
    END IF;

    IF transportLoopRecord.active AND NOT activeTransportModes ? transportLoopRecord.mode THEN
      activeTransportModes := activeTransportModes || TO_JSONB(transportLoopRecord.mode);
    END IF;
  END LOOP;

  --- Upsert by deleting existing one first.
  DELETE FROM jore_quay_extensions WHERE netex_id = actualQuayNetextId;
  INSERT INTO jore_quay_extensions VALUES (
    actualQuayId,
    actualQuayNetextId,

    keyValues.validity_start::DATE,
    keyValues.validity_end::DATE,
    keyValues.priority::INT,
    keyValues.stop_state::TEXT,

    transportModes,
    activeTransportModes,
    COALESCE(keyValues.trunk_line_stop::BOOLEAN, FALSE),
    COALESCE(keyValues.speed_tram_stop::BOOLEAN, FALSE),

    keyValues.functional_area::DOUBLE PRECISION,

    keyValues.street_address::TEXT,
    keyValues.ely_number::TEXT,
    keyValues.postal_code::TEXT,
    keyValues.stop_owner::TEXT,
    keyValues.timing_place_id::TEXT
  ) RETURNING * INTO saved;

  RETURN saved;
END;
$$;


--- Recalculate jore_quay_extensions table for quays that are or
--- were mirrored at some point.
DO $$
  DECLARE
    hybridQuay RECORD;
  BEGIN
    FOR hybridQuay IN (
      SELECT id
      FROM jore_quay_extensions
      WHERE jsonb_array_length(transport_modes) > 1
    ) LOOP
      PERFORM update_jore_quay_extensions_table(hybridQuay.id);
    END LOOP;
  END;
$$;


--- Create a trigger to update the Jore extensions table.
--- Trigger when a Quay gets deleted. = Exists on previous version of a StopPlace,
--- but not in the new version.
CREATE OR REPLACE FUNCTION update_jore_extensions_on_quay_delete()
  RETURNS TRIGGER
  LANGUAGE 'plpgsql'
  VOLATILE AS $$
DECLARE
  stopPlaceNetexId TEXT = NEW.netex_id;
  currentStopPlaceVersion BIGINT = NEW.version;
  previousStopPlaceVersion BIGINT;
  quayInStopPlace RECORD;
BEGIN
  --- Only apply to Jore StopPlaces/Quays (mainly to allow old Tiamat Junit tests to work.
  IF stopPlaceNetexId IS NULL OR NOT (stopPlaceNetexId LIKE 'HSL:StopPlace:%' OR stopPlaceNetexId LIKE 'FSR:StopPlace:%') THEN
    RETURN NULL;
  END IF;

  --- Resolve previous StopPalace version
  SELECT max(version) INTO previousStopPlaceVersion
  FROM stop_place
  WHERE netex_id = stopPlaceNetexId AND version < currentStopPlaceVersion;

  --- Loop over deleted quays
  FOR quayInStopPlace IN (
    --- Quays in previous StopPlace version
    SELECT p_q.netex_id
    FROM stop_place_quays AS p_spq
    INNER JOIN stop_place AS p_sp ON p_spq.stop_place_id = p_sp.id
    INNER JOIN quay AS p_q ON p_spq.quays_id = p_q.id
    WHERE p_sp.netex_id = stopPlaceNetexId AND p_sp.version = previousStopPlaceVersion

    EXCEPT --- Minus those that still exist

    ---- Quays in current StopPlace version
    SELECT c_q.netex_id
    FROM stop_place_quays AS c_spq
    INNER JOIN stop_place AS c_sp ON c_spq.stop_place_id = c_sp.id
    INNER JOIN quay AS c_q ON c_spq.quays_id = c_q.id
    WHERE c_sp.netex_id = stopPlaceNetexId AND c_sp.version = currentStopPlaceVersion
  ) LOOP
    PERFORM update_jore_quay_extensions_table(NULL, quayInStopPlace.netex_id);
  END LOOP;

  RETURN NULL;
END
$$;

CREATE CONSTRAINT TRIGGER update_jore_extensions_on_quay_delete
  AFTER INSERT ON stop_place
  INITIALLY DEFERRED DEFERRABLE
  FOR EACH ROW EXECUTE FUNCTION update_jore_extensions_on_quay_delete();

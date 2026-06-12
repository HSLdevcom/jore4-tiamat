CREATE OR REPLACE VIEW resolve_current_jore_stop_key_values AS
SELECT quay_id AS quayId,
       --- Key⋄Value pairs needed for the basic map operation.
       --- Replace max with any_value once we are using PostgreSQL version 16 or newer
       --- Stop validity
       max(vi.items) FILTER (WHERE qkv.key_values_key = 'validityStart')  AS validity_start,
       max(vi.items) FILTER (WHERE qkv.key_values_key = 'validityEnd')    AS validity_end,
       max(vi.items) FILTER (WHERE qkv.key_values_key = 'priority')       AS priority,
       max(vi.items) FILTER (WHERE qkv.key_values_key = 'stopState')      AS stop_state,

       --- Extra stop transport mode info
       max(vi.items) FILTER (WHERE qkv.key_values_key = 'trunkLineStop')  AS trunk_line_stop,
       max(vi.items) FILTER (WHERE qkv.key_values_key = 'speedTramStop')  AS speed_tram_stop,

       max(vi.items) FILTER (WHERE qkv.key_values_key = 'functionalArea') AS functional_area,

       --- Extra Key⋄Value pairs defined on the quay, used when filtering by search results.
       max(vi.items) FILTER (WHERE qkv.key_values_key = 'streetAddress')  AS street_address,
       max(vi.items) FILTER (WHERE qkv.key_values_key = 'elyNumber')      AS ely_number,
       max(vi.items) FILTER (WHERE qkv.key_values_key = 'postalCode')     AS postal_code,
       max(vi.items) FILTER (WHERE qkv.key_values_key = 'stopOwner')      AS stop_owner,
       max(vi.items) FILTER (WHERE qkv.key_values_key = 'timingPlaceId')  AS timing_place_id
FROM quay_key_values AS qkv
INNER JOIN value_items AS vi ON vi.value_id = qkv.key_values_id
GROUP BY quay_id;

COMMENT ON VIEW resolve_current_jore_stop_key_values IS
  'Helper view for jore_quay_extensions table: Lists details from the key⋄value store per quay version.';


--- This table contains most Jore related specialties needed to properly search and render
--- stops in Jore. While the quay DB id is the primary key, this table only contains an entry
--- for the latest version of each quay (unique Netex ID) and such can also be used to determine
--- the latest version of each quay. Raw DB id used to determine whether the table is upto-date.
--- In addition to the key⋄value pairs, this table also precalculates transport modes
--- for multimodal stops.
CREATE TABLE IF NOT EXISTS jore_quay_extensions (
  id                     BIGINT    NOT NULL PRIMARY KEY REFERENCES quay(id) ON DELETE CASCADE,
  netex_id               TEXT      NOT NULL UNIQUE,

  validity_start         DATE      NOT NULL,
  validity_end           DATE,
  priority               INT       NOT NULL DEFAULT 10,
  stop_state             TEXT      NOT NULL DEFAULT 'InOperation',

  transport_modes        JSONB     NOT NULL DEFAULT '[]'::JSONB,
  active_transport_modes JSONB     NOT NULL DEFAULT '[]'::JSONB,
  trunk_line_stop        BOOLEAN   NOT NULL DEFAULT FALSE,
  speed_tram_stop        BOOLEAN   NOT NULL DEFAULT FALSE,

  functional_area        DOUBLE PRECISION,

  street_address         TEXT,
  ely_number             TEXT,
  postal_code            TEXT,
  stop_owner             TEXT,
  timing_place_id        TEXT
);

COMMENT ON TABLE jore_quay_extensions IS
  'Helper table for handling stop details in jore: precalculates, parses, and indexes key⋄value store values for quay and tells which quay tiamat-version is latest.';


CREATE OR REPLACE FUNCTION resolve_actual_quay_db_id(inputQuayId BIGINT, inputQuayNetexId TEXT DEFAULT NULL)
  RETURNS BIGINT
  LANGUAGE 'plpgsql'
  VOLATILE AS $$
DECLARE
  quayId BIGINT := inputQuayId;
  mirroredQuayId BIGINT;
BEGIN
  --- Early return on null inputs
  IF inputQuayId IS NULL AND inputQuayNetexId IS NULL THEN
    RETURN NULL;
  END IF;

  --- Resolve the real DB id based on the inputs.
  IF inputQuayId IS NULL THEN
    SELECT q.id INTO quayId
    FROM quay AS q
    WHERE q.netex_id = inputQuayNetexId
    ORDER BY q.version DESC;
  END IF;

  --- See if the quay mirrors some other quay.
  SELECT mirroredQuay.id INTO mirroredQuayId
  FROM quay_key_values AS qkv
  INNER JOIN value_items AS vi ON vi.value_id = qkv.key_values_id
  INNER JOIN quay AS mirroredQuay ON mirroredQuay.netex_id = vi.items
  WHERE qkv.key_values_key = 'mirrors' AND qkv.quay_id = quayId
  ORDER BY mirroredQuay.version DESC;

  IF mirroredQuayId IS NOT NULL THEN
    RETURN mirroredQuayId;
  END IF;

  RETURN quayId;
END
$$;


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


-- Populate the jore_quay_extensions table with existing data.
DO $$
  DECLARE
    newestQuay RECORD;
  BEGIN
    FOR newestQuay IN (
      SELECT DISTINCT ON (netex_id) id
      FROM quay
      ORDER BY netex_id, version DESC
    ) LOOP
      PERFORM update_jore_quay_extensions_table(newestQuay.id);
    END LOOP;
  END;
$$;


--- Create a trigger to update the Jore extensions table.
--- Trigger on insert into quay_key_values. Changes to StopPlace or Quay always create
--- a full copy of the Quay, including copies of the key values.
--- Fire the trigger at the end of the Transaction in which the keyvalues are populated.
CREATE OR REPLACE FUNCTION update_jore_extensions_on_quay_save()
  RETURNS TRIGGER
  LANGUAGE 'plpgsql'
  VOLATILE AS $$
DECLARE
  quayNetexId TEXT;
BEGIN
  --- Only apply to Jore quays (mainly to allow old Tiamat Junit tests to work.
  SELECT netex_id INTO quayNetexId FROM quay WHERE id = NEW.quay_id;
  IF quayNetexId IS NULL AND NOT (quayNetexId LIKE 'HSL:Quay:%' OR quayNetexId LIKE 'FSR:Quay:%') THEN
    RETURN NULL;
  END IF;

  --- Skip update if already up to date, as this is called once per each key value pair.
  IF NOT EXISTS (SELECT 1 FROM jore_quay_extensions WHERE id = NEW.quay_id) THEN
    PERFORM update_jore_quay_extensions_table(NEW.quay_id);
  END IF;

  RETURN NULL;
END
$$;

CREATE CONSTRAINT TRIGGER update_jore_extensions_on_quay_save
  AFTER INSERT ON quay_key_values
  INITIALLY DEFERRED DEFERRABLE
  FOR EACH ROW EXECUTE FUNCTION update_jore_extensions_on_quay_save();

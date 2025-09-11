DROP VIEW IF EXISTS quay_newest_version;
DROP VIEW IF EXISTS quay_alt_name_by_type;
DROP VIEW IF EXISTS quay_max_version;


--- Separate max version selection to its own helper view
--- to keep the main view as a simple SELECT+JOIN view.
CREATE VIEW quay_max_version AS
SELECT DISTINCT ON (netex_id) netex_id, version, id
FROM quay
ORDER BY netex_id, version DESC;


--- Spread the join and the actual data table on single row →
--- Allows us to join a singular alt name on the main view,
--- without having to join in all quay_alternative_names rows.
CREATE VIEW quay_alt_name_by_type AS
SELECT qan.quay_id, an.name_type, an.name_lang, an.name_value
FROM quay_alternative_names AS qan
LEFT JOIN alternative_name AS an ON qan.alternative_names_id = an.id;


CREATE VIEW quay_newest_version AS
SELECT -- Quay's own fields
       q.id,
       q.netex_id,
       q.changed,
       q.created,
       q.from_date,
       q.to_date,
       q.version,
       q.version_comment,
       q.description_lang,
       q.description_value,
       q.name_lang,
       q.name_value,
       q.private_code_type,
       q.private_code_value,
       q.short_name_lang,
       q.short_name_value,
       q.centroid,
       q.all_areas_wheelchair_accessible,
       q.covered,
       q.level_ref,
       q.level_ref_version,
       q.site_ref,
       q.site_ref_version,
       q.label_lang,
       q.label_value,
       q.compass_bearing,
       q.public_code,
       q.polygon_id,
       q.accessibility_assessment_id,
       q.place_equipments_id,
       q.changed_by,

       -- Extra location bits
       qanbt.name_value     AS location_swe,
       streetAddress.items  AS street_address,

       -- HSL validity info
       priority.items       AS priority,
       validityStart.items  AS validity_start,
       validityEnd.items    AS validity_end,

       -- Extra used keyvalues
       ELYCode.items        AS ely_code,
       postalCode.items     AS postal_code,
       functionalArea.items AS functional_area,
       stopState.items      AS stop_state,

       -- Stop Place info
       spmv.id              AS stop_place_id,
       spmv.version         AS stop_place_version,
       spmv.netex_id        AS stop_place_netex_id

FROM quay AS q

    INNER JOIN quay_max_version AS maxVersion ON q.id = maxVersion.id

    INNER JOIN stop_place_quays AS spq ON spq.quays_id = q.id

    --- When deleting a Quay, it is not removed from the DB, and it is not event
    --- marked as soft deleted. Thus, by default 'SELECT FROM quay' also includes
    --- all deleted quays which we do not want. -> Select only those quays, that
    --- are still associated with some current version of a Stop Place.
    INNER JOIN stop_place_max_version AS spmv ON spq.stop_place_id = spmv.id

    --- These can technically contain multiple values -> Duplicate result rows.
    --- But in practice these should never contain duplicates on our use cases.
    --- Thus in name of performance assume they have a max one value.
    LEFT JOIN quay_alt_name_by_type AS qanbt ON
        q.id = qanbt.quay_id AND qanbt.name_type = 'OTHER' AND qanbt.name_lang = 'swe'

    LEFT JOIN quay_key_values AS qkvAddress ON
        q.id = qkvAddress.quay_id AND qkvAddress.key_values_key = 'streetAddress'
    LEFT JOIN value_items AS streetAddress ON qkvAddress.key_values_id = streetAddress.value_id

    LEFT JOIN quay_key_values AS qkvPriority ON
        q.id = qkvPriority.quay_id AND qkvPriority.key_values_key = 'priority'
    LEFT JOIN value_items AS priority ON qkvPriority.key_values_id = priority.value_id

    LEFT JOIN quay_key_values AS qkvValidityStart ON
        q.id = qkvValidityStart.quay_id AND qkvValidityStart.key_values_key = 'validityStart'
    LEFT JOIN value_items AS validityStart ON qkvValidityStart.key_values_id = validityStart.value_id

    LEFT JOIN quay_key_values AS qkvValidityEnd ON
        q.id = qkvValidityEnd.quay_id AND qkvValidityEnd.key_values_key = 'validityEnd'
    LEFT JOIN value_items AS validityEnd ON qkvValidityEnd.key_values_id = validityEnd.value_id

    LEFT JOIN quay_key_values AS qkvELYCode ON
        q.id = qkvELYCode.quay_id AND qkvELYCode.key_values_key = 'elyNumber'
    LEFT JOIN value_items AS ELYCode ON qkvELYCode.key_values_id = ELYCode.value_id

    LEFT JOIN quay_key_values AS qkvPostalCode ON
        q.id = qkvPostalCode.quay_id AND qkvPostalCode.key_values_key = 'postalCode'
    LEFT JOIN value_items AS postalCode ON qkvPostalCode.key_values_id = postalCode.value_id

    LEFT JOIN quay_key_values AS qkvFunctionalArea ON
        q.id = qkvFunctionalArea.quay_id AND qkvFunctionalArea.key_values_key = 'functionalArea'
    LEFT JOIN value_items AS functionalArea ON qkvFunctionalArea.key_values_id = functionalArea.value_id

    LEFT JOIN quay_key_values AS qkvStopState ON
    q.id = qkvStopState.quay_id AND qkvStopState.key_values_key = 'stopState'
    LEFT JOIN value_items AS stopState ON qkvStopState.key_values_id = stopState.value_id;

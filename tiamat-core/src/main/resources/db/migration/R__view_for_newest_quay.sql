DROP VIEW IF EXISTS quay_newest_version;
DROP VIEW IF EXISTS quay_alt_name_by_type;
--- Was needed previously, now handled by jore_quay_extensions.
DROP VIEW IF EXISTS quay_max_version;

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
       jqe.street_address,

       -- HSL validity info
       jqe.priority,
       jqe.validity_start,
       jqe.validity_end,

       -- Extra used keyvalues
       jqe.ely_number       AS ely_code,
       jqe.postal_code,
       jqe.functional_area,
       jqe.stop_state,
       jqe.stop_owner,
       jqe.timing_place_id,

       jqe.transport_modes,
       jqe.active_transport_modes,
       jqe.trunk_line_stop,
       jqe.speed_tram_stop,

       -- Stop Place info
       spmv.id              AS stop_place_id,
       spmv.version         AS stop_place_version,
       spmv.netex_id        AS stop_place_netex_id

FROM quay AS q

    INNER JOIN jore_quay_extensions AS jqe ON jqe.id = q.id

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
        q.id = qanbt.quay_id AND qanbt.name_type = 'OTHER' AND qanbt.name_lang = 'swe';

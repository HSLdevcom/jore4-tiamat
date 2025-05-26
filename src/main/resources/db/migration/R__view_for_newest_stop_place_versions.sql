DROP VIEW IF EXISTS stop_place_newest_version;

CREATE VIEW stop_place_newest_version AS
SELECT -- Stop Place's own fields
       sp.id,
       sp.netex_id,
       sp.changed,
       sp.created,
       sp.from_date,
       sp.to_date,
       sp.version,
       sp.version_comment,
       sp.description_lang,
       sp.description_value,
       sp.name_lang,
       sp.name_value,
       sp.private_code_type,
       sp.private_code_value,
       sp.short_name_lang,
       sp.short_name_value,
       sp.centroid,
       sp.all_areas_wheelchair_accessible,
       sp.covered,
       sp.parent_site_ref,
       sp.parent_site_ref_version,
       sp.air_submode,
       sp.border_crossing,
       sp.bus_submode,
       sp.coach_submode,
       sp.funicular_submode,
       sp.metro_submode,
       sp.public_code,
       sp.rail_submode,
       sp.stop_place_type,
       sp.telecabin_submode,
       sp.tram_submode,
       sp.transport_mode,
       sp.water_submode,
       sp.weighting,
       sp.polygon_id,
       sp.accessibility_assessment_id,
       sp.place_equipments_id,
       sp.topographic_place_id,
       sp.changed_by,
       sp.parent_stop_place,
       sp.modification_enumeration,

       -- Extra location bits
       streetAddress.items AS street_address,

       -- HSL validity info
       priority.items AS priority,
       validityStart.items AS validity_start,
       validityEnd.items AS validity_end

FROM stop_place AS sp

     INNER JOIN stop_place_max_version AS maxVersion ON sp.id = maxVersion.id

    --- These can technically contain multiple values -> Duplicate result rows.
    --- But in practice these should never contain duplicates on our use cases.
    --- Thus in name of performance assume they have a max one value.
    LEFT JOIN stop_place_key_values AS spkvAddress ON
        sp.id = spkvAddress.stop_place_id AND spkvAddress.key_values_key = 'streetAddress'
    LEFT JOIN value_items AS streetAddress ON spkvAddress.key_values_id = streetAddress.value_id

    LEFT JOIN stop_place_key_values AS spkvPriority ON
        sp.id = spkvPriority.stop_place_id AND spkvPriority.key_values_key = 'priority'
    LEFT JOIN value_items AS priority ON spkvPriority.key_values_id = priority.value_id

    LEFT JOIN stop_place_key_values AS spkvValidityStart ON
        sp.id = spkvValidityStart.stop_place_id AND spkvValidityStart.key_values_key = 'validityStart'
    LEFT JOIN value_items AS validityStart ON spkvValidityStart.key_values_id = validityStart.value_id

    LEFT JOIN stop_place_key_values AS spkvValidityEnd ON
        sp.id = spkvValidityEnd.stop_place_id AND spkvValidityEnd.key_values_key = 'validityEnd'
    LEFT JOIN value_items AS validityEnd ON spkvValidityEnd.key_values_id = validityEnd.value_id;

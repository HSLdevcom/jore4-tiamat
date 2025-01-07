DROP VIEW IF EXISTS quay_newest_version;

CREATE VIEW quay_newest_version AS
SELECT DISTINCT ON (q.netex_id)
    q.*,
    aa AS accessibility_assessment,
    hap AS hsl_accessibility_properties,
    ievs AS installed_equipment,
    pp AS persistable_polygon,
    qan as alternative_names,
    streetAddress.items as street_address,
    priority.items as priority,
    validityStart.items as validity_start,
    validityEnd.items as validity_end

FROM quay AS q

         LEFT JOIN accessibility_assessment AS aa ON q.accessibility_assessment_id = aa.id

         LEFT JOIN hsl_accessibility_properties AS hap ON aa.hsl_accessibility_properties_id = hap.id

         LEFT JOIN installed_equipment_version_structure as ievs on q.place_equipments_id = ievs.id

         LEFT JOIN persistable_polygon as pp on q.polygon_id = pp.id

         LEFT JOIN quay_alternative_names AS qan ON q.id = qan.quay_id

         LEFT JOIN quay_key_values AS qkvAddress ON q.id = qkvAddress.quay_id AND qkvAddress.key_values_key = 'streetAddress'
         LEFT JOIN value_items AS streetAddress ON qkvAddress.key_values_id = streetAddress.value_id

         LEFT JOIN quay_key_values AS qkvPriority ON q.id = qkvPriority.quay_id AND qkvPriority.key_values_key = 'priority'
         LEFT JOIN value_items AS priority ON qkvPriority.key_values_id = priority.value_id

         LEFT JOIN quay_key_values AS qkvValidityStart ON
            q.id = qkvValidityStart.quay_id AND qkvValidityStart.key_values_key = 'validityStart'
         LEFT JOIN value_items AS validityStart ON qkvValidityStart.key_values_id = validityStart.value_id

         LEFT JOIN quay_key_values AS qkvValidityEnd ON
            q.id = qkvValidityEnd.quay_id AND qkvValidityEnd.key_values_key = 'validityEnd'
         LEFT JOIN value_items AS validityEnd ON qkvValidityEnd.key_values_id = validityEnd.value_id


ORDER BY netex_id ASC, version DESC


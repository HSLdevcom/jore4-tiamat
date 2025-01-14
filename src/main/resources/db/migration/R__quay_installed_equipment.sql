DROP VIEW IF EXISTS quay_installed_equipment;

CREATE VIEW quay_installed_equipment AS

SELECT ievs.*,
       q.id as quay_id
FROM installed_equipment_version_structure_installed_equipment AS ievsie

         INNER JOIN installed_equipment_version_structure ievs on ievsie.installed_equipment_id = ievs.id
         INNER JOIN quay as q on ievsie.place_equipment_id = q.place_equipments_id

ORDER BY netex_id ASC, version DESC


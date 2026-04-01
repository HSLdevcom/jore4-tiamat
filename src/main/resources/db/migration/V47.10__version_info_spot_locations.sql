-- Add version tracking to InfoSpot location references
-- Extends existing info_spot_location table to support versioned references

ALTER TABLE info_spot_location 
    ADD COLUMN version VARCHAR(255);

CREATE INDEX idx_info_spot_location_ref_version 
    ON info_spot_location(location_netex_id, version);

-- Populate version column for existing data by looking up current versions
-- For Quay references
UPDATE info_spot_location isl
SET version = CAST((
    SELECT MAX(version)
    FROM quay 
    WHERE netex_id = isl.location_netex_id
) AS VARCHAR)
WHERE isl.location_netex_id LIKE '%:Quay:%';

-- For ParentStopPlace references
UPDATE info_spot_location isl
SET version = CAST((
    SELECT MAX(version)
    FROM stop_place 
    WHERE netex_id = isl.location_netex_id
) AS VARCHAR)
WHERE isl.location_netex_id LIKE '%:StopPlace:%';

-- For ShelterEquipment references
UPDATE info_spot_location isl
SET version = CAST((
    SELECT MAX(version)
    FROM installed_equipment_version_structure 
    WHERE netex_id = isl.location_netex_id
) AS VARCHAR)
WHERE isl.location_netex_id LIKE '%:ShelterEquipment:%';

-- For any remaining references without a version, default to '1'
UPDATE info_spot_location 
SET version = '1'
WHERE version IS NULL;

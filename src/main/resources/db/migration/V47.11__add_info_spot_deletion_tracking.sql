-- Add version-based deletion tracking for InfoSpots.
-- We store which entity (ref) and at which version number
-- the InfoSpot was deleted. This provides deterministic
-- filtering for version-aware queries.

ALTER TABLE info_spot ADD COLUMN deleted_at_entity_ref character varying(255);
ALTER TABLE info_spot ADD COLUMN deleted_at_entity_version bigint;

-- Add index for efficient querying
CREATE INDEX idx_info_spot_deleted_at_entity ON info_spot(deleted_at_entity_ref, deleted_at_entity_version);

COMMENT ON COLUMN info_spot.deleted_at_entity_ref IS 'NetEx ID of the entity (Quay/StopPlace) where this InfoSpot was deleted';
COMMENT ON COLUMN info_spot.deleted_at_entity_version IS 'Version number of the entity when this InfoSpot was deleted';

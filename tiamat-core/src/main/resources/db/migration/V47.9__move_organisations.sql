INSERT INTO quay_organisations (quay_id, organisation_ref, relationship_type)
SELECT spq.quays_id, spo.organisation_ref, spo.relationship_type
FROM stop_place_organisations as spo
INNER JOIN stop_place_quays as spq ON spo.stop_place_id = spq.stop_place_id;

DELETE FROM stop_place_organisations;
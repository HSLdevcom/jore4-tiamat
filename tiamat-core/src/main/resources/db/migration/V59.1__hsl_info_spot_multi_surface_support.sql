ALTER TABLE info_spot
  ADD COLUMN multi_surface_id BIGINT REFERENCES persistable_multi_polygon(id);

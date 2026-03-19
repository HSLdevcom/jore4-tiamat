
CREATE INDEX trgm_topographic_place_name_value_index ON topographic_place USING gist (name_value public.gist_trgm_ops);
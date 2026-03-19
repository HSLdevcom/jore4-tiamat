alter table stop_place add column modification_enumeration character varying(255);
CREATE INDEX stop_modification_enumeration_index ON stop_place USING btree (modification_enumeration);



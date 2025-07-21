-- Update info spot
ALTER TABLE info_spot
    ADD COLUMN width INTEGER,
    ADD COLUMN height INTEGER;

UPDATE info_spot SET width = 297, height = 420  WHERE poster_place_size = 'a3';
UPDATE info_spot SET width = 210, height = 297  WHERE poster_place_size = 'a4';
UPDATE info_spot SET width = 800, height = 1200 WHERE poster_place_size = 'cm80x120';

ALTER TABLE info_spot DROP COLUMN poster_place_size;


-- Alter info spot poster
ALTER TABLE info_spot_poster
    ADD COLUMN width INTEGER,
    ADD COLUMN height INTEGER;

UPDATE info_spot_poster SET width = 297, height = 420  WHERE poster_size = 'a3';
UPDATE info_spot_poster SET width = 210, height = 297  WHERE poster_size = 'a4';
UPDATE info_spot_poster SET width = 800, height = 1200 WHERE poster_size = 'cm80x120';

ALTER TABLE info_spot_poster DROP COLUMN poster_size;

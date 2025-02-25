CREATE OR REPLACE FUNCTION check_stop_place_unique_names()
RETURNS TRIGGER AS $$
BEGIN
    -- Find if there are any stop places with the same name with overlapping validity periods
    IF EXISTS (
        SELECT 1
        FROM stop_place sp
        WHERE sp.name_value = NEW.name_value
        AND sp.version = (
            SELECT MAX(version)
            FROM stop_place
            WHERE netex_id = sp.netex_id AND netex_id != NEW.netex_id
        )
        AND tstzrange(sp.from_date, sp.to_date, '[)') && tstzrange(NEW.from_date, NEW.to_date, '[)')
    ) THEN
        RAISE EXCEPTION 'STOP_PLACE_UNIQUE_NAME : Name % already exists for a stop place.', NEW.name_value;
    END IF;
    -- Find if there are any stop places with the same private code with overlapping validity periods
    IF EXISTS (
        SELECT 1
        FROM stop_place sp
        WHERE sp.private_code_value = NEW.private_code_value
        AND sp.version = (
            SELECT MAX(version)
            FROM stop_place
            WHERE netex_id = sp.netex_id AND netex_id != NEW.netex_id
        )
        AND tstzrange(sp.from_date, sp.to_date, '[)') && tstzrange(NEW.from_date, NEW.to_date, '[)')
    ) THEN
        RAISE EXCEPTION 'STOP_PLACE_UNIQUE_PRIVATE_CODE : Private code % already exists for a stop place.', NEW.private_code_value;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

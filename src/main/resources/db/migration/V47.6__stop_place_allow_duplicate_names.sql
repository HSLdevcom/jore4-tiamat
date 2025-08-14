-- Keeping the old function name and trigger
CREATE OR REPLACE FUNCTION check_stop_place_unique_names()
RETURNS TRIGGER AS $$
BEGIN
    -- Find if there are any stop places with the same private code with overlapping validity periods
    IF EXISTS (
        SELECT 1 FROM (
            SELECT old_range.date_range AS old_range,
                   new_range.date_range AS new_range
            FROM stop_place sp
            INNER JOIN get_stop_place_validity_period(sp.id) old_range ON sp.id = old_range.id
            INNER JOIN get_stop_place_validity_period(NEW.id) new_range ON NEW.id = new_range.id
            WHERE sp.private_code_value = NEW.private_code_value
            AND sp.version = (
                SELECT MAX(version)
                FROM stop_place
                WHERE netex_id = sp.netex_id AND netex_id != NEW.netex_id
            )
        ) stop_with_dates WHERE old_range && new_range
    ) THEN
        RAISE EXCEPTION 'STOP_PLACE_UNIQUE_PRIVATE_CODE : Private code % already exists for a stop place.', NEW.private_code_value;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION check_stop_place_unique_names()
RETURNS TRIGGER AS $$
BEGIN
    -- Find if there are any stop places with the same name with overlapping validity periods
    IF EXISTS (
        select 1 from (
            select stop.id, stop.name_value, stop.private_code_value, old_name_start_time, old_name_end_time, new_name_start_time, new_name_end_time
            from (
            select sp.id, sp.name_value, sp.private_code_value
            from stop_place sp
            where sp.name_value = NEW.name_value
            and sp.version = (
            SELECT MAX(version)
            FROM stop_place
            WHERE netex_id = sp.netex_id and netex_id != new.netex_id
            )) stop
            inner join lateral (
                select TO_DATE(vi.items, 'YYYY-MM-DD') as old_name_start_time
                from stop_place_key_values spkv
                left join value_items vi on spkv.key_values_id = vi.value_id
                where spkv.stop_place_id = stop.id and spkv.key_values_key = 'validityStart'
            ) old_name_start_time on true
            inner join lateral (
                select TO_DATE(vi.items, 'YYYY-MM-DD') as old_name_end_time
                from stop_place_key_values spkv
                left join value_items vi on spkv.key_values_id = vi.value_id
                where spkv.stop_place_id = stop.id and spkv.key_values_key = 'validityEnd'
            ) old_name_end_time on true
            inner join lateral (
                select TO_DATE(vi.items, 'YYYY-MM-DD') as new_name_start_time
                from stop_place_key_values spkv
                left join value_items vi on spkv.key_values_id = vi.value_id
                where spkv.stop_place_id = NEW.id and spkv.key_values_key = 'validityStart'
            ) new_name_start_time on true
            inner join lateral (
                select TO_DATE(vi.items, 'YYYY-MM-DD') as new_name_end_time
                from stop_place_key_values spkv
                left join value_items vi on spkv.key_values_id = vi.value_id
                where spkv.stop_place_id = NEW.id and spkv.key_values_key = 'validityEnd'
            ) new_name_end_time on true
        ) stop_with_dates where tstzrange(old_name_start_time, old_name_end_time, '[)') && tstzrange(new_name_start_time, new_name_end_time, '[)')
    ) THEN
        RAISE EXCEPTION 'STOP_PLACE_UNIQUE_NAME : Name % already exists for a stop place.', NEW.name_value;
    END IF;
    -- Find if there are any stop places with the same private code with overlapping validity periods
    IF EXISTS (
        select 1 from (
            select stop.id, stop.name_value, stop.private_code_value, old_label_start_time, old_label_end_time, new_label_start_time, new_label_end_time
            from (
            select sp.id, sp.name_value, sp.private_code_value
            from stop_place sp
            WHERE sp.private_code_value = NEW.private_code_value
            and sp.version = (
            SELECT MAX(version)
            FROM stop_place
            WHERE netex_id = sp.netex_id and netex_id != new.netex_id
            )) stop
            inner join lateral (
                select TO_DATE(vi.items, 'YYYY-MM-DD') as old_label_start_time
                from stop_place_key_values spkv
                left join value_items vi on spkv.key_values_id = vi.value_id
                where spkv.stop_place_id = stop.id and spkv.key_values_key = 'validityStart'
            ) old_label_start_time on true
            inner join lateral (
                select TO_DATE(vi.items, 'YYYY-MM-DD') as old_label_end_time
                from stop_place_key_values spkv
                left join value_items vi on spkv.key_values_id = vi.value_id
                where spkv.stop_place_id = stop.id and spkv.key_values_key = 'validityEnd'
            ) old_label_end_time on true
            inner join lateral (
                select TO_DATE(vi.items, 'YYYY-MM-DD') as new_label_start_time
                from stop_place_key_values spkv
                left join value_items vi on spkv.key_values_id = vi.value_id
                where spkv.stop_place_id = NEW.id and spkv.key_values_key = 'validityStart'
            ) new_label_start_time on true
            inner join lateral (
                select TO_DATE(vi.items, 'YYYY-MM-DD') as new_label_end_time
                from stop_place_key_values spkv
                left join value_items vi on spkv.key_values_id = vi.value_id
                where spkv.stop_place_id = NEW.id and spkv.key_values_key = 'validityEnd'
            ) new_label_end_time on true
        ) stop_with_dates where tstzrange(old_label_start_time, old_label_end_time, '[)') && tstzrange(new_label_start_time, new_label_end_time, '[)')
    ) THEN
        RAISE EXCEPTION 'STOP_PLACE_UNIQUE_PRIVATE_CODE : Private code % already exists for a stop place.', NEW.private_code_value;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE constraint TRIGGER stop_place_unique_names_trigger
AFTER INSERT OR UPDATE ON stop_place deferrable initially deferred
FOR EACH ROW
EXECUTE FUNCTION check_stop_place_unique_names();

-- Drop constraint if created in earlier migration version.
ALTER table stop_place DROP CONSTRAINT IF EXISTS has_valid_hsl_private_code;

-- Add in the DB constraint
ALTER TABLE stop_place
ADD CONSTRAINT has_valid_hsl_private_code
CHECK (
    -- Dont enforce format for non HSL codes
    private_code_type NOT LIKE 'HSL%'  OR

    -- Allow any random string as the code in test data.
    -- So that we don't have to rewrite all the test cases/data.
    (private_code_type = 'HSL/TEST') OR

    -- StopAreas created in Jore 4 need to start with 7 and be 6-numbers long
    (private_code_type = 'HSL/JORE-4' AND NOT parent_stop_place AND private_code_value ~ '^[7]\d{5}$') OR
    -- StopAreas created in Jore 3 do not start with 7 and are 6-numbers long
    (private_code_type = 'HSL/JORE-3' AND NOT parent_stop_place AND private_code_value ~ '^[012345689]\d{5}$') OR

    -- Terminals created in Jore 4 need to start with 7 and be 7-numbers long
    (private_code_type = 'HSL/JORE-4' AND parent_stop_place AND private_code_value ~ '^[7]\d{6}$') OR
    -- Terminals created in Jore 3 do not start with 7 and are 7-numbers long
    (private_code_type = 'HSL/JORE-3' AND parent_stop_place AND private_code_value ~ '^[012345689]\d{6}$')
);

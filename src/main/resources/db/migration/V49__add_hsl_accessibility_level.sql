ALTER TABLE ONLY hsl_accessibility_properties
    ADD COLUMN accessibility_level character varying(255) NOT NULL DEFAULT 'unknown';

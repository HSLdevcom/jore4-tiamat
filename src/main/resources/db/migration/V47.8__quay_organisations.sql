CREATE TABLE quay_organisations (
    quay_id bigint NOT NULL,
    organisation_ref character varying(255) NOT NULL,
    relationship_type character varying(255) NOT NULL,
    PRIMARY KEY (quay_id, organisation_ref, relationship_type),
    FOREIGN KEY (quay_id) REFERENCES quay(id) ON DELETE CASCADE
);
ALTER TABLE quay_organisations OWNER TO tiamat;

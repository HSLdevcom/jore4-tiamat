create table info_spot
(
    id                                bigint primary key,
    netex_id                          character varying(255),
    changed                           timestamp without time zone,
    created                           timestamp without time zone,
    from_date                         timestamp without time zone,
    to_date                           timestamp without time zone,
    version                           bigint not null,
    version_comment                   character varying(255),
    changed_by                        character varying(255),

    label                             character varying(255),
    purpose                           character varying(255),
    poster_place_type                 character varying(255),
    poster_place_size                 character varying(255),
    description                       text,
    backlight                         boolean,
    maintenance                       character varying(255),
    zone_label                        character varying(255),
    rail_information                  character varying(255),
    floor                             character varying(255),
    speech_property                   boolean,
    display_type                      character varying(255)
);

create table info_spot_poster
(
    label                             character varying(255) primary key,
    info_spot_id                      bigint not null,
    poster_type                       character varying(255),
    poster_size                       character varying(255),
    lines                             character varying(255),
    foreign key (info_spot_id) references info_spot (id) on delete cascade
);

create table info_spot_stop_place
(
    info_spot_id                      bigint not null,
    ref                               character varying(255),
    version                           character varying(255),
    foreign key (info_spot_id) references info_spot (id) on delete cascade
);

create table info_spot_key_values
(
    info_spot_id                      bigint not null,
    key_values_id                     bigint NOT NULL,
    key_values_key                    character varying(255) NOT NULL,
    foreign key (info_spot_id) references info_spot (id) on delete cascade
);

CREATE SEQUENCE info_spot_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

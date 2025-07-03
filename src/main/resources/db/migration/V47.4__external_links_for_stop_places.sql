CREATE TABLE stop_place_external_links (
    stop_place_id     BIGINT NOT NULL REFERENCES stop_place(id),
    order_num   INT NOT NULL,
    name        text,
    location    text,
    PRIMARY KEY (stop_place_id, order_num)
);

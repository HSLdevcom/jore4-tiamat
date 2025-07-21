package org.rutebanken.tiamat.model;

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.HEIGHT;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.WIDTH;

import java.util.Map;
import java.util.Objects;

/**
 * This enum exists as a temporary compatability helper, as we do not have versioned
 * microservice dependencies or monorepo, we need to convert back and forth with the
 * enum until, UI and E2E tests all use the new width/height interface.
 */
public enum PosterSizeEnumeration {
    A3("a3", 297, 420),
    A4("a4", 210, 297),
    CM80x120("cm80x120", 800, 1200);

    private final String value;

    private final int width;
    private final int height;

    PosterSizeEnumeration(String v, int w, int h) {
        value = v;
        width = w;
        height = h;
    }

    public String value() {
        return value;
    }

    public int height() {
        return height;
    }

    public int width() {
        return width;
    }

    public Map<String, Integer> toSizeMap() {
        return Map.of(WIDTH, width, HEIGHT, height);
    }

    public static PosterSizeEnumeration fromValue(String v) {

        for (PosterSizeEnumeration c : PosterSizeEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }

        throw new IllegalArgumentException(v + " is not a valid value of PosterSizeEnumeration");
    }

    public static PosterSizeEnumeration fromSize(Integer w, Integer h) {
        for (PosterSizeEnumeration c : PosterSizeEnumeration.values()) {
            if (Objects.equals(c.width, w) && Objects.equals(c.height, h)) {
                return c;
            }
        }

        return null;
    }
}

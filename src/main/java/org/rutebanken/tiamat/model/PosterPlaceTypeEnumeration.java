package org.rutebanken.tiamat.model;

public enum PosterPlaceTypeEnumeration {
    STATIC("static"),
    DYNAMIC("dynamic"),
    SOUND_BEACON("soundBeacon");

    private final String value;

    PosterPlaceTypeEnumeration(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static PosterPlaceTypeEnumeration fromValue(String v) {

        for (PosterPlaceTypeEnumeration c : PosterPlaceTypeEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }

        throw new IllegalArgumentException(v);
    }
}

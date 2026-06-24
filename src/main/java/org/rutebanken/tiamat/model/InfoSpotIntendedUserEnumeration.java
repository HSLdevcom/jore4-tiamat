package org.rutebanken.tiamat.model;

public enum InfoSpotIntendedUserEnumeration {
    MATKATIETO("MATKATIETO"),
    MARKKINOINTI("MARKKINOINTI"),
    VR("VR"),
    MUU("MUU");

    private final String value;

    InfoSpotIntendedUserEnumeration(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static InfoSpotIntendedUserEnumeration fromValue(String value) {
        for (var c : InfoSpotIntendedUserEnumeration.values()) {
            if (c.value.equals(value)) {
                return c;
            }
        }

        throw new IllegalArgumentException(value + " is not a valid value of InfoSpotIntendedUserEnumeration");
    }
}

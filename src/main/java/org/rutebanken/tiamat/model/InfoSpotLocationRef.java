package org.rutebanken.tiamat.model;

import jakarta.persistence.Embeddable;

/**
 * Version-aware reference for InfoSpot.
 * Replaces simple String-based references with versioned references.
 */
@Embeddable
public class InfoSpotLocationRef extends VersionOfObjectRefStructure {

    public InfoSpotLocationRef() {
    }

    public InfoSpotLocationRef(String netexId, Long version) {
        this.setRef(netexId);
        this.setVersion(version != null ? String.valueOf(version) : null);
    }

    public InfoSpotLocationRef(String netexId, String version) {
        this.setRef(netexId);
        this.setVersion(version);
    }

    /**
     * Constructor without version - for backward compatibility
     */
    public InfoSpotLocationRef(String netexId) {
        this.setRef(netexId);
    }
}

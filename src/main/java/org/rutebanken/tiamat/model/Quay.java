/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.model;

import com.google.common.base.MoreObjects;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "quay_netex_id_version_constraint", columnNames = {"netexId", "version"})}
)
public class Quay extends StopPlaceSpace_VersionStructure {

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<BoardingPosition> boardingPositions = new ArrayList<>();
    protected String publicCode;

    /**
     * TODO: reconsider data type for compass bearing.
     * https://rutebanken.atlassian.net/browse/NRP-895
     */
    protected Float compassBearing;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            joinColumns = @JoinColumn(name = "quayId")
    )
    @OrderColumn(name = "orderNum")
    protected List<QuayExternalLink> externalLinks = new ArrayList<>();

    public Quay(EmbeddableMultilingualString name) {
        super(name);
    }

    public Quay() {
    }

    public String getPublicCode() {
        return publicCode;
    }

    public void setPublicCode(String value) {
        this.publicCode = value;
    }

    public Float getCompassBearing() {
        return compassBearing;
    }

    public void setCompassBearing(Float value) {
        this.compassBearing = value;
    }

    public List<BoardingPosition> getBoardingPositions() {
        return boardingPositions;
    }

    public List<QuayExternalLink> getExternalLinks() {
        return externalLinks;
    }

    public void setExternalLinks(Collection<QuayExternalLink> externalLinks) {
        this.externalLinks = List.copyOf(externalLinks);
    }

    public void resetNetexIds() {

        setNetexId(null);
        setVersion(0);

        // Manually reset netex ids of the related items to prevent conflicts

        var accessibilityAssessment = getAccessibilityAssessment();
        if (accessibilityAssessment != null) {
            accessibilityAssessment.setNetexId(null);
            accessibilityAssessment.setVersion(0);
        }

        var installedEquipment = getPlaceEquipments();
        if (installedEquipment != null) {
            installedEquipment.setNetexId(null);
            installedEquipment.setVersion(0);
        }

        getAlternativeNames().forEach(alternativeName -> {
            alternativeName.setNetexId(null);
            alternativeName.setVersion(0);
        });

        var boardingPositions = getBoardingPositions();
        if (boardingPositions != null) {
            boardingPositions.forEach(boardingPosition -> {
                boardingPosition.setNetexId(null);
                boardingPosition.setVersion(0);
            });
        }

        var checkConstraints = getCheckConstraints();
        if (checkConstraints != null) {
            checkConstraints.forEach(checkConstraint -> {
                checkConstraint.setNetexId(null);
                checkConstraint.setVersion(0);
            });
        }

        var equipmentPlaces = getEquipmentPlaces();
        if (equipmentPlaces != null) {
            equipmentPlaces.forEach(equipmentPlace -> {
                equipmentPlace.setNetexId(null);
                equipmentPlace.setVersion(0);
            });
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Quay)) {
            return false;
        }

        Quay other = (Quay) object;

        return Objects.equals(this.name, other.name)
                && Objects.equals(this.centroid, other.centroid)
                && Objects.equals(this.compassBearing, other.compassBearing)
                && Objects.equals(this.publicCode, other.publicCode)
                && getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).containsAll(other.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY));
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, centroid,
                compassBearing, publicCode,
                getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id", id)
                .add("netexId", netexId)
                .add("version", version)
                .add("name", name)
                .add("centroid", centroid)
                .add("bearing", compassBearing)
                .add("publicCode", publicCode)
                .add("keyValues", getKeyValues())
                .toString();
    }
}

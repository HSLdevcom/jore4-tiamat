package org.rutebanken.tiamat.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "info_spot_netex_id_version_constraint", columnNames = {"netexId", "version"})}
)
public class InfoSpot extends DataManagedObjectStructure implements Serializable {

    protected String label;
    protected String purpose;
    protected String description;
    @Enumerated(EnumType.STRING)
    protected PosterPlaceTypeEnumeration posterPlaceType;
    @Enumerated(EnumType.STRING)
    protected PosterSizeEnumeration posterPlaceSize;
    protected Boolean backlight;
    protected String maintenance;
    protected String zoneLabel;
    protected String railInformation;
    protected String floor;
    protected Boolean speechProperty;
    @Enumerated(EnumType.STRING)
    protected DisplayTypeEnumeration displayType;

    @ElementCollection(targetClass = StopPlaceReference.class, fetch = FetchType.EAGER)
    @CollectionTable(
            name = "info_spot_stop_place"
    )
    private Set<StopPlaceReference> stopPlaces = new HashSet<>();

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PosterPlaceTypeEnumeration getPosterPlaceType() {
        return posterPlaceType;
    }

    public void setPosterPlaceType(PosterPlaceTypeEnumeration posterPlaceType) {
        this.posterPlaceType = posterPlaceType;
    }

    public PosterSizeEnumeration getPosterPlaceSize() {
        return posterPlaceSize;
    }

    public void setPosterPlaceSize(PosterSizeEnumeration posterPlaceSize) {
        this.posterPlaceSize = posterPlaceSize;
    }

    public Boolean getBacklight() {
        return backlight;
    }

    public void setBacklight(Boolean backlight) {
        this.backlight = backlight;
    }

    public String getMaintenance() {
        return maintenance;
    }

    public void setMaintenance(String maintenance) {
        this.maintenance = maintenance;
    }

    public String getZoneLabel() {
        return zoneLabel;
    }

    public void setZoneLabel(String zoneLabel) {
        this.zoneLabel = zoneLabel;
    }

    public String getRailInformation() {
        return railInformation;
    }

    public void setRailInformation(String railInformation) {
        this.railInformation = railInformation;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public Boolean getSpeechProperty() {
        return speechProperty;
    }

    public void setSpeechProperty(Boolean speechProperty) {
        this.speechProperty = speechProperty;
    }

    public DisplayTypeEnumeration getDisplayType() {
        return displayType;
    }

    public void setDisplayType(DisplayTypeEnumeration displayType) {
        this.displayType = displayType;
    }

    public Set<StopPlaceReference> getStopPlaces() {
        return stopPlaces;
    }

    public void setStopPlaces(Set<StopPlaceReference> stopPlaces) {
        this.stopPlaces = stopPlaces;
    }
}

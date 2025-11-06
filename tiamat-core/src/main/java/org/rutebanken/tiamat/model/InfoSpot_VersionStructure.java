package org.rutebanken.tiamat.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.MappedSuperclass;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@MappedSuperclass
public class InfoSpot_VersionStructure extends Zone_VersionStructure {

    @Serial
    private static final long serialVersionUID = -4061319784665164923L;

    private InfoSpotTypeEnumeration infoSpotType;
    private String label;
    private String purpose;
    private Integer width;
    private Integer height;
    private Boolean backlight;
    private String maintenance;
    private String zoneLabel;
    private String railInformation;
    private String floor;
    private Boolean speechProperty;
    @Enumerated(EnumType.STRING)
    private DisplayTypeEnumeration displayType;

    @ElementCollection
    @CollectionTable(
            name = "info_spot_location"
    )
    @Column(name="location_netex_id")
    private Set<String> infoSpotLocations = new HashSet<>();

    @ElementCollection(targetClass = InfoSpotPosterRef.class, fetch = FetchType.EAGER)
    @CollectionTable(
            name = "info_spot_poster_ref"
    )
    private List<InfoSpotPosterRef> posters = new ArrayList<>();

    public InfoSpotTypeEnumeration getInfoSpotType() {
        return infoSpotType;
    }

    public void setInfoSpotType(InfoSpotTypeEnumeration infoSpotType) {
        this.infoSpotType = infoSpotType;
    }

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

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
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

    public Set<String> getInfoSpotLocations() {
        return infoSpotLocations;
    }

    public void setInfoSpotLocations(Collection<String> infoSpotLocation) {
        this.infoSpotLocations = Set.copyOf(infoSpotLocation);
    }

    public List<InfoSpotPosterRef> getPosters() {
        return posters;
    }

    public void setPosters(List<InfoSpotPosterRef> posters) {
        this.posters = posters;
    }
}

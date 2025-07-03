package org.rutebanken.tiamat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class StopPlaceExternalLink {
    private String name;
    private String location;

    @Column(insertable=false, updatable=false)
    private long stopPlaceId;
    @Column(insertable=false, updatable=false)
    private int orderNum;

    public StopPlaceExternalLink() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getStopPlaceId() {
        return stopPlaceId;
    }

    public int getOrderNum() {
        return orderNum;
    }
}

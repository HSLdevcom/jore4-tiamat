package org.rutebanken.tiamat.model;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class InfoSpotPoster_VersionStructure extends DataManagedObjectStructure {
    private String label;
    private String lines;

    private Integer width;
    private Integer height;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLines() {
        return lines;
    }

    public void setLines(String lines) {
        this.lines = lines;
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
}

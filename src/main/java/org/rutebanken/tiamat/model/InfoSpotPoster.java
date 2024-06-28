package org.rutebanken.tiamat.model;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class InfoSpotPoster {

    private String label;
    private String posterType;
    private String lines;

    @Enumerated(EnumType.STRING)
    private PosterSizeEnumeration posterSize;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPosterType() {
        return posterType;
    }

    public void setPosterType(String posterType) {
        this.posterType = posterType;
    }

    public String getLines() {
        return lines;
    }

    public void setLines(String lines) {
        this.lines = lines;
    }

    public PosterSizeEnumeration getPosterSize() {
        return posterSize;
    }

    public void setPosterSize(PosterSizeEnumeration posterSize) {
        this.posterSize = posterSize;
    }
}

package org.rutebanken.tiamat.model;

public class StopAssignment_VersionStructure
        extends Assignment_VersionStructure {

    protected Boolean boardingUse;
    protected Boolean alightingUse;
    protected PrivateCodeStructure privateCode;
    protected ScheduledStopPointRefStructure scheduledStopPointRef;

    public Boolean isBoardingUse() {
        return boardingUse;
    }

    public void setBoardingUse(Boolean value) {
        this.boardingUse = value;
    }

    public Boolean isAlightingUse() {
        return alightingUse;
    }

    public void setAlightingUse(Boolean value) {
        this.alightingUse = value;
    }

    public PrivateCodeStructure getPrivateCode() {
        return privateCode;
    }

    public void setPrivateCode(PrivateCodeStructure value) {
        this.privateCode = value;
    }

    public ScheduledStopPointRefStructure getScheduledStopPointRef() {
        return scheduledStopPointRef;
    }

    public void setScheduledStopPointRef(ScheduledStopPointRefStructure value) {
        this.scheduledStopPointRef = value;
    }

}

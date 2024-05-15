package org.rutebanken.tiamat.model.hsl;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.rutebanken.tiamat.model.VersionedChildStructure;

import javax.persistence.Entity;
import java.util.Objects;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class HslAccessibilityProperties extends VersionedChildStructure {
    protected Double stopAreaSideSlope;
    protected Double stopAreaLengthwiseSlope;
    protected Double endRampSlope;
    protected Double shelterLaneDistance;
    protected Double curbBackOfRailDistance;
    protected Double curbDriveSideOfRailDistance;
    protected Double structureLaneDistance;
    protected Double stopHeightFromRailTop;
    protected Double stopHeightFromSidewalk;
    protected Double lowerCleatHeight;
    protected Double serviceAreaWidth;
    protected Double serviceAreaLength;
    protected Boolean platformEdgeWarningArea;
    protected Boolean guidanceTiles;
    protected Boolean guidanceStripe;
    protected Boolean serviceAreaStripes;
    protected Boolean sidewalkAccessibleConnection;
    protected Boolean stopAreaSurroundingsAccessible;
    protected Boolean curvedStop;

    public void copyPropertiesFrom(HslAccessibilityProperties base) {
        this.stopAreaSideSlope = base.stopAreaSideSlope;
        this.stopAreaLengthwiseSlope = base.stopAreaLengthwiseSlope;
        this.endRampSlope = base.endRampSlope;
        this.shelterLaneDistance = base.shelterLaneDistance;
        this.curbBackOfRailDistance = base.curbBackOfRailDistance;
        this.curbDriveSideOfRailDistance = base.curbDriveSideOfRailDistance;
        this.structureLaneDistance = base.structureLaneDistance;
        this.stopHeightFromRailTop = base.stopHeightFromRailTop;
        this.stopHeightFromSidewalk = base.stopHeightFromSidewalk;
        this.lowerCleatHeight = base.lowerCleatHeight;
        this.serviceAreaWidth = base.serviceAreaWidth;
        this.serviceAreaLength = base.serviceAreaLength;
        this.platformEdgeWarningArea = base.platformEdgeWarningArea;
        this.guidanceTiles = base.guidanceTiles;
        this.guidanceStripe = base.guidanceStripe;
        this.serviceAreaStripes = base.serviceAreaStripes;
        this.sidewalkAccessibleConnection = base.sidewalkAccessibleConnection;
        this.stopAreaSurroundingsAccessible = base.stopAreaSurroundingsAccessible;
        this.curvedStop = base.curvedStop;
    }

    public HslAccessibilityProperties copy() {
        HslAccessibilityProperties copy = new HslAccessibilityProperties();
        copy.copyPropertiesFrom(this);
        return copy;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof HslAccessibilityProperties)) {
            return false;
        }

        HslAccessibilityProperties other = (HslAccessibilityProperties) object;

        return Objects.equals(this.stopAreaSideSlope, other.stopAreaSideSlope)
                && Objects.equals(this.stopAreaLengthwiseSlope, other.stopAreaLengthwiseSlope)
                && Objects.equals(this.endRampSlope, other.endRampSlope)
                && Objects.equals(this.shelterLaneDistance, other.shelterLaneDistance)
                && Objects.equals(this.curbBackOfRailDistance, other.curbBackOfRailDistance)
                && Objects.equals(this.curbDriveSideOfRailDistance, other.curbDriveSideOfRailDistance)
                && Objects.equals(this.structureLaneDistance, other.structureLaneDistance)
                && Objects.equals(this.stopHeightFromRailTop, other.stopHeightFromRailTop)
                && Objects.equals(this.stopHeightFromSidewalk, other.stopHeightFromSidewalk)
                && Objects.equals(this.lowerCleatHeight, other.lowerCleatHeight)
                && Objects.equals(this.serviceAreaWidth, other.serviceAreaWidth)
                && Objects.equals(this.serviceAreaLength, other.serviceAreaLength)
                && Objects.equals(this.platformEdgeWarningArea, other.platformEdgeWarningArea)
                && Objects.equals(this.guidanceTiles, other.guidanceTiles)
                && Objects.equals(this.guidanceStripe, other.guidanceStripe)
                && Objects.equals(this.serviceAreaStripes, other.serviceAreaStripes)
                && Objects.equals(this.sidewalkAccessibleConnection, other.sidewalkAccessibleConnection)
                && Objects.equals(this.stopAreaSurroundingsAccessible, other.stopAreaSurroundingsAccessible)
                && Objects.equals(this.curvedStop, other.curvedStop);
    }

    public Double getStopAreaSideSlope() {
        return stopAreaSideSlope;
    }

    public void setStopAreaSideSlope(Double stopAreaSideSlope) {
        this.stopAreaSideSlope = stopAreaSideSlope;
    }

    public Double getStopAreaLengthwiseSlope() {
        return stopAreaLengthwiseSlope;
    }

    public void setStopAreaLengthwiseSlope(Double stopAreaLengthwiseSlope) {
        this.stopAreaLengthwiseSlope = stopAreaLengthwiseSlope;
    }

    public Double getEndRampSlope() {
        return endRampSlope;
    }

    public void setEndRampSlope(Double endRampSlope) {
        this.endRampSlope = endRampSlope;
    }

    public Double getShelterLaneDistance() {
        return shelterLaneDistance;
    }

    public void setShelterLaneDistance(Double shelterRoadwayDistance) {
        this.shelterLaneDistance = shelterRoadwayDistance;
    }

    public Double getCurbBackOfRailDistance() {
        return curbBackOfRailDistance;
    }

    public void setCurbBackOfRailDistance(Double curbBackOfRailDistance) {
        this.curbBackOfRailDistance = curbBackOfRailDistance;
    }

    public Double getCurbDriveSideOfRailDistance() {
        return curbDriveSideOfRailDistance;
    }

    public void setCurbDriveSideOfRailDistance(Double curbDriveSideOfRailDistance) {
        this.curbDriveSideOfRailDistance = curbDriveSideOfRailDistance;
    }

    public Double getStructureLaneDistance() {
        return structureLaneDistance;
    }

    public void setStructureLaneDistance(Double structureRoadwayDistance) {
        this.structureLaneDistance = structureRoadwayDistance;
    }

    public Double getStopHeightFromRailTop() {
        return stopHeightFromRailTop;
    }

    public void setStopHeightFromRailTop(Double stopHeightFromRailTop) {
        this.stopHeightFromRailTop = stopHeightFromRailTop;
    }

    public Double getStopHeightFromSidewalk() {
        return stopHeightFromSidewalk;
    }

    public void setStopHeightFromSidewalk(Double stopHeightFromCurb) {
        this.stopHeightFromSidewalk = stopHeightFromCurb;
    }

    public Double getLowerCleatHeight() {
        return lowerCleatHeight;
    }

    public void setLowerCleatHeight(Double lowerCleatHeight) {
        this.lowerCleatHeight = lowerCleatHeight;
    }

    public Double getServiceAreaWidth() {
        return serviceAreaWidth;
    }

    public void setServiceAreaWidth(Double serviceAreaWidth) {
        this.serviceAreaWidth = serviceAreaWidth;
    }

    public Double getServiceAreaLength() {
        return serviceAreaLength;
    }

    public void setServiceAreaLength(Double serviceAreaLength) {
        this.serviceAreaLength = serviceAreaLength;
    }

    public Boolean isPlatformEdgeWarningArea() {
        return platformEdgeWarningArea;
    }

    public void setPlatformEdgeWarningArea(Boolean platformEdgeWarningArea) {
        this.platformEdgeWarningArea = platformEdgeWarningArea;
    }

    public Boolean isGuidanceTiles() {
        return guidanceTiles;
    }

    public void setGuidanceTiles(Boolean guideTiles) {
        this.guidanceTiles = guideTiles;
    }

    public Boolean isGuidanceStripe() {
        return guidanceStripe;
    }

    public void setGuidanceStripe(Boolean guideStripe) {
        this.guidanceStripe = guideStripe;
    }

    public Boolean isServiceAreaStripes() {
        return serviceAreaStripes;
    }

    public void setServiceAreaStripes(Boolean serviceAreaStripes) {
        this.serviceAreaStripes = serviceAreaStripes;
    }

    public Boolean isSidewalkAccessibleConnection() {
        return sidewalkAccessibleConnection;
    }

    public void setSidewalkAccessibleConnection(Boolean sidewalkAccessibleConnection) {
        this.sidewalkAccessibleConnection = sidewalkAccessibleConnection;
    }

    public Boolean isStopAreaSurroundingsAccessible() {
        return stopAreaSurroundingsAccessible;
    }

    public void setStopAreaSurroundingsAccessible(Boolean stopAreaSurroundingsAccessible) {
        this.stopAreaSurroundingsAccessible = stopAreaSurroundingsAccessible;
    }

    public Boolean isCurvedStop() {
        return curvedStop;
    }

    public void setCurvedStop(Boolean curvedStop) {
        this.curvedStop = curvedStop;
    }
}

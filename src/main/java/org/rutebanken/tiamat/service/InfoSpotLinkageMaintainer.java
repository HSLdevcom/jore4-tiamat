package org.rutebanken.tiamat.service;

import org.rutebanken.tiamat.model.InfoSpot;
import org.rutebanken.tiamat.model.InfoSpotLocationRef;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.InfoSpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service responsible for maintaining InfoSpot linkages when entities are versioned.
 * When a StopPlace/Quay is versioned, this service copies all InfoSpot linkages
 * from the old version to the new version, so InfoSpots appear on both versions.
 */
@Service
@Transactional
public class InfoSpotLinkageMaintainer {

    private static final Logger logger = LoggerFactory.getLogger(InfoSpotLinkageMaintainer.class);

    @Autowired
    private InfoSpotRepository infoSpotRepository;

    /**
     * Copy InfoSpot linkages when a StopPlace is versioned, optionally excluding a specific InfoSpot.
     * Used when deleting an InfoSpot (pass excludeInfoSpotNetexId) or copying normally (pass null).
     * This ensures InfoSpots appear on both the old and new versions.
     */
    public void maintainInfoSpotLinkagesExcluding(StopPlace oldStopPlace, StopPlace newStopPlace, String excludeInfoSpotNetexId) {
        if (oldStopPlace == null || newStopPlace == null) {
            return;
        }

        logger.info("Maintaining InfoSpot linkages from StopPlace {}:{} to {}:{}",
            oldStopPlace.getNetexId(), oldStopPlace.getVersion(),
            newStopPlace.getNetexId(), newStopPlace.getVersion());

        // Copy for StopPlace
        copyInfoSpotLinksToNewVersion(oldStopPlace.getNetexId(), oldStopPlace.getVersion(),
            newStopPlace.getVersion(), excludeInfoSpotNetexId);

        // Copy for all Quays
        if (oldStopPlace.getQuays() != null && newStopPlace.getQuays() != null) {
            for (Quay oldQuay : oldStopPlace.getQuays()) {
                for (Quay newQuay : newStopPlace.getQuays()) {
                    if (oldQuay.getNetexId().equals(newQuay.getNetexId()) && 
                        oldQuay.getVersion() != newQuay.getVersion()) {
                        copyInfoSpotLinksToNewVersion(oldQuay.getNetexId(), oldQuay.getVersion(),
                            newQuay.getVersion(), excludeInfoSpotNetexId);
                    }
                }
            }
        }

        // Copy for children (terminals)
        if (oldStopPlace.getChildren() != null && newStopPlace.getChildren() != null) {
            for (StopPlace oldChild : oldStopPlace.getChildren()) {
                for (StopPlace newChild : newStopPlace.getChildren()) {
                    if (oldChild.getNetexId().equals(newChild.getNetexId())) {
                        maintainInfoSpotLinkagesExcluding(oldChild, newChild, excludeInfoSpotNetexId);
                    }
                }
            }
        }
    }

    /**
     * Copy InfoSpot linkages from one entity version to another.
     * Finds all InfoSpots linked to the old version and adds the new version to their locationRefs.
     */
    private void copyInfoSpotLinksToNewVersion(String netexId, Long oldVersion, Long newVersion, String excludeInfoSpotNetexId) {
        List<InfoSpot> linkedSpots = infoSpotRepository.findForAssociationWithVersion(netexId, oldVersion);
        logger.debug("Found {} InfoSpots linked to {}:{}", linkedSpots.size(), netexId, oldVersion);

        for (InfoSpot spot : linkedSpots) {
            // Skip the excluded InfoSpot (used when deleting)
            if (excludeInfoSpotNetexId != null && excludeInfoSpotNetexId.equals(spot.getNetexId())) {
                logger.debug("Skipping InfoSpot {} (excluded from copy)", spot.getNetexId());
                continue;
            }

            // Get the managed collection
            Set<InfoSpotLocationRef> refs = spot.getLocationRefs();
            if (refs == null) {
                refs = new HashSet<>();
                spot.setLocationRefs(refs);
            }

            // Add the new linkage - this is additive, doesn't remove old refs
            InfoSpotLocationRef newRef = new InfoSpotLocationRef(netexId, String.valueOf(newVersion));
            refs.add(newRef);
        }

        logger.debug("Copied {} InfoSpot linkages from {}:{} to {}:{}",
            linkedSpots.size(), netexId, oldVersion, netexId, newVersion);
    }
}

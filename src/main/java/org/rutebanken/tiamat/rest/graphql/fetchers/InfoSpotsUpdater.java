package org.rutebanken.tiamat.rest.graphql.fetchers;

import com.google.api.client.util.Preconditions;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.rutebanken.helper.organisation.ReflectionAuthorizationService;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.model.DisplayTypeEnumeration;
import org.rutebanken.tiamat.model.InfoSpot;
import org.rutebanken.tiamat.model.InfoSpotPoster;
import org.rutebanken.tiamat.model.InfoSpotPosterRef;
import org.rutebanken.tiamat.model.InfoSpotLocationRef;
import org.rutebanken.tiamat.model.InfoSpotTypeEnumeration;
import org.rutebanken.tiamat.model.PosterSizeEnumeration;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.InfoSpotPosterRepository;
import org.rutebanken.tiamat.repository.InfoSpotRepository;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.ShelterEquipmentRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.rest.graphql.mappers.GeometryMapper;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.VersionIncrementor;
import org.rutebanken.tiamat.versioning.save.InfoSpotPosterVersionedSaverService;
import org.rutebanken.tiamat.versioning.save.InfoSpotVersionedSaverService;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.rutebanken.helper.organisation.AuthorizationConstants.ROLE_EDIT_STOPS;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.BACKLIGHT;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.DESCRIPTION;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.DISPLAY_TYPE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.FLOOR;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.GEOMETRY;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.HEIGHT;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.ID;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.INFO_SPOT_LOCATIONS;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.INFO_SPOT_TYPE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.LABEL;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.LINES;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.MAINTENANCE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.OUTPUT_TYPE_INFO_SPOT;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.OUTPUT_TYPE_POSTER;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.POSTER_PLACE_SIZE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.POSTER_SIZE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PURPOSE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.RAIL_INFORMATION;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.SPEECH_PROPERTY;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.WIDTH;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.ZONE_LABEL;
import static org.rutebanken.tiamat.rest.graphql.mappers.EmbeddableMultilingualStringMapper.getEmbeddableString;

@Service("infoSpotsUpdater")
@Transactional
public class InfoSpotsUpdater implements DataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(InfoSpotsUpdater.class);

    @Autowired
    private InfoSpotRepository infoSpotRepository;

    @Autowired
    private InfoSpotPosterRepository infoSpotPosterRepository;

    @Autowired
    private QuayRepository quayRepository;

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private ShelterEquipmentRepository shelterEquipmentRepository;

    @Autowired
    private ReflectionAuthorizationService authorizationService;

    @Autowired
    private InfoSpotVersionedSaverService infoSpotVersionedSaverService;

    @Autowired
    private InfoSpotPosterVersionedSaverService infoSpotPosterVersionedSaverService;

    @Autowired
    private VersionCreator versionCreator;

    @Autowired
    private VersionIncrementor versionIncrementor;

    @Autowired
    private GeometryMapper geometryMapper;

    @Autowired
    private StopPlaceVersionedSaverService stopPlaceVersionedSaverService;

    @Autowired
    private UsernameFetcher usernameFetcher;

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        List<Map> input = environment.getArgument(OUTPUT_TYPE_INFO_SPOT);

        if (input != null) {
            return input.stream()
                    .map(this::createOrUpdateInfoSpot)
                    .toList();
        }
        return null;
    }

    private InfoSpot createOrUpdateInfoSpot(Map input) {

        InfoSpot updatedInfoSpot;
        InfoSpot existingVersion = null;
        String netexId = (String) input.get(ID);
        if (netexId != null) {
            logger.info("Updating Info Spot {}", netexId);
            existingVersion = infoSpotRepository.findFirstByNetexIdOrderByVersionDesc(netexId);
            Preconditions.checkArgument(existingVersion != null, "Attempting to update InfoSpot [id = %s], but InfoSpot does not exist.", netexId);
            updatedInfoSpot = versionCreator.createCopy(existingVersion, InfoSpot.class);
        } else {
            logger.info("Creating new InfoSpot");
            updatedInfoSpot = new InfoSpot();
        }
        boolean isUpdated = populateInfoSpot(input, updatedInfoSpot);

        if (isUpdated) {
            authorizationService.assertAuthorized(ROLE_EDIT_STOPS, Arrays.asList(existingVersion, updatedInfoSpot));

            // Check if this is a deletion
            boolean isDeletion = (updatedInfoSpot.getLocationRefs() == null || updatedInfoSpot.getLocationRefs().isEmpty()) 
                && existingVersion != null 
                && existingVersion.getLocationRefs() != null 
                && !existingVersion.getLocationRefs().isEmpty();

            if (isDeletion) {
                // For deletion: Version entities WITHOUT copying this InfoSpot
                logger.info("InfoSpot {} is being deleted - versioning entities without copying this spot", netexId);

                // Get UNIQUE entity netexIds to version (deduplicate to avoid versioning the same entity multiple times)
                Set<String> uniqueEntitiesToVersion = existingVersion.getLocationRefs().stream()
                    .map(InfoSpotLocationRef::getRef)
                    .filter(ref -> ref.contains(":Quay:") || ref.contains(":StopPlace:"))
                    .collect(Collectors.toSet());

                logger.info("InfoSpot {} linked to {} unique entities", netexId, uniqueEntitiesToVersion.size());

                // Version each UNIQUE entity once, excluding this InfoSpot from being copied
                for (String entityNetexId : uniqueEntitiesToVersion) {
                    try {
                        versionLinkedLocationExcludingInfoSpot(entityNetexId, existingVersion.getNetexId());
                        logger.info("Versioned {} (excluded InfoSpot {} from copy)", entityNetexId, netexId);
                    } catch (Exception e) {
                        logger.warn("Failed to version {} during InfoSpot deletion: {}",
                            entityNetexId, e.getMessage());
                    }
                }

                versionIncrementor.initiateOrIncrementInfoSpot(updatedInfoSpot);
                updatedInfoSpot = infoSpotVersionedSaverService.saveNewVersion(updatedInfoSpot);

                return updatedInfoSpot;
            } else {
                // For create/update: Version ALL linked entities and update refs to new versions
                // If updating existing InfoSpot, exclude it from automatic copying
                String excludeInfoSpotForUpdate = (existingVersion != null) ? existingVersion.getNetexId() : null;
                versionAllLinkedEntitiesAndUpdateRefs(updatedInfoSpot, excludeInfoSpotForUpdate);
                versionIncrementor.initiateOrIncrementInfoSpot(updatedInfoSpot);

                logger.info("Saving new version of InfoSpot {}", updatedInfoSpot);
                updatedInfoSpot = infoSpotVersionedSaverService.saveNewVersion(updatedInfoSpot);

                return updatedInfoSpot;
            }
        } else {
            logger.info("No changes - InfoSpot {} NOT updated", netexId);
        }
        return existingVersion;

    }

    private boolean populateInfoSpot(Map input, InfoSpot target) {
        boolean isUpdated = false;

        if (input.containsKey(LABEL)) {
            var label = (String) input.get(LABEL);
            isUpdated = !Objects.equals(label, target.getLabel());
            target.setLabel(label);
        }
        if (input.containsKey(INFO_SPOT_TYPE)) {
            var infoSpotType = (InfoSpotTypeEnumeration) input.get(INFO_SPOT_TYPE);
            isUpdated |= !Objects.equals(infoSpotType, target.getInfoSpotType());
            target.setInfoSpotType(infoSpotType);
        }
        if (input.containsKey(PURPOSE)) {
            var purpose = (String) input.get(PURPOSE);
            isUpdated |= !Objects.equals(purpose, target.getPurpose());
            target.setPurpose(purpose);
        }
        if (input.containsKey(DESCRIPTION)) {
            var description = (Map) input.get(DESCRIPTION);
            var newDescription = getEmbeddableString(description);
            var oldDescription = target.getDescription();

            // Compare the actual text values, not object references to avoid unnecessary versioning
            String oldValue = oldDescription != null ? oldDescription.getValue() : null;
            String newValue = newDescription != null ? newDescription.getValue() : null;
            String oldLang = oldDescription != null ? oldDescription.getLang() : null;
            String newLang = newDescription != null ? newDescription.getLang() : null;

            isUpdated |= !Objects.equals(oldValue, newValue) || !Objects.equals(oldLang, newLang);
            target.setDescription(newDescription);
        }

        if (input.containsKey(POSTER_PLACE_SIZE)) {
            var posterPlaceSize = (PosterSizeEnumeration) input.get(POSTER_PLACE_SIZE);
            isUpdated |= populateInfoSpotSize(PosterSizeEnumeration.toSizeMap(posterPlaceSize), target);
        } else {
            isUpdated |= populateInfoSpotSize(input, target);
        }

        if (input.containsKey(BACKLIGHT)) {
            var backlight = (Boolean) input.get(BACKLIGHT);
            isUpdated |= !Objects.equals(backlight, target.getBacklight());
            target.setBacklight(backlight);
        }
        if (input.containsKey(MAINTENANCE)) {
            var maintenance = (String) input.get(MAINTENANCE);
            isUpdated |= !Objects.equals(maintenance, target.getMaintenance());
            target.setMaintenance(maintenance);
        }
        if (input.containsKey(ZONE_LABEL)) {
            var zoneLabel = (String) input.get(ZONE_LABEL);
            isUpdated |= !Objects.equals(zoneLabel, target.getZoneLabel());
            target.setZoneLabel(zoneLabel);
        }
        if (input.containsKey(RAIL_INFORMATION)) {
            var railInformation = (String) input.get(RAIL_INFORMATION);
            isUpdated |= !Objects.equals(railInformation, target.getRailInformation());
            target.setRailInformation(railInformation);
        }
        if (input.containsKey(FLOOR)) {
            var floor = (String) input.get(FLOOR);
            isUpdated |= !Objects.equals(floor, target.getFloor());
            target.setFloor(floor);
        }
        if (input.containsKey(SPEECH_PROPERTY)) {
            var speechProperty = (Boolean) input.get(SPEECH_PROPERTY);
            isUpdated |= !Objects.equals(speechProperty, target.getSpeechProperty());
            target.setSpeechProperty(speechProperty);
        }
        if (input.containsKey(DISPLAY_TYPE)) {
            var displayType = (DisplayTypeEnumeration) input.get(DISPLAY_TYPE);
            isUpdated |= !Objects.equals(displayType, target.getDisplayType());
            target.setDisplayType(displayType);
        }

        // Handle location references
        if (input.containsKey(INFO_SPOT_LOCATIONS)) {
            List<String> locationList = (List<String>) input.get(INFO_SPOT_LOCATIONS);

            Set<InfoSpotLocationRef> newLocationRefs = locationList.stream()
                    .map(this::convertLocationStringToRef)
                    .collect(Collectors.toSet());

            // Only mark as updated if the entity IDs changed, not just version numbers
            // (version numbers change when entities are versioned in the same batch)
            if (target.getLocationRefs() != null && !target.getLocationRefs().isEmpty()) {
                Set<String> oldEntityIds = target.getLocationRefs().stream()
                        .map(InfoSpotLocationRef::getRef)
                        .collect(Collectors.toSet());
                Set<String> newEntityIds = newLocationRefs.stream()
                        .map(InfoSpotLocationRef::getRef)
                        .collect(Collectors.toSet());

                boolean locationsChanged = !oldEntityIds.equals(newEntityIds);

                isUpdated |= locationsChanged;
            } else {
                // New InfoSpot or no existing refs - this is an update
                boolean hasNewRefs = !newLocationRefs.isEmpty();

                isUpdated |= hasNewRefs;
            }

            // Always update to have current version numbers
            target.setLocationRefs(newLocationRefs);
        }

        if (input.containsKey(OUTPUT_TYPE_POSTER)) {
            List<Map> posters = (List<Map>) input.get(OUTPUT_TYPE_POSTER);
            List<InfoSpotPosterRef> oldPosters = target.getPosters();

            if (posters != null) {
                List<InfoSpotPoster> existingPosters = oldPosters.stream()
                        .map(p -> infoSpotPosterRepository.findFirstByNetexIdOrderByVersionDesc(p.getRef()))
                        .collect(Collectors.toList());

                List<InfoSpotPosterRef> updatedPosters = posters.stream()
                        .map(p -> createPoster(p, existingPosters))
                        .map(InfoSpotPosterRef::new)
                        .collect(Collectors.toList());

                target.setPosters(updatedPosters);

                // Check if poster refs OR versions changed
                // (poster content changes result in new version numbers even if NetEx ID stays the same)
                Set<String> oldPosterKeys = oldPosters.stream()
                        .map(p -> p.getRef() + "@" + p.getVersion())
                        .collect(Collectors.toSet());
                Set<String> newPosterKeys = updatedPosters.stream()
                        .map(p -> p.getRef() + "@" + p.getVersion())
                        .collect(Collectors.toSet());
                isUpdated |= !oldPosterKeys.equals(newPosterKeys);
            } else {
                isUpdated |= !oldPosters.isEmpty();
                target.setPosters(Collections.emptyList());
            }
        }
        if (input.containsKey(GEOMETRY)) {
            org.locationtech.jts.geom.Point newGeometry = geometryMapper.createGeoJsonPoint((Map) input.get(GEOMETRY));
            isUpdated |= !Objects.equals(target.getCentroid(), newGeometry);
            target.setCentroid(newGeometry);
        }

        return isUpdated;
    }

    private boolean populateInfoSpotSize(Map input, InfoSpot target) {
        boolean isUpdated = false;

        if (input.containsKey(WIDTH) && !Objects.equals(target.getWidth(), input.get(WIDTH))) {
            target.setWidth((Integer) input.get(WIDTH));
            isUpdated = true;
        }

        if (input.containsKey(HEIGHT) && !Objects.equals(target.getHeight(), input.get(HEIGHT))) {
            target.setHeight((Integer) input.get(HEIGHT));
            isUpdated = true;
        }

        return isUpdated;
    }

    /**
     * Convert old format location string to versioned ref by resolving current version
     */
    private InfoSpotLocationRef convertLocationStringToRef(String netexId) {
        String version = null;

        if (netexId.contains(":Quay:")) {
            var quay = quayRepository.findFirstByNetexIdOrderByVersionDesc(netexId);
            if (quay != null) {
                version = String.valueOf(quay.getVersion());
            }
        } else if (netexId.contains(":StopPlace:")) {
            var stopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(netexId);
            if (stopPlace != null) {
                version = String.valueOf(stopPlace.getVersion());
            }
        } else if (netexId.contains(":ShelterEquipment:")) {
            var equipment = shelterEquipmentRepository.findFirstByNetexIdOrderByVersionDesc(netexId);
            if (equipment != null) {
                version = String.valueOf(equipment.getVersion());
            }
        }

        return new InfoSpotLocationRef(netexId, version);
    }

    private InfoSpotPoster createPoster(Map input, List<InfoSpotPoster> existingPosters) {
        if (input.containsKey(LABEL)) {
            boolean isUpdated = false;
            String label = (String) input.get(LABEL);
            var poster = existingPosters.stream()
                    .filter(p -> p.getLabel().equals(label))
                    .findFirst()
                    .map(isp ->
                        versionCreator.createCopy(isp, InfoSpotPoster.class)
                    )
                    .orElseGet(() -> {
                        var isp = new InfoSpotPoster();
                        isp.setLabel(label);
                        return isp;
                    });

            if (input.containsKey(LINES) && !Objects.equals(poster.getLines(), input.get(LINES))) {
                poster.setLines((String) input.get(LINES));
                isUpdated = true;
            }

            if (input.containsKey(POSTER_SIZE)) {
                var posterSize = (PosterSizeEnumeration) input.get(POSTER_SIZE);
                isUpdated |= populatePosterSize(PosterSizeEnumeration.toSizeMap(posterSize), poster);
            } else {
                isUpdated |= populatePosterSize(input, poster);
            }

            if (input.containsKey(WIDTH) && !Objects.equals(poster.getWidth(), input.get(WIDTH))) {
                poster.setWidth((Integer) input.get(WIDTH));
                isUpdated = true;
            }

            if (input.containsKey(HEIGHT) && !Objects.equals(poster.getHeight(), input.get(HEIGHT))) {
                poster.setHeight((Integer) input.get(HEIGHT));
                isUpdated = true;
            }

            if (isUpdated) {
                return infoSpotPosterVersionedSaverService.saveNewVersion(poster);
            }
            return poster;
        }
        else {
            throw new IllegalArgumentException("Expected label for poster, none provided");
        }
    }

    private boolean populatePosterSize(Map input, InfoSpotPoster target) {
        boolean isUpdated = false;

        if (input.containsKey(WIDTH) && !Objects.equals(target.getWidth(), input.get(WIDTH))) {
            target.setWidth((Integer) input.get(WIDTH));
            isUpdated = true;
        }

        if (input.containsKey(HEIGHT) && !Objects.equals(target.getHeight(), input.get(HEIGHT))) {
            target.setHeight((Integer) input.get(HEIGHT));
            isUpdated = true;
        }

        return isUpdated;
    }

    /**
     * Version ALL linked entities before saving InfoSpot, and update locationRefs to point to new versions.
     * This ensures that when an InfoSpot is created/updated, all linked Quays/StopPlaces get new versions
     * and the info_spot_location junction table entries point to those new versions.
     *
     * @param infoSpot The InfoSpot being created/updated
     * @param excludeInfoSpotNetexId The netexId of the InfoSpot to exclude from automatic copying (null for new InfoSpots)
     */
    private void versionAllLinkedEntitiesAndUpdateRefs(InfoSpot infoSpot, String excludeInfoSpotNetexId) {
        if (infoSpot.getLocationRefs() == null || infoSpot.getLocationRefs().isEmpty()) {
            return;
        }

        logger.info("Versioning ALL linked entities for InfoSpot before saving");

        // Get UNIQUE entity netexIds that need versioning (deduplicate to avoid versioning the same entity multiple times)
        Set<String> uniqueEntitiesToVersion = infoSpot.getLocationRefs().stream()
            .map(InfoSpotLocationRef::getRef)
            .filter(ref -> ref.contains(":Quay:") || ref.contains(":StopPlace:"))
            .collect(Collectors.toSet());

        if (uniqueEntitiesToVersion.isEmpty()) {
            return;
        }

        logger.info("Versioning {} unique entities{}", uniqueEntitiesToVersion.size(),
            excludeInfoSpotNetexId != null ? " (excluding InfoSpot " + excludeInfoSpotNetexId + " from copy)" : "");

        // Version each UNIQUE entity once and collect the new version numbers
        Map<String, String> netexIdToNewVersion = new HashMap<>();
        for (String netexId : uniqueEntitiesToVersion) {
            try {
                String newVersionNumber = versionLinkedLocationExcludingInfoSpot(netexId, excludeInfoSpotNetexId);
                netexIdToNewVersion.put(netexId, newVersionNumber);
                logger.info("Versioned {} to version {}", netexId, newVersionNumber);
            } catch (Exception e) {
                logger.warn("Failed to version {}: {}", netexId, e.getMessage());
            }
        }

        // Update ALL refs to point to the new versions
        Set<InfoSpotLocationRef> updatedRefs = new HashSet<>();
        for (InfoSpotLocationRef ref : infoSpot.getLocationRefs()) {
            String netexId = ref.getRef();
            if (netexIdToNewVersion.containsKey(netexId)) {
                // Update to new version
                updatedRefs.add(new InfoSpotLocationRef(netexId, netexIdToNewVersion.get(netexId)));
            } else {
                // Keep other refs (like ShelterEquipment) unchanged
                updatedRefs.add(ref);
            }
        }

        infoSpot.setLocationRefs(updatedRefs);
    }

    /**
     * Version the linked StopPlace for a given location reference, optionally excluding a specific InfoSpot from being copied.
     * Used for both InfoSpot creation/update (excludeInfoSpotNetexId = null) and deletion (excludeInfoSpotNetexId = the deleted spot).
     * Returns the new version number as a String.
     */
    private String versionLinkedLocationExcludingInfoSpot(String netexId, String excludeInfoSpotNetexId) {
        if (netexId.contains(":Quay:")) {
            return versionStopPlaceFromQuayReferenceForInfoSpot(netexId, excludeInfoSpotNetexId);
        } else {
            StopPlace stopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(netexId);
            if (stopPlace == null) {
                throw new IllegalStateException("Could not find StopPlace " + netexId);
            }
            return versionStopPlaceForInfoSpot(stopPlace, null, excludeInfoSpotNetexId);
        }
    }

    /**
     * Version the parent StopPlace for a Quay reference, FOR InfoSpot operations (includes linkage copying).
     * Returns the new version number of the Quay as a String.
     * @throws IllegalStateException if the Quay or its parent StopPlace cannot be found
     */
    private String versionStopPlaceFromQuayReferenceForInfoSpot(String quayNetexId, String excludeInfoSpotNetexId) {
        Quay quay = quayRepository.findFirstByNetexIdOrderByVersionDesc(quayNetexId);
        if (quay == null) {
            throw new IllegalStateException("Could not find Quay " + quayNetexId);
        }

        StopPlace parent = stopPlaceRepository.findByQuay(quay);
        if (parent == null) {
            throw new IllegalStateException("Could not find parent StopPlace for Quay " + quayNetexId);
        }

        StopPlace topLevel = getTopLevelStopPlace(parent);
        versionStopPlaceForInfoSpot(topLevel, quayNetexId, excludeInfoSpotNetexId);

        // Fetch the new version of the quay after versioning
        Quay newQuayVersion = quayRepository.findFirstByNetexIdOrderByVersionDesc(quayNetexId);
        return String.valueOf(newQuayVersion.getVersion());
    }

    /**
     * Create and save a new version of the StopPlace, FOR InfoSpot operations (includes linkage copying).
     * Returns the new version number as a String.
     */
    private String versionStopPlaceForInfoSpot(StopPlace stopPlace, String modifiedQuayNetexId, String excludeInfoSpotNetexId) {
        logger.info("Incrementing version for StopPlace {} (for InfoSpot operation)", stopPlace.getNetexId());

        StopPlace newVersion = versionCreator.createCopy(stopPlace, StopPlace.class);

        if (modifiedQuayNetexId != null) {
            markQuayAsModified(newVersion, modifiedQuayNetexId);
        }

        StopPlace savedVersion = stopPlaceVersionedSaverService.saveNewVersion(stopPlace, newVersion, excludeInfoSpotNetexId);

        return String.valueOf(savedVersion.getVersion());
    }

    /**
     * Mark a quay as modified in the given StopPlace or its children.
     */
    private void markQuayAsModified(StopPlace stopPlace, String quayNetexId) {
        Instant now = Instant.now();
        String changedBy = usernameFetcher.getUserNameForAuthenticatedUser();

        // Try direct quays first
        Quay foundQuay = findQuayInStopPlace(stopPlace, quayNetexId);

        // If not found, try children's quays (for terminal/parent stop places)
        if (foundQuay == null && stopPlace.getChildren() != null) {
            for (StopPlace child : stopPlace.getChildren()) {
                foundQuay = findQuayInStopPlace(child, quayNetexId);
                if (foundQuay != null) {
                    break;
                }
            }
        }

        if (foundQuay != null) {
            foundQuay.setChanged(now);
            foundQuay.setChangedBy(changedBy);
        }
    }

    /**
     * Find a quay by netexId in the given StopPlace's direct quays.
     */
    private Quay findQuayInStopPlace(StopPlace stopPlace, String quayNetexId) {
        if (stopPlace.getQuays() == null) {
            return null;
        }

        return stopPlace.getQuays().stream()
            .filter(q -> quayNetexId.equals(q.getNetexId()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get the top-level StopPlace. If the given StopPlace is a child of a parent/terminal,
     * returns the parent. Otherwise returns the StopPlace itself.
     * @throws IllegalStateException if parent reference is invalid or parent is not found
     */
    private StopPlace getTopLevelStopPlace(StopPlace stopPlace) {
        if (stopPlace.getParentSiteRef() == null) {
            return stopPlace;
        }

        String parentRef = stopPlace.getParentSiteRef().getRef();
        String parentVersion = stopPlace.getParentSiteRef().getVersion();

        if (parentVersion == null) {
            throw new IllegalStateException("StopPlace " + stopPlace.getNetexId() +
                " has parent reference with null version");
        }

        StopPlace parent = stopPlaceRepository.findFirstByNetexIdAndVersion(
            parentRef,
            Long.parseLong(parentVersion)
        );

        if (parent != null) {
            logger.debug("StopPlace {} is a child of parent {}, will version parent instead", 
                stopPlace.getNetexId(), parent.getNetexId());
            return parent;
        }

        throw new IllegalStateException("StopPlace " + stopPlace.getNetexId() +
            " has parent reference but parent not found: " + parentRef);
    }
}

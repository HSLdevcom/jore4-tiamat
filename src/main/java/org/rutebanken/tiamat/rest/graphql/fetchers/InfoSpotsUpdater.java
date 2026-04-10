package org.rutebanken.tiamat.rest.graphql.fetchers;

import com.google.api.client.util.Preconditions;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
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

            versionIncrementor.initiateOrIncrementInfoSpot(updatedInfoSpot);

            logger.info("Saving new version of InfoSpot {}", updatedInfoSpot);
            updatedInfoSpot = infoSpotVersionedSaverService.saveNewVersion(updatedInfoSpot);

            incrementVersionForLinkedEntities(updatedInfoSpot, existingVersion);

            return updatedInfoSpot;
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
            isUpdated |= !Objects.equals(description, target.getDescription());
            target.setDescription(getEmbeddableString(description));
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

        // Handle location references - auto-resolve to current version
        if (input.containsKey(INFO_SPOT_LOCATIONS)) {
            Set<InfoSpotLocationRef> newLocationRefs = ((List<String>) input.get(INFO_SPOT_LOCATIONS)).stream()
                    .map(this::convertLocationStringToRef)
                    .collect(Collectors.toSet());

            isUpdated |= !Objects.equals(target.getLocationRefs(), newLocationRefs);
            target.setLocationRefs(newLocationRefs);
        }

        if (input.containsKey(OUTPUT_TYPE_POSTER)) {
            List<Map> posters = (List<Map>) input.get(OUTPUT_TYPE_POSTER);
            if (posters != null) {
                List<InfoSpotPosterRef> posterRefs = target.getPosters();

                List<InfoSpotPoster> existingPosters = posterRefs.stream()
                        .map(p -> infoSpotPosterRepository.findFirstByNetexIdOrderByVersionDesc(p.getRef()))
                        .collect(Collectors.toList());

                List<InfoSpotPosterRef> updatedPosters = posters.stream()
                        .map(p -> createPoster(p, existingPosters))
                        .map(InfoSpotPosterRef::new)
                        .collect(Collectors.toList());

                target.setPosters(updatedPosters);
                isUpdated = true;
            }
            else {
                target.setPosters(Collections.emptyList());
            }
        }
        if (input.containsKey(GEOMETRY)) {
            target.setCentroid(geometryMapper.createGeoJsonPoint((Map) input.get(GEOMETRY)));
            isUpdated = true;
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
     * Increment version for linked entities when InfoSpot is updated.
     */
    private void incrementVersionForLinkedEntities(InfoSpot savedInfoSpot, InfoSpot existingInfoSpot) {
        if (savedInfoSpot.getLocationRefs() == null || savedInfoSpot.getLocationRefs().isEmpty()) {
            return;
        }

        logger.info("Incrementing versions for locations linked to InfoSpot {}", savedInfoSpot.getNetexId());

        Set<String> versionedStopPlaces = new HashSet<>();

        for (InfoSpotLocationRef locationRef : savedInfoSpot.getLocationRefs()) {
            String netexId = locationRef.getRef();

            if (netexId.contains(":Quay:")) {
                handleQuayVersioning(netexId, versionedStopPlaces);
            } else if (netexId.contains(":StopPlace:")) {
                handleStopPlaceVersioning(netexId, versionedStopPlaces);
            }
        }
    }

    /**
     * Handle versioning for a Quay reference by versioning its parent StopPlace.
     */
    private void handleQuayVersioning(String quayNetexId, Set<String> versionedStopPlaces) {
        Quay quay = quayRepository.findFirstByNetexIdOrderByVersionDesc(quayNetexId);
        if (quay == null) {
            logger.warn("Could not find Quay {}", quayNetexId);
            return;
        }

        StopPlace parent = stopPlaceRepository.findByQuay(quay);
        if (parent == null) {
            logger.warn("Could not find parent StopPlace for Quay {}", quayNetexId);
            return;
        }

        versionStopPlaceIfNeeded(parent, quayNetexId, versionedStopPlaces);
    }

    /**
     * Handle versioning for a StopPlace reference.
     */
    private void handleStopPlaceVersioning(String stopPlaceNetexId, Set<String> versionedStopPlaces) {
        StopPlace stopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(stopPlaceNetexId);
        if (stopPlace == null) {
            logger.warn("Could not find StopPlace {}", stopPlaceNetexId);
            return;
        }

        versionStopPlaceIfNeeded(stopPlace, null, versionedStopPlaces);
    }

    /**
     * Version a StopPlace if not already versioned in this operation.
     * Automatically handles child stops by versioning their parent terminal instead.
     */
    private void versionStopPlaceIfNeeded(StopPlace stopPlace, String modifiedQuayNetexId, Set<String> versionedStopPlaces) {
        StopPlace topLevel = getTopLevelStopPlace(stopPlace);
        String stopPlaceId = topLevel.getNetexId();

        if (versionedStopPlaces.contains(stopPlaceId)) {
            return;
        }

        logger.info("Incrementing version for StopPlace {}", stopPlaceId);

        StopPlace newVersion = versionCreator.createCopy(topLevel, StopPlace.class);

        if (modifiedQuayNetexId != null) {
            markQuayAsModified(newVersion, modifiedQuayNetexId);
        }

        stopPlaceVersionedSaverService.saveNewVersion(topLevel, newVersion);
        versionedStopPlaces.add(stopPlaceId);
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
     */
    private StopPlace getTopLevelStopPlace(StopPlace stopPlace) {
        if (stopPlace.getParentSiteRef() == null) {
            return stopPlace;
        }

        StopPlace parent = stopPlaceRepository.findFirstByNetexIdAndVersion(
            stopPlace.getParentSiteRef().getRef(),
            Long.parseLong(stopPlace.getParentSiteRef().getVersion())
        );

        if (parent != null) {
            logger.debug("StopPlace {} is a child of parent {}, will version parent instead", 
                stopPlace.getNetexId(), parent.getNetexId());
            return parent;
        }

        logger.warn("StopPlace {} has parent reference but parent not found, versioning child", 
            stopPlace.getNetexId());
        return stopPlace;
    }
}

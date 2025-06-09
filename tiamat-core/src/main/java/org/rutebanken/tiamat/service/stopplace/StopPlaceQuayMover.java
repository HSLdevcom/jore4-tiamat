/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.service.stopplace;

import org.rutebanken.tiamat.geo.StopPlaceCentroidComputer;
import org.rutebanken.tiamat.lock.MutateLock;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.ValidBetween;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;
import org.rutebanken.tiamat.versioning.util.CopiedEntity;
import org.rutebanken.tiamat.versioning.util.StopPlaceCopyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Transactional
@Component
public class StopPlaceQuayMover {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceQuayMover.class);

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private QuayRepository quayRepository;

    @Autowired
    private StopPlaceCentroidComputer stopPlaceCentroidComputer;

    @Autowired
    private StopPlaceVersionedSaverService stopPlaceVersionedSaverService;

    @Autowired
    private StopPlaceCopyHelper stopPlaceCopyHelper;

    @Autowired
    private VersionCreator versionCreator;

    @Autowired
    private MutateLock mutateLock;

    public StopPlace moveQuays(List<String> quayIds, String destinationStopPlaceId, Instant moveQuayFromDate, String fromVersionComment, String toVersionComment) {

        return mutateLock.executeInLock(() -> {
            Set<Quay> quays = resolveQuays(quayIds);

            Set<StopPlace> sourceStopPlaces = resolveSourceStopPlaces(quays);
            verifySize(quayIds, sourceStopPlaces);

            StopPlace sourceStopPlace = sourceStopPlaces.iterator().next();

            logger.debug("Found stop place to move quays {} from {}", quayIds, sourceStopPlace);

            Set<Quay> quaysToAdd = modifySourceQuaysValidityDates(sourceStopPlace, fromVersionComment, quayIds, moveQuayFromDate);
            StopPlace response = addQuaysToDestinationStop(destinationStopPlaceId, quaysToAdd, toVersionComment, moveQuayFromDate);

            logger.info("Moved quays: {} from stop {} to {}", quayIds, sourceStopPlace.getNetexId(), response.getNetexId());
            return response;
        });
    }

    private StopPlace addQuaysToDestinationStop(String destinationStopPlaceId, Set<Quay> quaysToAdd, String toVersionComment, Instant moveDate) {
        StopPlace destinationStopPlace;
        if (destinationStopPlaceId == null) {
            destinationStopPlace = new StopPlace();
        } else {
            destinationStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(destinationStopPlaceId);
        }

        if (destinationStopPlace == null) {
            throw new IllegalArgumentException("Cannot resolve destination stop place by ID " + destinationStopPlaceId);
        }

        CopiedEntity<StopPlace> destination = stopPlaceCopyHelper.createCopies(destinationStopPlace);
        StopPlace stopPlaceToAddQuaysTo = destination.getCopiedEntity();

        // Add correct validity dates to the given quays, so that they are valid starting from the move date
        for (Quay quay : quaysToAdd) {
            ValidBetween quayValidity = quay.getValidBetween();
            ValidBetween stopPlaceValidity = stopPlaceToAddQuaysTo.getValidBetween();

            Instant quayValidToDate = (quayValidity == null) ? null : quayValidity.getToDate();
            Instant stopPlaceValidTo =  (stopPlaceValidity == null) ? null : stopPlaceValidity.getToDate();

            // If quay is valid after the stop place, set the quay validity to end on the same time as the stop place
            if (quayValidToDate != null && stopPlaceValidTo != null && quayValidToDate.isAfter(stopPlaceValidTo)) {
                quayValidToDate = stopPlaceValidTo;
            }

            quay.setValidBetween(new ValidBetween(moveDate, quayValidToDate));
        }

        stopPlaceToAddQuaysTo.getQuays().addAll(quaysToAdd);
        stopPlaceCentroidComputer.computeCentroidForStopPlace(stopPlaceToAddQuaysTo);
        stopPlaceToAddQuaysTo.setVersionComment(toVersionComment);

        logger.debug("Saved stop place with new quays {} {}", quaysToAdd, destinationStopPlace);
        return save(destination, moveDate);
    }

    private Set<Quay> modifySourceQuaysValidityDates(StopPlace sourceStopPlace, String fromVersionComment, List<String> quayIds, Instant moveDate) {
        CopiedEntity<StopPlace> source = stopPlaceCopyHelper.createCopies(sourceStopPlace);
        StopPlace stopPlaceToModifyQuays = source.getCopiedEntity();

        Set<Quay> quaysToAdd = new HashSet<>();

        // Modify the validity of the given quays to end on the given moveDate
        for (Quay quay : stopPlaceToModifyQuays.getQuays()) {
            if (quayIds.contains(quay.getNetexId())) {
                Quay copiedQuay = versionCreator.createCopy(quay, Quay.class);
                copiedQuay.setVersion(0); // Set version to 0, as null is not supported, it will be incremented to 1 when saved
                copiedQuay.setNetexId(null);
                quaysToAdd.add(copiedQuay);

                ValidBetween currentValidity = quay.getValidBetween();
                Instant quayValidFromDate = (currentValidity == null) ? null : currentValidity.getFromDate();
                quay.setValidBetween(new ValidBetween(quayValidFromDate, moveDate));
            }
        }
        stopPlaceToModifyQuays.setVersionComment(fromVersionComment);

        logger.debug("Modified validity of quays {} {}", quayIds, stopPlaceToModifyQuays);
        save(source, moveDate);

        return quaysToAdd;
    }

    /**
     * Saves parent copy if parent exist. If not, saves monomodal stop place.
     *
     * @param copiedEntity
     * @param moveDate
     * @return
     */
    private StopPlace save(CopiedEntity<StopPlace> copiedEntity, Instant moveDate) {
        if (copiedEntity.hasParent()) {
            return stopPlaceVersionedSaverService.saveNewVersion(copiedEntity.getExistingParent(), copiedEntity.getCopiedParent(), moveDate);
        } else
            return stopPlaceVersionedSaverService.saveNewVersion(copiedEntity.getExistingEntity(), copiedEntity.getCopiedEntity(), moveDate);
    }

    private void verifySize(List<String> quayIds, Set<StopPlace> sourceStopPlaces) {
        if (sourceStopPlaces.size() > 1) {
            throw new IllegalArgumentException("Cannot move quay(s) " + quayIds + " from different stops " + sourceStopPlaces);
        }
    }

    private Set<Quay> resolveQuays(List<String> quayIds) {
        return quayIds.stream()
                .map(quayId -> quayRepository.findFirstByNetexIdOrderByVersionDesc(quayId))
                .peek(quay -> {
                    if (quay == null) {
                        throw new IllegalArgumentException("Could not resolve quays from list" + quayIds);
                    }
                })
                .collect(toSet());
    }

    private Set<StopPlace> resolveSourceStopPlaces(Set<Quay> quays) {
        return quays.stream()
                .map(stopPlaceRepository::findByQuay)
                .collect(toSet());
    }
}

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
import org.rutebanken.tiamat.model.*;
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

import java.time.*;
import java.time.format.DateTimeFormatter;
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
            Set<StopPlace> sourceStopPlaces = resolveSourceStopPlaces(resolveQuays(quayIds));
            verifySize(quayIds, sourceStopPlaces);

            StopPlace sourceStopPlace = sourceStopPlaces.iterator().next();

            logger.debug("Found stop place to move quays {} from {}", quayIds, sourceStopPlace);

            // If move date is not given, use current time
            Instant moveDate = (moveQuayFromDate != null) ? moveQuayFromDate : Instant.now();

            Set<Quay> quaysToAdd = modifySourceQuaysValidityDates(sourceStopPlace, fromVersionComment, quayIds, moveDate);
            StopPlace response = addQuaysToDestinationStop(destinationStopPlaceId, quaysToAdd, toVersionComment, moveDate);

            logger.info("Moved quays: {} from stop {} to {}", quayIds, sourceStopPlace.getNetexId(), response.getNetexId());
            return response;
        });
    }

    private Set<Quay> modifySourceQuaysValidityDates(StopPlace sourceStopPlace, String fromVersionComment, List<String> quayIds, Instant moveDate) {
        CopiedEntity<StopPlace> source = stopPlaceCopyHelper.createCopies(sourceStopPlace);
        StopPlace stopPlaceToModifyQuays = source.getCopiedEntity();

        Set<Quay> quaysToAdd = new HashSet<>();

        // Modify the validity of the given quays to end on the given moveDate
        for (Quay quay : stopPlaceToModifyQuays.getQuays()) {
            if (quayIds.contains(quay.getNetexId())) {
                Quay copiedQuay = versionCreator.createCopy(quay, Quay.class);
                copiedQuay.resetNetexIds();
                quaysToAdd.add(copiedQuay);

                String validityEndDate = instantToDateString(moveDate);
                quay.getKeyValues().put("validityEnd", new Value(validityEndDate));
            }
        }
        stopPlaceToModifyQuays.setVersionComment(fromVersionComment);

        logger.debug("Modified validity of quays {} {}", quayIds, stopPlaceToModifyQuays);
        save(source, Instant.now());

        return quaysToAdd;
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
            quay.getKeyValues().put("validityStart", new Value(instantToDateString(moveDate)));

            Value stopPlaceValidityEnd = stopPlaceToAddQuaysTo.getKeyValues().get("validityEnd");

            // If the stop place has no validity end date, no changes are needed for the quay's validity end date
            if (stopPlaceValidityEnd == null) {
                continue;
            }

            Value quayValidityEnd = quay.getKeyValues().get("validityEnd");

            // If the quay has no validity end or its validity end is after the stop place's validity end,
            // set the quay's validity end to be the same as the stop place's.
            if (quayValidityEnd == null || isDateAfter(quayValidityEnd, stopPlaceValidityEnd)) {
                quay.getKeyValues().put("validityEnd", stopPlaceValidityEnd);
            }
        }

        stopPlaceToAddQuaysTo.getQuays().addAll(quaysToAdd);
        stopPlaceCentroidComputer.computeCentroidForStopPlace(stopPlaceToAddQuaysTo);
        stopPlaceToAddQuaysTo.setVersionComment(toVersionComment);

        logger.debug("Saved stop place with new quays {} {}", quaysToAdd, destinationStopPlace);
        return save(destination, Instant.now());
    }

    private String instantToDateString(Instant instant) {
        LocalDate localDate = LocalDate.ofInstant(instant, ZoneId.of("Europe/Helsinki"));
        return localDate.toString();
    }

    /**
     * Helper method to compare the dates from two Value objects.
     *
     * @param dateValue1 The first Value object containing a date string.
     * @param dateValue2 The second Value object containing a date string.
     * @return true if the date in dateValue1 is after the date in dateValue2, false otherwise.
     */
    private boolean isDateAfter(Value dateValue1, Value dateValue2) {
        String dateStr1 = dateValue1.getItems().stream().findFirst().orElse(null);
        String dateStr2 = dateValue2.getItems().stream().findFirst().orElse(null);

        // If either date string is missing, a comparison cannot be made.
        if (dateStr1 == null || dateStr2 == null) {
            return false;
        }

        LocalDate date1 = LocalDate.parse(dateStr1);
        LocalDate date2 = LocalDate.parse(dateStr2);

        return date1.isAfter(date2);
    }

    /**
     * Saves parent copy if a parent exists. If not, save monomodal stop place.
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

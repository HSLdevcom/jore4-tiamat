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
import org.rutebanken.tiamat.model.Value;
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
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    public StopPlace moveQuays(List<String> quayIds, String destinationStopPlaceId, LocalDate moveQuayFromDate, String fromVersionComment, String toVersionComment) {

        return mutateLock.executeInLock(() -> {
            Set<StopPlace> sourceStopPlaces = resolveSourceStopPlaces(resolveQuays(quayIds));
            verifySize(quayIds, sourceStopPlaces);

            if (moveQuayFromDate == null) {
                throw new IllegalArgumentException("Move quay from date is null");
            }

            if (moveQuayFromDate.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Selected move date is in the past.");
            }

            StopPlace sourceStopPlace = sourceStopPlaces.iterator().next();
            logger.debug("Found stop place to move quays {} from {}", quayIds, sourceStopPlace);

            Instant saveDateTime = Instant.now();
            SourceQuayModificationResult sourceQuayResults = modifySourceQuays(sourceStopPlace, fromVersionComment, quayIds, moveQuayFromDate, saveDateTime);
            StopPlace response = addQuaysToDestinationStop(destinationStopPlaceId, sourceQuayResults.quaysToAdd, sourceQuayResults.quaysToMove, toVersionComment, moveQuayFromDate, saveDateTime);

            logger.info("Moved quays: {} from stop {} to {}", quayIds, sourceStopPlace.getNetexId(), response.getNetexId());
            return response;
        });
    }

    private SourceQuayModificationResult modifySourceQuays(StopPlace sourceStopPlace, String fromVersionComment, List<String> quayIds, LocalDate moveDate, Instant saveDateTime) {
        CopiedEntity<StopPlace> source = stopPlaceCopyHelper.createCopies(sourceStopPlace);
        StopPlace stopPlaceToModifyQuays = source.getCopiedEntity();

        Set<Quay> quaysToAdd = new HashSet<>();
        Set<Quay> quaysToMove = new HashSet<>();

        // Modify the validity of the given quays to end on the given moveDate
        for (Quay quay : stopPlaceToModifyQuays.getQuays()) {
            if (quayIds.contains(quay.getNetexId())) {
                validateQuayIsValidOnDate(quay, moveDate);

                // If quay's validityStart is not null and is on move date, move the quay instead of copying
                if (quayIsMovable(quay, moveDate)) {
                    quaysToMove.add(quay);
                } else {
                    // Quay is not movable, add validity end date and create a copy
                    Quay copiedQuay = versionCreator.createCopy(quay, Quay.class);
                    copiedQuay.resetNetexIds();
                    quaysToAdd.add(copiedQuay);

                    quay.getKeyValues().put("validityEnd", new Value(moveDate.toString()));
                }
            }
        }
        stopPlaceToModifyQuays.setVersionComment(fromVersionComment);

        if (!quaysToMove.isEmpty()) {
            // If moving a quay, remove it from the stop place entirely
            stopPlaceToModifyQuays.getQuays().removeIf(quay -> quaysToMove.contains(quay));
        }

        logger.debug("Modified validity of quays {} and removed quays {} from {}", quaysToAdd.stream().map(q -> q.getNetexId()).collect(toSet()), quaysToMove.stream().map(q -> q.getNetexId()).collect(toSet()), stopPlaceToModifyQuays);
        save(source, saveDateTime);

        return new SourceQuayModificationResult(quaysToAdd, quaysToMove);
    }

    public record SourceQuayModificationResult(Set<Quay> quaysToAdd, Set<Quay> quaysToMove) {}

    private StopPlace addQuaysToDestinationStop(String destinationStopPlaceId, Set<Quay> quaysToAdd, Set<Quay> quaysToMove, String toVersionComment, LocalDate moveDate, Instant saveDateTime) {

        StopPlace destinationStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(destinationStopPlaceId);

        if (destinationStopPlace == null) {
            throw new IllegalArgumentException("Cannot resolve destination stop place by ID " + destinationStopPlaceId);
        }

        CopiedEntity<StopPlace> destination = stopPlaceCopyHelper.createCopies(destinationStopPlace);
        StopPlace stopPlaceToAddQuaysTo = destination.getCopiedEntity();
        Value stopPlaceValidityEnd = stopPlaceToAddQuaysTo.getKeyValues().get("validityEnd");

        // Add correct validity dates to the given quays, so that they are valid starting from the move date
        for (Quay quay : quaysToAdd) {
            quay.getKeyValues().put("validityStart", new Value(moveDate.toString()));

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

        // Add correct validity end dates for moved quays
        if (stopPlaceValidityEnd != null) {
            for (Quay quay : quaysToMove) {
                Value quayValidityEnd = quay.getKeyValues().get("validityEnd");

                // If the quay has no validity end or its validity end is after the stop place's validity end,
                // set the quay's validity end to be the same as the stop place's.
                if (quayValidityEnd == null || isDateAfter(quayValidityEnd, stopPlaceValidityEnd)) {
                    quay.getKeyValues().put("validityEnd", stopPlaceValidityEnd);
                }
            }
        }

        stopPlaceToAddQuaysTo.getQuays().addAll(quaysToAdd);
        stopPlaceToAddQuaysTo.getQuays().addAll(quaysToMove);
        stopPlaceCentroidComputer.computeCentroidForStopPlace(stopPlaceToAddQuaysTo);
        stopPlaceToAddQuaysTo.setVersionComment(toVersionComment);

        logger.debug("Saved stop place with copied quays {} and moved quays {} {}", quaysToAdd, quaysToMove, destinationStopPlace);
        return save(destination, saveDateTime);
    }

    private void validateQuayIsValidOnDate(Quay quay, LocalDate date) throws IllegalArgumentException {
        Optional<LocalDate> validityStartOpt = getQuayDate(quay, "validityStart");
        Optional<LocalDate> validityEndOpt = getQuayDate(quay, "validityEnd");

        // Check if the move date is before the quay's validity start date
        if (validityStartOpt.isPresent() && date.isBefore(validityStartOpt.get())) {
            throw new IllegalArgumentException("Quay " + quay.getNetexId() + " is not yet valid on the selected date.");
        }

        // Check if the move date is after the quay's validity end date
        if (validityEndOpt.isPresent() && !date.isBefore(validityEndOpt.get())) {
            throw new IllegalArgumentException("Quay " + quay.getNetexId() + " has already expired on the selected date.");
        }
    }

    /**
     * Quay is movable if move date is the same as validity start date
     */
    private Boolean quayIsMovable(Quay quay, LocalDate moveDate) {
        Optional<LocalDate> validityStartOpt = getQuayDate(quay, "validityStart");
        return validityStartOpt.isPresent() && validityStartOpt.get().equals(moveDate);
    }

    private Optional<LocalDate> getQuayDate(Quay quay, String keyName) {
        return Optional.ofNullable(quay.getKeyValues().get(keyName))
                .flatMap(value -> value.getItems().stream().findFirst())
                .map(LocalDate::parse);
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
     * @param saveDateTime
     * @return
     */
    private StopPlace save(CopiedEntity<StopPlace> copiedEntity, Instant saveDateTime) {
        if (copiedEntity.hasParent()) {
            return stopPlaceVersionedSaverService.saveNewVersion(copiedEntity.getExistingParent(), copiedEntity.getCopiedParent(), saveDateTime);
        } else
            return stopPlaceVersionedSaverService.saveNewVersion(copiedEntity.getExistingEntity(), copiedEntity.getCopiedEntity(), saveDateTime);
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

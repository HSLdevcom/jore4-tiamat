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

import org.junit.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.Value;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class StopPlaceQuayMoverTest extends TiamatIntegrationTest {

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private StopPlaceQuayMover stopPlaceQuayMover;

    @Test
    public void copyQuayToExistingStop() {

        StopPlace fromStopPlace = new StopPlace();

        Quay quayToMove = new Quay(new EmbeddableMultilingualString("quay to be moved"));
        quayToMove.setVersion(1L);
        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace.setVersion(1L);
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace(new EmbeddableMultilingualString("Destination stop place"));
        destinationStopPlace.setVersion(1L);
        stopPlaceRepository.save(destinationStopPlace);

        LocalDate tomorrow = getTomorrow();
        String tomorrowStr = tomorrow.toString();
        String todayStr = LocalDate.now().toString();

        StopPlace result = stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), tomorrow,null, null);

        assertThat(result.getNetexId()).isEqualTo(destinationStopPlace.getNetexId());
        assertThat(result.getQuays()).hasSize(1);
        assertThat(result.getName()).isEqualTo(destinationStopPlace.getName());
        assertThat(result.getVersion()).isEqualTo(2L);

        Quay destinationQuay = result.getQuays().iterator().next();
        assertThat(destinationQuay.getName()).isNotNull();
        assertThat(destinationQuay.getName().getValue()).isEqualTo(quayToMove.getName().getValue());
        assertThat(destinationQuay.getVersion()).isEqualTo(1L);
        assertThat(destinationQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(tomorrowStr);

        fromStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(fromStopPlace.getNetexId());
        assertThat(fromStopPlace.getQuays()).hasSize(1);
        assertThat(fromStopPlace.getVersion()).as("new version of source stop place with modified validity quays").isEqualTo(2L);

        Quay sourceQuay = fromStopPlace.getQuays().iterator().next();
        assertThat(sourceQuay.getName()).isNotNull();
        assertThat(sourceQuay.getName().getValue()).isEqualTo(quayToMove.getName().getValue());
        assertThat(sourceQuay.getVersion()).isEqualTo(2L);
        assertThat(sourceQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(todayStr);

        destinationStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(destinationStopPlace.getNetexId());
        assertThat(destinationStopPlace).isEqualTo(result);
    }

    @Test
    public void copyQuayBetweenChildStops() {

        // Arrange
        // Quays
        Quay quayToMove = new Quay();
        quayToMove.setPublicCode("1");

        Quay existingQuay = new Quay();
        existingQuay.setPublicCode("2");

        // Stop places
        StopPlace sourceStopPlace = new StopPlace();
        sourceStopPlace.getQuays().add(quayToMove);

        StopPlace destinationStopPlace = new StopPlace();
        destinationStopPlace.getQuays().add(existingQuay);

        // Parent stop places
        StopPlace parentSourceStopPlace = new StopPlace();
        parentSourceStopPlace.getChildren().add(sourceStopPlace);
        parentSourceStopPlace = stopPlaceVersionedSaverService.saveNewVersion(parentSourceStopPlace);

        StopPlace parentDestinationStopPlace = new StopPlace();
        parentDestinationStopPlace.getChildren().add(destinationStopPlace);
        parentDestinationStopPlace = stopPlaceVersionedSaverService.saveNewVersion(parentDestinationStopPlace);

        LocalDate tomorrow = getTomorrow();
        String tomorrowStr = tomorrow.toString();
        String todayStr = LocalDate.now().toString();
        String fromVersionComment = "from comment";
        String toVersionComment = "to comment";

        // Act
        destinationStopPlace = stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), tomorrow, fromVersionComment, toVersionComment);

        // Assert source
        parentSourceStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(parentSourceStopPlace.getNetexId());
        assertThat(parentSourceStopPlace.getChildren()).hasSize(1);

        sourceStopPlace = parentSourceStopPlace.getChildren().iterator().next();
        assertThat(sourceStopPlace.getQuays()).hasSize(1);
        assertThat(sourceStopPlace.getVersion()).isEqualTo(2L);
        assertThat(sourceStopPlace.getVersionComment()).isEqualTo(fromVersionComment);

        Quay sourceQuay = sourceStopPlace.getQuays().iterator().next();
        assertThat(sourceQuay.getVersion()).isEqualTo(2L);
        assertThat(sourceQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(todayStr);

        // Assert destination
        StopPlace actualParentDestinationStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(destinationStopPlace.getParentSiteRef().getRef());
        
        assertThat(actualParentDestinationStopPlace).isNotNull();
        assertThat(actualParentDestinationStopPlace.getNetexId()).isEqualTo(parentDestinationStopPlace.getNetexId());
        assertThat(actualParentDestinationStopPlace.getChildren()).hasSize(1);

        StopPlace actualDestinationStopPlace = actualParentDestinationStopPlace.getChildren().iterator().next();
        assertThat(actualDestinationStopPlace.getNetexId()).isEqualTo(destinationStopPlace.getNetexId());
        assertThat(actualDestinationStopPlace.getQuays()).hasSize(2);
        assertThat(actualDestinationStopPlace.getQuays()).extracting(Quay::getNetexId).doesNotContain(quayToMove.getNetexId());
        assertThat(actualDestinationStopPlace.getVersion()).isEqualTo(2L);
        assertThat(actualDestinationStopPlace.getVersionComment()).isEqualTo(toVersionComment);

        Quay destinationQuay = actualDestinationStopPlace
                .getQuays()
                .stream()
                .filter(quay -> !quay.getNetexId().equals(existingQuay.getNetexId())) // Skip the existingQuay
                .findFirst()
                .orElse(null);
        assertThat(destinationQuay).isNotNull();
        assertThat(destinationQuay.getVersion()).isEqualTo(1L);
        assertThat(destinationQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(tomorrowStr);
    }

    @Test
    public void moveQuayToExistingStop() {

        // Arrange
        LocalDate tomorrow = getTomorrow();
        String tomorrowStr = tomorrow.toString();

        Quay quayToMove = new Quay(new EmbeddableMultilingualString("quay to be moved"));
        quayToMove.setVersion(1L);
        quayToMove.getKeyValues().put("validityStart", new Value(tomorrowStr));

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace.setVersion(1L);
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace(new EmbeddableMultilingualString("Destination stop place"));
        destinationStopPlace.setVersion(1L);
        stopPlaceRepository.save(destinationStopPlace);

        // Act
        StopPlace result = stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), tomorrow,null, null);

        // Assert
        assertThat(result.getNetexId()).isEqualTo(destinationStopPlace.getNetexId());
        assertThat(result.getQuays()).hasSize(1);
        assertThat(result.getVersion()).isEqualTo(2L);

        Quay quay = result.getQuays().iterator().next();
        assertThat(quay.getName()).isNotNull();
        assertThat(quay.getName().getValue()).isEqualTo(quayToMove.getName().getValue());
        assertThat(quay.getVersion()).isEqualTo(2L);
        assertThat(quay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(tomorrowStr);

        fromStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(fromStopPlace.getNetexId());
        assertThat(fromStopPlace.getQuays()).hasSize(0);
        assertThat(fromStopPlace.getVersion()).isEqualTo(2L);

        destinationStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(destinationStopPlace.getNetexId());
        assertThat(destinationStopPlace).isEqualTo(result);
    }

    @Test
    public void copyQuayToStopPlaceAndBack() {

        /*
        First copy a quay to another stop place, then copy it back and make sure that the original
        stop place now has two versions of the quay and the another one has one
        */

        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate firstMoveDate = today.plusDays(3);
        LocalDate secondMoveDate = firstMoveDate.plusDays(3);

        String quayPublicCode = "00001";
        String quayPriority = "10";

        Quay quayToMove = new Quay();
        quayToMove.setPublicCode(quayPublicCode);
        quayToMove.setVersion(1L);
        quayToMove.getKeyValues().put("validityStart", new Value(today.toString()));
        quayToMove.getKeyValues().put("priority", new Value(quayPriority));
        quayToMove.getKeyValues().put("imported-id", new Value(getImportedId(quayPublicCode, today, quayPriority)));

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace.setVersion(1L);
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace();
        destinationStopPlace.setVersion(1L);
        stopPlaceRepository.save(destinationStopPlace);

        // Act
        destinationStopPlace = stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), firstMoveDate,null, null);

        Quay movedQuay = destinationStopPlace.getQuays().iterator().next();
        fromStopPlace = stopPlaceQuayMover.moveQuays(Arrays.asList(movedQuay.getNetexId()), fromStopPlace.getNetexId(), secondMoveDate,null, null);

        // Assert
        assertThat(fromStopPlace.getNetexId()).isEqualTo(fromStopPlace.getNetexId());
        assertThat(fromStopPlace.getQuays()).hasSize(2);
        assertThat(fromStopPlace.getVersion()).isEqualTo(3L);

        Quay originalQuay = fromStopPlace.getQuays().stream()
                .filter(q -> q.getNetexId().equals(quayToMove.getNetexId()))
                .findFirst()
                .orElse(null);
        assertThat(originalQuay).isNotNull();
        assertThat(originalQuay.getVersion()).isEqualTo(3L);
        assertThat(originalQuay.getPublicCode()).isEqualTo(quayPublicCode);
        assertThat(originalQuay.getKeyValues().get("priority").getItems().stream().findFirst().get()).isEqualTo(quayPriority);
        assertThat(originalQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(today.toString());
        assertThat(originalQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(firstMoveDate.minusDays(1).toString());
        assertThat(originalQuay.getKeyValues().get("imported-id").getItems().stream().findFirst().get()).isEqualTo(getImportedId(quayPublicCode, today, quayPriority));

        Quay secondMovedQuay = fromStopPlace.getQuays().stream()
                .filter(q -> !q.getNetexId().equals(quayToMove.getNetexId()))
                .findFirst()
                .orElse(null);
        assertThat(secondMovedQuay).isNotNull();
        assertThat(secondMovedQuay.getVersion()).isEqualTo(1L);
        assertThat(secondMovedQuay.getPublicCode()).isEqualTo(quayPublicCode);
        assertThat(secondMovedQuay.getKeyValues().get("priority").getItems().stream().findFirst().get()).isEqualTo(quayPriority);
        assertThat(secondMovedQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(secondMoveDate.toString());
        assertThat(secondMovedQuay.getKeyValues().get("validityEnd")).isEqualTo(null);
        assertThat(secondMovedQuay.getKeyValues().get("imported-id").getItems().stream().findFirst().get()).isEqualTo(getImportedId(quayPublicCode, secondMoveDate, quayPriority));

        destinationStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(destinationStopPlace.getNetexId());
        assertThat(destinationStopPlace.getQuays()).hasSize(1);
        assertThat(destinationStopPlace.getVersion()).isEqualTo(3L);

        Quay firstMovedQuay = destinationStopPlace.getQuays().iterator().next();
        assertThat(firstMovedQuay).isNotNull();
        assertThat(firstMovedQuay.getVersion()).isEqualTo(2L);
        assertThat(firstMovedQuay.getPublicCode()).isEqualTo(quayPublicCode);
        assertThat(firstMovedQuay.getKeyValues().get("priority").getItems().stream().findFirst().get()).isEqualTo(quayPriority);
        assertThat(firstMovedQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(firstMoveDate.toString());
        assertThat(firstMovedQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(secondMoveDate.minusDays(1).toString());
        assertThat(firstMovedQuay.getKeyValues().get("imported-id").getItems().stream().findFirst().get()).isEqualTo(getImportedId(quayPublicCode, firstMoveDate, quayPriority));
    }

    @Test
    public void moveQuayToStopPlaceAndBack() {

        /*
        First copy a quay to another stop place, then move it back and make sure that the original
        stop place now has two versions of the quay and the another one has zero
        */

        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate firstMoveDate = today.plusDays(3);

        String quayPublicCode = "00001";
        String quayPriority = "10";

        Quay quayToMove = new Quay();
        quayToMove.setPublicCode(quayPublicCode);
        quayToMove.setVersion(1L);
        quayToMove.getKeyValues().put("validityStart", new Value(today.toString()));
        quayToMove.getKeyValues().put("priority", new Value(quayPriority));
        quayToMove.getKeyValues().put("imported-id", new Value(getImportedId(quayPublicCode, today, quayPriority)));

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace.setVersion(1L);
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace();
        destinationStopPlace.setVersion(1L);
        stopPlaceRepository.save(destinationStopPlace);

        // Act => Copy the quay first to destination and then move it back
        destinationStopPlace = stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), firstMoveDate,null, null);

        Quay movedQuay = destinationStopPlace.getQuays().iterator().next();
        fromStopPlace = stopPlaceQuayMover.moveQuays(Arrays.asList(movedQuay.getNetexId()), fromStopPlace.getNetexId(), firstMoveDate,null, null);

        // Assert
        assertThat(fromStopPlace.getNetexId()).isEqualTo(fromStopPlace.getNetexId());
        assertThat(fromStopPlace.getQuays()).hasSize(2);
        assertThat(fromStopPlace.getVersion()).isEqualTo(3L);

        Quay originalQuay = fromStopPlace.getQuays().stream()
                .filter(q -> q.getNetexId().equals(quayToMove.getNetexId()))
                .findFirst()
                .orElse(null);
        assertThat(originalQuay).isNotNull();
        assertThat(originalQuay.getVersion()).isEqualTo(3L);
        assertThat(originalQuay.getPublicCode()).isEqualTo(quayPublicCode);
        assertThat(originalQuay.getKeyValues().get("priority").getItems().stream().findFirst().get()).isEqualTo(quayPriority);
        assertThat(originalQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(today.toString());
        assertThat(originalQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(firstMoveDate.minusDays(1).toString());
        assertThat(originalQuay.getKeyValues().get("imported-id").getItems().stream().findFirst().get()).isEqualTo(getImportedId(quayPublicCode, today, quayPriority));

        Quay secondMovedQuay = fromStopPlace.getQuays().stream()
                .filter(q -> !q.getNetexId().equals(quayToMove.getNetexId()))
                .findFirst()
                .orElse(null);
        assertThat(secondMovedQuay).isNotNull();
        assertThat(secondMovedQuay.getVersion()).isEqualTo(2L);
        assertThat(secondMovedQuay.getPublicCode()).isEqualTo(quayPublicCode);
        assertThat(secondMovedQuay.getKeyValues().get("priority").getItems().stream().findFirst().get()).isEqualTo(quayPriority);
        assertThat(secondMovedQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(firstMoveDate.toString());
        assertThat(secondMovedQuay.getKeyValues().get("validityEnd")).isEqualTo(null);
        assertThat(secondMovedQuay.getKeyValues().get("imported-id").getItems().stream().findFirst().get()).isEqualTo(getImportedId(quayPublicCode, firstMoveDate, quayPriority));

        destinationStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(destinationStopPlace.getNetexId());
        assertThat(destinationStopPlace.getQuays()).hasSize(0);
        assertThat(destinationStopPlace.getVersion()).isEqualTo(3L);
    }

    @Test
    public void copyQuayToStopPlaceWithValidityEndDate() {

        /*
        Copy a quay to a stop place with validity end set and make sure that the validity end
        is correctly set for the copied quay
        */

        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate moveDate = today.plusDays(3);
        LocalDate validityEndDate = today.plusDays(10);

        Quay quayToMove = new Quay();
        quayToMove.setVersion(1L);
        quayToMove.getKeyValues().put("validityStart", new Value(today.toString()));

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace.setVersion(1L);
        fromStopPlace.getKeyValues().put("validityStart", new Value(today.toString()));
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace();
        destinationStopPlace.setVersion(1L);
        destinationStopPlace.getKeyValues().put("validityStart", new Value(today.toString()));
        destinationStopPlace.getKeyValues().put("validityEnd", new Value(validityEndDate.toString()));
        stopPlaceRepository.save(destinationStopPlace);

        // Act
        destinationStopPlace = stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), moveDate,null, null);

        // Assert
        fromStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(fromStopPlace.getNetexId());
        assertThat(fromStopPlace.getQuays()).hasSize(1);
        assertThat(fromStopPlace.getVersion()).isEqualTo(2L);

        Quay sourceQuay = fromStopPlace.getQuays().iterator().next();
        assertThat(sourceQuay).isNotNull();
        assertThat(sourceQuay.getVersion()).isEqualTo(2L);
        assertThat(sourceQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(today.toString());
        assertThat(sourceQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(moveDate.minusDays(1).toString());

        assertThat(destinationStopPlace.getNetexId()).isEqualTo(destinationStopPlace.getNetexId());
        assertThat(destinationStopPlace.getQuays()).hasSize(1);
        assertThat(destinationStopPlace.getVersion()).isEqualTo(2L);

        Quay movedQuay = destinationStopPlace.getQuays().iterator().next();
        assertThat(movedQuay).isNotNull();
        assertThat(movedQuay.getVersion()).isEqualTo(1L);
        assertThat(movedQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(moveDate.toString());
        assertThat(movedQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(validityEndDate.toString());
    }

    @Test
    public void moveQuayToStopPlaceWithValidityEndDate() {

        /*
        Move a quay to a stop place with validity end set and make sure that the validity end
        is correctly set for the moved quay
        */

        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate moveDate = today.plusDays(3);
        LocalDate validityEndDate = today.plusDays(10);

        Quay quayToMove = new Quay();
        quayToMove.setVersion(1L);
        quayToMove.getKeyValues().put("validityStart", new Value(moveDate.toString()));

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace.setVersion(1L);
        fromStopPlace.getKeyValues().put("validityStart", new Value(today.toString()));
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace();
        destinationStopPlace.setVersion(1L);
        destinationStopPlace.getKeyValues().put("validityStart", new Value(today.toString()));
        destinationStopPlace.getKeyValues().put("validityEnd", new Value(validityEndDate.toString()));
        stopPlaceRepository.save(destinationStopPlace);

        // Act
        destinationStopPlace = stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), moveDate,null, null);

        // Assert
        fromStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(fromStopPlace.getNetexId());
        assertThat(fromStopPlace.getQuays()).hasSize(0);
        assertThat(fromStopPlace.getVersion()).isEqualTo(2L);

        assertThat(destinationStopPlace.getNetexId()).isEqualTo(destinationStopPlace.getNetexId());
        assertThat(destinationStopPlace.getQuays()).hasSize(1);
        assertThat(destinationStopPlace.getVersion()).isEqualTo(2L);

        Quay movedQuay = destinationStopPlace.getQuays().iterator().next();
        assertThat(movedQuay).isNotNull();
        assertThat(movedQuay.getVersion()).isEqualTo(2L);
        assertThat(movedQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(moveDate.toString());
        assertThat(movedQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(validityEndDate.toString());
    }

    @Test
    public void copyQuayWithValidityToStopPlace() {

        /*
        Copy a quay that has validity end set to a stop place and make sure that the validity end is not changed.
        */

        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate moveDate = today.plusDays(3);
        LocalDate validityEndDate = today.plusDays(10);

        Quay quayToMove = new Quay();
        quayToMove.setVersion(1L);
        quayToMove.getKeyValues().put("validityStart", new Value(today.toString()));
        quayToMove.getKeyValues().put("validityEnd", new Value(validityEndDate.toString()));

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace.setVersion(1L);
        fromStopPlace.getKeyValues().put("validityStart", new Value(today.toString()));
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace();
        destinationStopPlace.setVersion(1L);
        destinationStopPlace.getKeyValues().put("validityStart", new Value(today.toString()));
        stopPlaceRepository.save(destinationStopPlace);

        // Act
        destinationStopPlace = stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), moveDate,null, null);

        // Assert
        fromStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(fromStopPlace.getNetexId());
        assertThat(fromStopPlace.getQuays()).hasSize(1);
        assertThat(fromStopPlace.getVersion()).isEqualTo(2L);

        Quay sourceQuay = fromStopPlace.getQuays().iterator().next();
        assertThat(sourceQuay).isNotNull();
        assertThat(sourceQuay.getVersion()).isEqualTo(2L);
        assertThat(sourceQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(today.toString());
        assertThat(sourceQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(moveDate.minusDays(1).toString());

        assertThat(destinationStopPlace.getNetexId()).isEqualTo(destinationStopPlace.getNetexId());
        assertThat(destinationStopPlace.getQuays()).hasSize(1);
        assertThat(destinationStopPlace.getVersion()).isEqualTo(2L);

        Quay movedQuay = destinationStopPlace.getQuays().iterator().next();
        assertThat(movedQuay).isNotNull();
        assertThat(movedQuay.getVersion()).isEqualTo(1L);
        assertThat(movedQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(moveDate.toString());
        assertThat(movedQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(validityEndDate.toString());
    }

    @Test
    public void moveQuayWithValidityToStopPlace() {

        /*
        Move a quay that has validity end set to a stop place and make sure that the validity end is not changed.
        */

        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate moveDate = today.plusDays(3);
        LocalDate validityEndDate = today.plusDays(10);

        Quay quayToMove = new Quay();
        quayToMove.setVersion(1L);
        quayToMove.getKeyValues().put("validityStart", new Value(moveDate.toString()));
        quayToMove.getKeyValues().put("validityEnd", new Value(validityEndDate.toString()));

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace.setVersion(1L);
        fromStopPlace.getKeyValues().put("validityStart", new Value(today.toString()));
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace();
        destinationStopPlace.setVersion(1L);
        destinationStopPlace.getKeyValues().put("validityStart", new Value(today.toString()));
        stopPlaceRepository.save(destinationStopPlace);

        // Act
        destinationStopPlace = stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), moveDate,null, null);

        // Assert
        fromStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(fromStopPlace.getNetexId());
        assertThat(fromStopPlace.getQuays()).hasSize(0);
        assertThat(fromStopPlace.getVersion()).isEqualTo(2L);

        assertThat(destinationStopPlace.getNetexId()).isEqualTo(destinationStopPlace.getNetexId());
        assertThat(destinationStopPlace.getQuays()).hasSize(1);
        assertThat(destinationStopPlace.getVersion()).isEqualTo(2L);

        Quay movedQuay = destinationStopPlace.getQuays().iterator().next();
        assertThat(movedQuay).isNotNull();
        assertThat(movedQuay.getVersion()).isEqualTo(2L);
        assertThat(movedQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(moveDate.toString());
        assertThat(movedQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(validityEndDate.toString());
    }

    @Test
    public void copyQuayWithValidityToStopPlaceWithValidity() {

        /*
        Copy a quay that has validity end set to a stop place that has a tighter validity and make
        sure that the validity end is changed to match that of the stop place.
        */

        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate moveDate = today.plusDays(3);
        LocalDate quayValidityEndDate = today.plusDays(10);
        LocalDate stopPlaceValidityEndDate = today.plusDays(8);

        Quay quayToMove = new Quay();
        quayToMove.setVersion(1L);
        quayToMove.getKeyValues().put("validityStart", new Value(today.toString()));
        quayToMove.getKeyValues().put("validityEnd", new Value(quayValidityEndDate.toString()));

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace.setVersion(1L);
        fromStopPlace.getKeyValues().put("validityStart", new Value(today.toString()));
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace();
        destinationStopPlace.setVersion(1L);
        destinationStopPlace.getKeyValues().put("validityStart", new Value(today.toString()));
        destinationStopPlace.getKeyValues().put("validityEnd", new Value(stopPlaceValidityEndDate.toString()));
        stopPlaceRepository.save(destinationStopPlace);

        // Act
        destinationStopPlace = stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), moveDate,null, null);

        // Assert
        fromStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(fromStopPlace.getNetexId());
        assertThat(fromStopPlace.getQuays()).hasSize(1);
        assertThat(fromStopPlace.getVersion()).isEqualTo(2L);

        Quay sourceQuay = fromStopPlace.getQuays().iterator().next();
        assertThat(sourceQuay).isNotNull();
        assertThat(sourceQuay.getVersion()).isEqualTo(2L);
        assertThat(sourceQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(today.toString());
        assertThat(sourceQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(moveDate.minusDays(1).toString());

        assertThat(destinationStopPlace.getNetexId()).isEqualTo(destinationStopPlace.getNetexId());
        assertThat(destinationStopPlace.getQuays()).hasSize(1);
        assertThat(destinationStopPlace.getVersion()).isEqualTo(2L);
        assertThat(destinationStopPlace.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(today.toString());
        assertThat(destinationStopPlace.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(stopPlaceValidityEndDate.toString());

        Quay movedQuay = destinationStopPlace.getQuays().iterator().next();
        assertThat(movedQuay).isNotNull();
        assertThat(movedQuay.getVersion()).isEqualTo(1L);
        assertThat(movedQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(moveDate.toString());
        assertThat(movedQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(stopPlaceValidityEndDate.toString());
    }

    @Test
    public void moveQuayWithValidityToStopPlaceWithValidity() {

        /*
        Move a quay that has validity end set to a stop place that has a tighter validity and make
        sure that the validity end is changed to match that of the stop place.
        */

        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate moveDate = today.plusDays(3);
        LocalDate quayValidityEndDate = today.plusDays(10);
        LocalDate stopPlaceValidityEndDate = today.plusDays(8);

        Quay quayToMove = new Quay();
        quayToMove.setVersion(1L);
        quayToMove.getKeyValues().put("validityStart", new Value(moveDate.toString()));
        quayToMove.getKeyValues().put("validityEnd", new Value(quayValidityEndDate.toString()));

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace.setVersion(1L);
        fromStopPlace.getKeyValues().put("validityStart", new Value(today.toString()));
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace();
        destinationStopPlace.setVersion(1L);
        destinationStopPlace.getKeyValues().put("validityStart", new Value(today.toString()));
        destinationStopPlace.getKeyValues().put("validityEnd", new Value(stopPlaceValidityEndDate.toString()));
        stopPlaceRepository.save(destinationStopPlace);

        // Act
        destinationStopPlace = stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), moveDate,null, null);

        // Assert
        fromStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(fromStopPlace.getNetexId());
        assertThat(fromStopPlace.getQuays()).hasSize(0);
        assertThat(fromStopPlace.getVersion()).isEqualTo(2L);

        assertThat(destinationStopPlace.getNetexId()).isEqualTo(destinationStopPlace.getNetexId());
        assertThat(destinationStopPlace.getQuays()).hasSize(1);
        assertThat(destinationStopPlace.getVersion()).isEqualTo(2L);
        assertThat(destinationStopPlace.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(today.toString());
        assertThat(destinationStopPlace.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(stopPlaceValidityEndDate.toString());

        Quay movedQuay = destinationStopPlace.getQuays().iterator().next();
        assertThat(movedQuay).isNotNull();
        assertThat(movedQuay.getVersion()).isEqualTo(2L);
        assertThat(movedQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(moveDate.toString());
        assertThat(movedQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(stopPlaceValidityEndDate.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAllowMoveIntoStopPlaceThatIsNotValidOnMoveDate() {

        /*
        Test that an IllegalArgumentException is thrown when trying to move a quay to a
        stop place that is not valid on the move date.
        */

        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate moveDate = today.plusDays(3);
        LocalDate stopPlaceValidityStart = today.plusDays(5); // Stop place validity starts after the move date

        Quay quayToMove = new Quay();
        quayToMove.getKeyValues().put("validityStart", new Value(today.toString()));

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace();
        destinationStopPlace.getKeyValues().put("validityStart", new Value(stopPlaceValidityStart.toString()));
        stopPlaceRepository.save(destinationStopPlace);

        // Act
        stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), moveDate, null,null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAllowMoveFromStopPlaceThatIsNotValidOnMoveDate() {

        /*
        Test that an IllegalArgumentException is thrown when trying to move a quay from a
        stop place that is not valid on the move date.
        */

        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate moveDate = today.plusDays(5);
        LocalDate stopPlaceValidityEnd = today.plusDays(3); // Stop place validity ends before move date

        Quay quayToMove = new Quay();
        quayToMove.getKeyValues().put("validityStart", new Value(today.toString()));

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace.getKeyValues().put("validityEnd", new Value(stopPlaceValidityEnd.toString()));
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace();
        stopPlaceRepository.save(destinationStopPlace);

        // Act
        stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), moveDate, null,null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAllowInvalidDateInQuayKeyValues() {

        /*
        Test that an IllegalArgumentException is thrown when trying to move a quay with an invalid date in its key values.
        This is to ensure that the system does not allow invalid dates to be processed.
        */

        // Arrange
        Quay quayToMove = new Quay();
        quayToMove.getKeyValues().put("validityStart", new Value("invalid-date"));

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace();
        stopPlaceRepository.save(destinationStopPlace);

        // Act
        stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), LocalDate.now(), null,null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAllowInvalidDateInSourceStopPlaceKeyValues() {

        /*
        Test that an IllegalArgumentException is thrown when trying to move a quay from a stop place that has an invalid date in its key values.
        This is to ensure that the system does not allow invalid dates to be processed.
        */

        // Arrange
        Quay quayToMove = new Quay();

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace.getKeyValues().put("validityStart", new Value("invalid-date"));
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace();
        stopPlaceRepository.save(destinationStopPlace);

        // Act
        stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), LocalDate.now(), null,null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAllowInvalidDateInDestinationStopPlaceKeyValues() {

        /*
        Test that an IllegalArgumentException is thrown when trying to move a quay to a stop place that has an invalid date in its key values.
        This is to ensure that the system does not allow invalid dates to be processed.
        */

        // Arrange
        Quay quayToMove = new Quay();

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace();
        destinationStopPlace.getKeyValues().put("validityStart", new Value("invalid-date"));
        stopPlaceRepository.save(destinationStopPlace);

        // Act
        stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), LocalDate.now(), null,null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAllowMoveQuayBeforeValidityStart() {

        /*
        Confirm that a quay cannot be moved to a stop place before the quay's validity start date.
        This should throw an IllegalArgumentException.
        */

        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate moveDate = today.plusDays(3);
        LocalDate validityStartDate = today.plusDays(5); // Validity start is after the move date

        Quay quayToMove = new Quay();
        quayToMove.getKeyValues().put("validityStart", new Value(validityStartDate.toString()));

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace();
        stopPlaceRepository.save(destinationStopPlace);

        // Act
        stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), moveDate, null,null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAllowMoveQuayAfterValidityEnd() {

        /*
        Confirm that a quay cannot be moved to a stop place after the quay's validity end date.
        This should throw an IllegalArgumentException.
        */

        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate validityEndDate = today.plusDays(1); // Validity end is before the move date
        LocalDate moveDate = today.plusDays(3);

        Quay quayToMove = new Quay();
        quayToMove.getKeyValues().put("validityStart", new Value(today.toString()));
        quayToMove.getKeyValues().put("validityEnd", new Value(validityEndDate.toString()));

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        stopPlaceRepository.save(fromStopPlace);

        StopPlace destinationStopPlace = new StopPlace();
        stopPlaceRepository.save(destinationStopPlace);

        // Act
        stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), moveDate, null,null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAllowMoveQuayToNewStop() {

        Quay quayToMove = new Quay();

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        stopPlaceRepository.save(fromStopPlace);

        stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), null, getTomorrow(), null,null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAcceptInvalidQuayId() {

        StopPlace destinationStopPlace = new StopPlace();
        stopPlaceRepository.save(destinationStopPlace);

        stopPlaceQuayMover.moveQuays(Arrays.asList("NSR:Quay:99999999"), destinationStopPlace.getNetexId(), LocalDate.now(), null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAcceptInvalidStopPlaceDestinationId() {

        Quay quayToMove = new Quay();

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.getQuays().add(quayToMove);
        stopPlaceRepository.save(fromStopPlace);

        stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), "NSR:StopPlace:91919191", LocalDate.now(), null, null);
    }

    private LocalDate getTomorrow() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        return tomorrow;
    }

    private String getImportedId(String publicCode, LocalDate date, String priority) {
        return String.format("%s-%s-%s", publicCode, date.toString(), priority);
    }
}
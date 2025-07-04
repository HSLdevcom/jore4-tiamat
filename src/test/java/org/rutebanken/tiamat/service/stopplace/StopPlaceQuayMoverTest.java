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
    public void moveQuayToExistingStopByCopying() {

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

        StopPlace result = stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), tomorrow,null, null);

        assertThat(result.getNetexId()).isEqualTo(destinationStopPlace.getNetexId());
        assertThat(result.getQuays()).hasSize(1);
        assertThat(result.getVersion()).isEqualTo(2L);

        Quay destinationQuay = result.getQuays().iterator().next();
        assertThat(destinationQuay.getName()).isNotNull();
        assertThat(destinationQuay.getVersion()).isEqualTo(1L);
        assertThat(destinationQuay.getName().getValue()).isEqualTo(quayToMove.getName().getValue());
        assertThat(destinationQuay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(tomorrowStr);

        fromStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(fromStopPlace.getNetexId());
        assertThat(fromStopPlace.getQuays()).hasSize(1);
        assertThat(fromStopPlace.getVersion()).as("new version of source stop place with modified validity quays").isEqualTo(2L);

        Quay sourceQuay = fromStopPlace.getQuays().iterator().next();
        assertThat(sourceQuay.getName()).isNotNull();
        assertThat(sourceQuay.getVersion()).isEqualTo(2L);
        assertThat(sourceQuay.getName().getValue()).isEqualTo(quayToMove.getName().getValue());
        assertThat(sourceQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(tomorrowStr);

        destinationStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(destinationStopPlace.getNetexId());
        assertThat(destinationStopPlace).isEqualTo(result);
    }

    @Test
    public void moveQuayBetweenChildStopsByCopying() {

        // Arrange
        // Quays
        Quay quayToMove = new Quay(new EmbeddableMultilingualString("Quay to move"));
        quayToMove.setPublicCode("1");

        Quay existingQuay = new Quay(new EmbeddableMultilingualString("Quay to not move"));
        existingQuay.setPublicCode("2");

        // Stop places
        StopPlace sourceStopPlace = new StopPlace();
        sourceStopPlace.getQuays().add(quayToMove);

        StopPlace destinationStopPlace = new StopPlace();
        destinationStopPlace.getQuays().add(existingQuay);

        // Parent stop places
        StopPlace parentSourceStopPlace = new StopPlace(new EmbeddableMultilingualString("parent from stop place"));
        parentSourceStopPlace.getChildren().add(sourceStopPlace);
        parentSourceStopPlace = stopPlaceVersionedSaverService.saveNewVersion(parentSourceStopPlace);

        StopPlace parentDestinationStopPlace = new StopPlace(new EmbeddableMultilingualString("destination parent stop place"));
        parentDestinationStopPlace.getChildren().add(destinationStopPlace);
        parentDestinationStopPlace = stopPlaceVersionedSaverService.saveNewVersion(parentDestinationStopPlace);

        LocalDate tomorrow = getTomorrow();
        String tomorrowStr = tomorrow.toString();
        String fromVersionComment = "from comment";
        String toVersionComment = "to comment";

        // Act
        StopPlace actualParentDestinationStopPlace = stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), destinationStopPlace.getNetexId(), tomorrow, fromVersionComment, toVersionComment);

        // Assert source
        parentSourceStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(parentSourceStopPlace.getNetexId());
        assertThat(parentSourceStopPlace.getChildren()).hasSize(1);

        sourceStopPlace = parentSourceStopPlace.getChildren().iterator().next();
        assertThat(sourceStopPlace.getQuays()).hasSize(1);
        assertThat(sourceStopPlace.getVersion()).isEqualTo(2L);
        assertThat(sourceStopPlace.getVersionComment()).isEqualTo(fromVersionComment);

        Quay sourceQuay = sourceStopPlace.getQuays().iterator().next();
        assertThat(sourceQuay.getName()).isNotNull();
        assertThat(sourceQuay.getVersion()).isEqualTo(2L);
        assertThat(sourceQuay.getKeyValues().get("validityEnd").getItems().stream().findFirst().get()).isEqualTo(tomorrowStr);

        // Assert destination
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
    public void moveQuayToExistingStopByMoving() {

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
        assertThat(quay.getVersion()).isEqualTo(2L);
        assertThat(quay.getName().getValue()).isEqualTo(quayToMove.getName().getValue());
        assertThat(quay.getKeyValues().get("validityStart").getItems().stream().findFirst().get()).isEqualTo(tomorrowStr);

        fromStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(fromStopPlace.getNetexId());
        assertThat(fromStopPlace.getQuays()).hasSize(0);
        assertThat(fromStopPlace.getVersion()).isEqualTo(2L);

        destinationStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(destinationStopPlace.getNetexId());
        assertThat(destinationStopPlace).isEqualTo(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAllowMoveQuayToNewStop() {

        StopPlace fromStopPlace = new StopPlace();

        Quay quayToMove = new Quay(new EmbeddableMultilingualString("quay to be moved 2"));
        quayToMove.setVersion(1L);
        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace.setVersion(1L);
        stopPlaceRepository.save(fromStopPlace);

        LocalDate tomorrow = getTomorrow();
        String tomorrowStr = tomorrow.toString();

        StopPlace result = stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), null, tomorrow, null,null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAcceptInvalidQuayId() {
        stopPlaceQuayMover.moveQuays(Arrays.asList("NSR:Quay:99999999"), null, LocalDate.now(), null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAcceptInvalidStopPlaceDestionationId() {
        StopPlace fromStopPlace = new StopPlace();

        Quay quayToMove = new Quay(new EmbeddableMultilingualString("quay to be moved 4"));
        quayToMove.setVersion(1L);
        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace.setVersion(1L);
        stopPlaceRepository.save(fromStopPlace);

        stopPlaceQuayMover.moveQuays(Arrays.asList(quayToMove.getNetexId()), "NSR:StopPlace:91919191", LocalDate.now(), null, null);
    }

    private LocalDate getTomorrow() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        return tomorrow;
    }
}
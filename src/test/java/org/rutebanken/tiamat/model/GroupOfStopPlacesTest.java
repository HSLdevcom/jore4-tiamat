package org.rutebanken.tiamat.model;

import java.time.Instant;
import org.junit.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;


public class GroupOfStopPlacesTest extends TiamatIntegrationTest {


    @Test
    public void addStopPlacesToGroupOfStopPlaces() {

        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString("Stop place 1"));
        stopPlace.setVersion(1L);
        stopPlace = stopPlaceRepository.save(stopPlace);

        StopPlace stopPlace2 = new StopPlace(new EmbeddableMultilingualString("Stop place 2"));
        stopPlace2.setVersion(1L);
        stopPlace2 = stopPlaceRepository.save(stopPlace2);

        Instant validityStart = Instant.now();
        Instant validityEnd = validityStart.plusSeconds(60*60*24*7); // Week of seconds

        String groupName = "group of stop places";
        GroupOfStopPlaces groupOfStopPlaces = new GroupOfStopPlaces(new EmbeddableMultilingualString(groupName));

        groupOfStopPlaces.getMembers().add(new StopPlaceReference(stopPlace.getNetexId()));
        groupOfStopPlaces.getMembers().add(new StopPlaceReference(stopPlace2.getNetexId()));

        groupOfStopPlaces.setValidBetween(new ValidBetween(validityStart, validityEnd));

        groupOfStopPlaces = groupOfStopPlacesRepository.save(groupOfStopPlaces);

        assertThat(groupOfStopPlaces.getName().getValue()).isEqualTo(groupName);

        assertThat(groupOfStopPlaces.getMembers())
                .extracting(StopPlaceReference::getRef)
                .contains(stopPlace.getNetexId(), stopPlace2.getNetexId());

        assertThat(groupOfStopPlaces.getValidBetween()).isEqualTo(new ValidBetween(validityStart, validityEnd));
    }

    @Test
    public void stopPlaceCouldBelongToMultipleGroups() {

        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString("Stop place 1"));
        stopPlace.setVersion(1L);
        stopPlace = stopPlaceRepository.save(stopPlace);

        String groupName = "group of stop places 1";
        GroupOfStopPlaces groupOfStopPlaces = new GroupOfStopPlaces(new EmbeddableMultilingualString(groupName));
        groupOfStopPlaces.getMembers().add(new StopPlaceReference(stopPlace.getNetexId()));
        groupOfStopPlaces = groupOfStopPlacesRepository.save(groupOfStopPlaces);

        String groupName2 = "group of stop places 2";
        GroupOfStopPlaces groupOfStopPlaces2 = new GroupOfStopPlaces(new EmbeddableMultilingualString(groupName2));
        groupOfStopPlaces2.getMembers().add(new StopPlaceReference(stopPlace.getNetexId()));
        groupOfStopPlaces2 = groupOfStopPlacesRepository.save(groupOfStopPlaces2);

        assertThat(groupOfStopPlaces.getMembers())
                .extracting(StopPlaceReference::getRef)
                .contains(stopPlace.getNetexId());
        assertThat(groupOfStopPlaces2.getMembers())
                .extracting(StopPlaceReference::getRef)
                .contains(stopPlace.getNetexId());
    }

}

package org.rutebanken.tiamat.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jvnet.hk2.annotations.Service;
import org.rutebanken.tiamat.model.InfoSpot;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.reference.ReferenceResolver;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.stream.Collectors.toList;

public class InfoSpotStopPlaceResolver {


    private ReferenceResolver referenceResolver;

    public List<StopPlace> resolve(InfoSpot infoSpot) {
        if(infoSpot.getStopPlaces() != null) {

            return infoSpot.getStopPlaces()
                    .stream()
                    .map(ref -> {
                        StopPlace stopPlace = referenceResolver.resolve(ref);
                        return stopPlace;
                    })
                    .filter(Objects::nonNull)
                    .collect(toList());
        }
        return new ArrayList<>();
    }
}

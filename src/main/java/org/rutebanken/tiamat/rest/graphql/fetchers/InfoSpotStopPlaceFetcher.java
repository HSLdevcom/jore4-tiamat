package org.rutebanken.tiamat.rest.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import org.rutebanken.tiamat.model.InfoSpot;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.service.InfoSpotStopPlaceResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

public class InfoSpotStopPlaceFetcher implements DataFetcher<List<StopPlace>> {

    private InfoSpotStopPlaceResolver infoSpotStopPlaceResolver;

    @Override
    public List<StopPlace> get(DataFetchingEnvironment dataFetchingEnvironment) {
        InfoSpot infoSpot = dataFetchingEnvironment.getSource();
        return infoSpotStopPlaceResolver.resolve(infoSpot);
    }
}

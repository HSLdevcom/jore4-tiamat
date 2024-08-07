package org.rutebanken.tiamat.rest.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Objects;
import org.rutebanken.tiamat.model.InfoSpot;
import org.rutebanken.tiamat.model.InfoSpotPoster;
import org.rutebanken.tiamat.repository.reference.ReferenceResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InfoSpotPosterFetcher implements DataFetcher {

    @Autowired
    private ReferenceResolver referenceResolver;

    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        InfoSpot infoSpot = (InfoSpot) dataFetchingEnvironment.getSource();
        if (infoSpot.getPosters() != null) {
            return infoSpot.getPosters()
                    .stream()
                    .map(referenceResolver::resolve)
                    .filter(Objects::nonNull)
                    .filter(p -> p instanceof InfoSpotPoster)
                    .toList();
        }
        return new ArrayList<>();
    }
}

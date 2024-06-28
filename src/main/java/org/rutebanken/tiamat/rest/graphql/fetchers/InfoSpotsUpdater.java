package org.rutebanken.tiamat.rest.graphql.fetchers;

import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import org.rutebanken.tiamat.lock.MutateLock;
import org.rutebanken.tiamat.model.DisplayTypeEnumeration;
import org.rutebanken.tiamat.model.InfoSpot;
import org.rutebanken.tiamat.model.PosterPlaceTypeEnumeration;
import org.rutebanken.tiamat.model.PosterSizeEnumeration;
import org.rutebanken.tiamat.repository.InfoSpotRepository;
import org.rutebanken.tiamat.rest.graphql.GraphQLNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.BACKLIGHT;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.DESCRIPTION;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.DISPLAY_TYPE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.FLOOR;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.ID;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.LABEL;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.MAINTENANCE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.POSTER_PLACE_SIZE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.POSTER_PLACE_TYPE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PURPOSE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.RAIL_INFORMATION;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.SPEECH_PROPERTY;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.ZONE_LABEL;

@Service("infoSpotsUpdater")
@Transactional
public class InfoSpotsUpdater implements DataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(PathLinkUpdater.class);

    @Autowired
    private MutateLock mutateLock;

    @Autowired
    private InfoSpotRepository infoSpotRepository;

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        return mutateLock.executeInLock(() -> {
            final List<Field> fields = environment.getFields();

            logger.info("Got fields {}", fields);

            return "Moi";
        });
    }

    private InfoSpot createOrUpdateInfoSpot(Map input) {
        InfoSpot target = input.containsKey(ID) ? infoSpotRepository.findFirstByNetexIdOrderByVersionDesc((String) input.get(ID)) : new InfoSpot();

        if (input.containsKey(LABEL)) {
            target.setLabel((String) input.get(LABEL));
        }
        if (input.containsKey(PURPOSE)) {
            target.setPurpose((String) input.get(PURPOSE));
        }
        if (input.containsKey(DESCRIPTION)) {
            target.setDescription((String) input.get(DESCRIPTION));
        }
        if (input.containsKey(POSTER_PLACE_TYPE)) {
            target.setPosterPlaceType((PosterPlaceTypeEnumeration) input.get(POSTER_PLACE_TYPE));
        }
        if (input.containsKey(POSTER_PLACE_SIZE)) {
            target.setPosterPlaceSize((PosterSizeEnumeration) input.get(POSTER_PLACE_SIZE));
        }
        if (input.containsKey(BACKLIGHT)) {
            target.setBacklight((Boolean) input.get(BACKLIGHT));
        }
        if (input.containsKey(MAINTENANCE)) {
            target.setMaintenance((String) input.get(MAINTENANCE));
        }
        if (input.containsKey(ZONE_LABEL)) {
            target.setZoneLabel((String) input.get(ZONE_LABEL));
        }
        if (input.containsKey(RAIL_INFORMATION)) {
            target.setRailInformation((String) input.get(RAIL_INFORMATION));
        }
        if (input.containsKey(FLOOR)) {
            target.setFloor((String) input.get(FLOOR));
        }
        if (input.containsKey(SPEECH_PROPERTY)) {
            target.setSpeechProperty((Boolean) input.get(SPEECH_PROPERTY));
        }
        if (input.containsKey(DISPLAY_TYPE)) {
            target.setDisplayType((DisplayTypeEnumeration) input.get(DISPLAY_TYPE));
        }

        // TODO: Set posters

        // TODO: Set associated stops

        return infoSpotRepository.save(target);
    }
}

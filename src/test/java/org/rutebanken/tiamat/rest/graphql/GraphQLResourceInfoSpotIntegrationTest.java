package org.rutebanken.tiamat.rest.graphql;

import java.util.Set;
import org.junit.Test;
import org.rutebanken.tiamat.model.InfoSpot;
import org.rutebanken.tiamat.model.InfoSpotPoster;
import org.rutebanken.tiamat.model.PosterPlaceTypeEnumeration;
import org.rutebanken.tiamat.model.PosterSizeEnumeration;
import org.rutebanken.tiamat.model.StopPlaceReference;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.notNullValue;

public class GraphQLResourceInfoSpotIntegrationTest extends AbstractGraphQLResourceIntegrationTest {

    @Test
    public void listInfoSpots() throws Exception {
        String testLabel = "I9876";

        InfoSpot infoSpot = new InfoSpot();
        infoSpot.setLabel(testLabel);
        infoSpot.setBacklight(true);
        infoSpot.setFloor("2");
        infoSpot.setDescription("Descriptive");
        infoSpot.setMaintenance("Maintainer");
        infoSpot.setPosterPlaceSize(PosterSizeEnumeration.CM80x120);
        infoSpot.setPosterPlaceType(PosterPlaceTypeEnumeration.STATIC);
        infoSpot.setPurpose("Purpose of info");
        infoSpot.setRailInformation("Rail 1");
        infoSpot.setZoneLabel("A");

        InfoSpotPoster poster = new InfoSpotPoster();
        poster.setLabel("posteri");
        poster.setPosterSize(PosterSizeEnumeration.A3);
        poster.setPosterType("Map poster");
        poster.setLines("1, 2, 5");

        infoSpot.setPosters(Set.of(poster));

        StopPlaceReference stopPlaceReference = new StopPlaceReference();
        stopPlaceReference.setVersion("1");
        stopPlaceReference.setRef("HSL:StopPlace:1");
        infoSpot.setStopPlaces(Set.of(stopPlaceReference));

        infoSpotRepository.save(infoSpot);

        String graphQlJsonQuery = "{" +
                "\"query\":\"{" +
                "  infoSpots { " +
                "    id " +
                "    label " +
                "    backlight " +
                "    floor " +
                "    description " +
                "    maintenance " +
                "    posterPlaceSize " +
                "    posterPlaceType " +
                "    purpose " +
                "    railInformation " +
                "    zoneLabel " +
                "    poster { " +
                "      label " +
                "      posterSize " +
                "      posterType " +
                "      lines " +
                "    } " +
                "    onStopPlace " +
                "  } " +
                "}\"," +
                "\"variables\":\"\"}";

        executeGraphQL(graphQlJsonQuery)
                .body("data.infoSpots[0].id", equalTo(infoSpot.getNetexId()))
                .body("data.infoSpots[0].label", equalTo(testLabel))
                .body("data.infoSpots[0].backlight", equalTo(infoSpot.getBacklight()))
                .body("data.infoSpots[0].floor", equalTo(infoSpot.getFloor()))
                .body("data.infoSpots[0].description", equalTo(infoSpot.getDescription()))
                .body("data.infoSpots[0].maintenance", equalTo(infoSpot.getMaintenance()))
                .body("data.infoSpots[0].posterPlaceSize", equalTo(infoSpot.getPosterPlaceSize().value()))
                .body("data.infoSpots[0].posterPlaceType", equalTo(infoSpot.getPosterPlaceType().value()))
                .body("data.infoSpots[0].purpose", equalTo(infoSpot.getPurpose()))
                .body("data.infoSpots[0].railInformation", equalTo(infoSpot.getRailInformation()))
                .body("data.infoSpots[0].zoneLabel", equalTo(infoSpot.getZoneLabel()));
    }

    @Test
    public void createInfoSpot() throws Exception {
        String graphQlJsonQuery = "{" +
                "\"query\": \"mutation { " +
                "  mutateInfoSpot( InfoSpot: {"+
                "   label: \\\"new label\\\"" +
                "   backlight: false" +
                "   floor: \\\"5\\\"" +
                "   description: \\\"new description\\\"" +
                "   maintenance: \\\"new maintainer\\\"" +
                "   posterPlaceSize: %s".formatted(PosterSizeEnumeration.A4.value()) +
                "   posterPlaceType: %s".formatted(PosterPlaceTypeEnumeration.SOUND_BEACON.value()) +
                "   purpose: \\\"new purpose\\\"" +
                "   railInformation: \\\"new rail info\\\"" +
                "   zoneLabel: \\\"N\\\"" +
                // TODO: poster
                // TODO: onStopPlace
                "  }) { " +
                "    id " +
                "    label " +
                "    backlight " +
                "    floor " +
                "    description " +
                "    maintenance " +
                "    posterPlaceSize " +
                "    posterPlaceType " +
                "    purpose " +
                "    railInformation " +
                "    zoneLabel " +
                "    poster { " +
                "      label " +
                "      posterSize " +
                "      posterType " +
                "      lines " +
                "    } " +
                "    onStopPlace " +
                "  } " +
                "}\"," +
                "\"variables\":\"\"}";

        executeGraphQL(graphQlJsonQuery)
                .body("data.mutateInfoSpot.id", notNullValue())
//                .body("data.mutateInfoSpot.version", equalTo(1)) // TODO
                .body("data.mutateInfoSpot.label", equalTo("new label"))
                .body("data.mutateInfoSpot.backlight", equalTo(false))
                .body("data.mutateInfoSpot.floor", equalTo("5"))
                .body("data.mutateInfoSpot.description", equalTo("new description"))
                .body("data.mutateInfoSpot.maintenance", equalTo("new maintainer"))
                .body("data.mutateInfoSpot.posterPlaceSize", equalTo(PosterSizeEnumeration.A4.value()))
                .body("data.mutateInfoSpot.posterPlaceType", equalTo(PosterPlaceTypeEnumeration.SOUND_BEACON.value()))
                .body("data.mutateInfoSpot.purpose", equalTo("new purpose"))
                .body("data.mutateInfoSpot.railInformation", equalTo("new rail info"))
                .body("data.mutateInfoSpot.zoneLabel", equalTo("N"));
    }

    @Test
    public void updateInfoSpot() throws Exception {
        String testLabel = "I9876";

        InfoSpot infoSpot = new InfoSpot();
        infoSpot.setLabel(testLabel);
        infoSpot.setBacklight(true);
        infoSpot.setFloor("2");
        infoSpot.setDescription("Descriptive");
        infoSpot.setMaintenance("Maintainer");
        infoSpot.setPosterPlaceSize(PosterSizeEnumeration.CM80x120);
        infoSpot.setPosterPlaceType(PosterPlaceTypeEnumeration.STATIC);
        infoSpot.setPurpose("Purpose of info");
        infoSpot.setRailInformation("Rail 1");
        infoSpot.setZoneLabel("A");

        InfoSpotPoster poster = new InfoSpotPoster();
        poster.setLabel("posteri");
        poster.setPosterSize(PosterSizeEnumeration.A3);
        poster.setPosterType("Map poster");
        poster.setLines("1, 2, 5");

        infoSpot.setPosters(Set.of(poster));

        StopPlaceReference stopPlaceReference = new StopPlaceReference();
        stopPlaceReference.setVersion("1");
        stopPlaceReference.setRef("HSL:StopPlace:1");
        infoSpot.setStopPlaces(Set.of(stopPlaceReference));

        infoSpotRepository.save(infoSpot);

        String graphQlJsonQuery = "{" +
                "\"query\": \"mutation { " +
                "  mutateInfoSpot( InfoSpot: {"+
                "   id: \\\"%s\\\"".formatted(infoSpot.getNetexId()) +
                "   label: \\\"new label\\\"" +
                "   backlight: false" +
                "   floor: \\\"5\\\"" +
                "   description: \\\"new description\\\"" +
                "   maintenance: \\\"new maintainer\\\"" +
                "   posterPlaceSize: %s".formatted(PosterSizeEnumeration.A4.value()) +
                "   posterPlaceType: %s".formatted(PosterPlaceTypeEnumeration.SOUND_BEACON.value()) +
                "   purpose: \\\"new purpose\\\"" +
                "   railInformation: \\\"new rail info\\\"" +
                "   zoneLabel: \\\"N\\\"" +
                // TODO: poster
                // TODO: onStopPlace
                "  }) { " +
                "    id " +
                "    label " +
                "    backlight " +
                "    floor " +
                "    description " +
                "    maintenance " +
                "    posterPlaceSize " +
                "    posterPlaceType " +
                "    purpose " +
                "    railInformation " +
                "    zoneLabel " +
                "    poster { " +
                "      label " +
                "      posterSize " +
                "      posterType " +
                "      lines " +
                "    } " +
                "    onStopPlace " +
                "  } " +
                "}\"," +
                "\"variables\":\"\"}";

        executeGraphQL(graphQlJsonQuery)
                .body("data.mutateInfoSpot.id", equalTo(infoSpot.getNetexId()))
                .body("data.mutateInfoSpot.label", equalTo("new label"))
                .body("data.mutateInfoSpot.backlight", equalTo(false))
                .body("data.mutateInfoSpot.floor", equalTo("5"))
                .body("data.mutateInfoSpot.description", equalTo("new description"))
                .body("data.mutateInfoSpot.maintenance", equalTo("new maintainer"))
                .body("data.mutateInfoSpot.posterPlaceSize", equalTo(PosterSizeEnumeration.A4.value()))
                .body("data.mutateInfoSpot.posterPlaceType", equalTo(PosterPlaceTypeEnumeration.SOUND_BEACON.value()))
                .body("data.mutateInfoSpot.purpose", equalTo("new purpose"))
                .body("data.mutateInfoSpot.railInformation", equalTo("new rail info"))
                .body("data.mutateInfoSpot.zoneLabel", equalTo("N"));
    }
}

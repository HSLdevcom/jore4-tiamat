package org.rutebanken.tiamat.rest.graphql;

import org.junit.Test;
import org.rutebanken.tiamat.model.InfoSpot;
import org.rutebanken.tiamat.model.PosterPlaceTypeEnumeration;
import org.rutebanken.tiamat.model.PosterSizeEnumeration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;

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
                "  } " +
                "}\"," +
                "\"variables\":\"\"}";

        executeGraphQL(graphQlJsonQuery)
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
}

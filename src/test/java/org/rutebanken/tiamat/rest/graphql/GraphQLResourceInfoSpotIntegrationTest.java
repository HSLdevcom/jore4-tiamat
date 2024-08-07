package org.rutebanken.tiamat.rest.graphql;

import java.util.Set;
import org.junit.Test;
import org.rutebanken.tiamat.model.InfoSpot;
import org.rutebanken.tiamat.model.InfoSpotPoster;
import org.rutebanken.tiamat.model.InfoSpotPosterRef;
import org.rutebanken.tiamat.model.InfoSpotTypeEnumeration;
import org.rutebanken.tiamat.model.PosterSizeEnumeration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

public class GraphQLResourceInfoSpotIntegrationTest extends AbstractGraphQLResourceIntegrationTest {

    static String INFO_SPOT_ID = "NSR:InfoSpot:1";
    static String INFO_SPOT2_ID = "NSR:InfoSpot:2";

    static InfoSpot oldInfoSpot;
    static InfoSpot updatedInfoSpot;
    static InfoSpot otherInfoSpot;

    static InfoSpotPoster oldPoster;
    static InfoSpotPoster updatedPoster;
    static InfoSpotPoster otherPoster;

    @Test
    public void testListInfoSpots() throws Exception {

        insertInfoSpots();

        String graphQlJsonQuery = """
            query {
                infoSpots {
                    id
                    label
                    infoSpotType
                    backlight
                    floor
                    description
                    maintenance
                    posterPlaceSize
                    purpose
                    railInformation
                    zoneLabel
                    speechProperty
                    infoSpotLocations
                    poster {
                        label
                        posterSize
                        lines
                    }
                }
            }
        """;

        executeGraphqQLQueryOnly(graphQlJsonQuery)
                .rootPath("data.infoSpots[0]")
                .body("label", equalTo(updatedInfoSpot.getLabel()))
                .body("infoSpotType", equalTo(updatedInfoSpot.getInfoSpotType().value()))
                .body("backlight", equalTo(updatedInfoSpot.getBacklight()))
                .body("floor", equalTo(updatedInfoSpot.getFloor()))
                .body("description", equalTo(updatedInfoSpot.getDescription()))
                .body("maintenance", equalTo(updatedInfoSpot.getMaintenance()))
                .body("posterPlaceSize", equalTo(updatedInfoSpot.getPosterPlaceSize().value()))
                .body("purpose", equalTo(updatedInfoSpot.getPurpose()))
                .body("railInformation", equalTo(updatedInfoSpot.getRailInformation()))
                .body("zoneLabel", equalTo(updatedInfoSpot.getZoneLabel()))
                .body("speechProperty", equalTo(updatedInfoSpot.getSpeechProperty()))
                .body("infoSpotLocations", hasItems(updatedInfoSpot.getInfoSpotLocations().toArray()))
                .appendRootPath("poster[0]")
                .body("label", equalTo(updatedPoster.getLabel()))
                .body("posterSize", equalTo(updatedPoster.getPosterSize().value()))
                .body("lines", equalTo(updatedPoster.getLines()))
                .rootPath("data.infoSpots[1]")
                .body("label", equalTo(otherInfoSpot.getLabel()))
                .body("infoSpotType", equalTo(otherInfoSpot.getInfoSpotType().value()))
                .body("backlight", equalTo(otherInfoSpot.getBacklight()))
                .body("floor", equalTo(otherInfoSpot.getFloor()))
                .body("description", equalTo(otherInfoSpot.getDescription()))
                .body("maintenance", equalTo(otherInfoSpot.getMaintenance()))
                .body("posterPlaceSize", equalTo(otherInfoSpot.getPosterPlaceSize().value()))
                .body("purpose", equalTo(otherInfoSpot.getPurpose()))
                .body("railInformation", equalTo(otherInfoSpot.getRailInformation()))
                .body("zoneLabel", equalTo(otherInfoSpot.getZoneLabel()))
                .body("speechProperty", equalTo(otherInfoSpot.getSpeechProperty()))
                .body("infoSpotLocations", hasItems(otherInfoSpot.getInfoSpotLocations().toArray()))
                .appendRootPath("poster[0]")
                .body("label", equalTo(otherPoster.getLabel()))
                .body("posterSize", equalTo(otherPoster.getPosterSize().value()))
                .body("lines", equalTo(otherPoster.getLines()));
    }

    @Test
    public void testQueryInfoSpotById() throws Exception {

        insertInfoSpots();

        String graphQlJsonQuery = """
            query {
                infoSpots (id: "%s"){
                    id
                    label
                    infoSpotType
                    backlight
                    floor
                    description
                    maintenance
                    posterPlaceSize
                    purpose
                    railInformation
                    zoneLabel
                    speechProperty
                    infoSpotLocations
                    poster {
                        label
                        posterSize
                        lines
                    }
                }
            }
        """.formatted(INFO_SPOT_ID);

        executeGraphqQLQueryOnly(graphQlJsonQuery)
                .rootPath("data.infoSpots[0]")
                .body("id", equalTo(INFO_SPOT_ID))
                .body("label", equalTo(updatedInfoSpot.getLabel()))
                .body("infoSpotType", equalTo(updatedInfoSpot.getInfoSpotType().value()))
                .body("backlight", equalTo(updatedInfoSpot.getBacklight()))
                .body("floor", equalTo(updatedInfoSpot.getFloor()))
                .body("description", equalTo(updatedInfoSpot.getDescription()))
                .body("maintenance", equalTo(updatedInfoSpot.getMaintenance()))
                .body("posterPlaceSize", equalTo(updatedInfoSpot.getPosterPlaceSize().value()))
                .body("purpose", equalTo(updatedInfoSpot.getPurpose()))
                .body("railInformation", equalTo(updatedInfoSpot.getRailInformation()))
                .body("zoneLabel", equalTo(updatedInfoSpot.getZoneLabel()))
                .body("speechProperty", equalTo(updatedInfoSpot.getSpeechProperty()))
                .body("infoSpotLocations", hasItems(updatedInfoSpot.getInfoSpotLocations().toArray()))
                .appendRootPath("poster[0]")
                .body("label", equalTo(updatedPoster.getLabel()))
                .body("posterSize", equalTo(updatedPoster.getPosterSize().value()))
                .body("lines", equalTo(updatedPoster.getLines()));
    }

    @Test
    public void saveInfoSpot() throws Exception {
        String label = "I9876";
        InfoSpotTypeEnumeration infoSpotType = InfoSpotTypeEnumeration.SOUND_BEACON;
        Boolean backlight = true;
        String floor = "2";
        String description = "Description of info spot";
        String maintenance = "Maintainer";
        PosterSizeEnumeration size = PosterSizeEnumeration.CM80x120;
        String purpose = "Purpose of info";
        String railInformation = "Rail 1";
        String zoneLabel = "A";
        Boolean speechProperty = true;
        String location1 = "NSR:Shelter:1";
        String location2 = "NSH:StopPlace:2";

        String posterLabel = "Info Poster";
        PosterSizeEnumeration posterSize = PosterSizeEnumeration.A3;
        String posterLines = "1, 4, 5";


        String graphQlJsonQuery = """
                    mutation {
                        infoSpot: mutateInfoSpots(
                            InfoSpot: {
                                label: "%s"
                                infoSpotType: %s
                                backlight: %s
                                floor: "%s"
                                description: "%s"
                                maintenance: "%s"
                                posterPlaceSize: %s
                                purpose: "%s"
                                railInformation: "%s"
                                zoneLabel: "%s"
                                speechProperty: %s
                                infoSpotLocations: [
                                    "%s",
                                    "%s"
                                ],
                                poster: [
                                    {
                                        label: "%s",
                                        posterSize: %s,
                                        lines: "%s"
                                    }
                                ]
                            }
                        ) {
                            id
                            label
                            infoSpotType
                            backlight
                            floor
                            description
                            maintenance
                            posterPlaceSize
                            purpose
                            railInformation
                            zoneLabel
                            speechProperty
                            infoSpotLocations
                            poster {
                                label
                                posterSize
                                lines
                            }
                        }
                    }
                """.formatted(
                label,
                infoSpotType.toString().toLowerCase(),
                backlight.toString(),
                floor,
                description,
                maintenance,
                size.toString().toLowerCase(),
                purpose,
                railInformation,
                zoneLabel,
                speechProperty,
                location1,
                location2,
                posterLabel,
                posterSize.toString().toLowerCase(),
                posterLines
        );

        executeGraphqQLQueryOnly(graphQlJsonQuery)
                .rootPath("data.infoSpot[0]")
                .body("label", equalTo(label))
                .body("infoSpotType", equalTo(infoSpotType.value().toLowerCase()))
                .body("backlight", equalTo(backlight))
                .body("floor", equalTo(floor))
                .body("description", equalTo(description))
                .body("maintenance", equalTo(maintenance))
                .body("posterPlaceSize", equalTo(size.value()))
                .body("purpose", equalTo(purpose))
                .body("railInformation", equalTo(railInformation))
                .body("zoneLabel", equalTo(zoneLabel))
                .body("speechProperty", equalTo(speechProperty))
                .body("infoSpotLocations", hasItems(location1, location2))
                .appendRootPath("poster[0]")
                .body("label", equalTo(posterLabel))
                .body("posterSize", equalTo(posterSize.value()))
                .body("lines", equalTo(posterLines));
    }

    @Test
    public void updateInfoSpot() throws Exception {
        String location1 = "NSR:Shelter:2";
        String location2 = "NSH:StopPlace:1";

        InfoSpot infoSpot = new InfoSpot();
        infoSpot.setLabel("I9876");
        infoSpot.setInfoSpotType(InfoSpotTypeEnumeration.STATIC);
        infoSpot.setBacklight(true);
        infoSpot.setFloor("2");
        infoSpot.setDescription("Descriptive");
        infoSpot.setMaintenance("Maintainer");
        infoSpot.setPosterPlaceSize(PosterSizeEnumeration.CM80x120);
        infoSpot.setPurpose("Purpose of info");
        infoSpot.setRailInformation("Rail 1");
        infoSpot.setZoneLabel("A");
        infoSpot.setSpeechProperty(true);
        infoSpot.setInfoSpotLocations(Set.of(location1, location2));

        InfoSpotPoster poster = new InfoSpotPoster();
        poster.setNetexId("NSR:InfoSpotPoster:1");
        poster.setVersion(1);
        poster.setLabel("Informative poster");
        poster.setPosterSize(PosterSizeEnumeration.A4);
        poster.setLines("1, 2, 5");

        infoSpotPosterRepository.save(poster);

        InfoSpotPosterRef posterRef = new InfoSpotPosterRef(poster);

        infoSpot.setPosters(Set.of(posterRef));

        infoSpotRepository.save(infoSpot);


        String graphQlJsonQuery = """
                    mutation {
                        infoSpot: mutateInfoSpots(
                            InfoSpot: {
                                label: "%s"
                                infoSpotType: %s
                                backlight: %s
                                floor: "%s"
                                description: "%s"
                                maintenance: "%s"
                                posterPlaceSize: %s
                                purpose: "%s"
                                railInformation: "%s"
                                zoneLabel: "%s"
                                speechProperty: %s
                                infoSpotLocations: [
                                    "%s",
                                    "%s"
                                ],
                                poster: [
                                    {
                                        label: "%s",
                                        posterSize: %s,
                                        lines: "%s"
                                    }
                                ]
                            }
                        ) {
                            id
                            label
                            infoSpotType
                            backlight
                            floor
                            description
                            maintenance
                            posterPlaceSize
                            purpose
                            railInformation
                            zoneLabel
                            speechProperty
                            infoSpotLocations
                            poster {
                                label
                                posterSize
                                lines
                            }
                        }
                    }
                """.formatted(
                infoSpot.getLabel(),
                infoSpot.getInfoSpotType().value().toLowerCase(),
                infoSpot.getBacklight().toString(),
                infoSpot.getFloor(),
                infoSpot.getDescription(),
                infoSpot.getMaintenance(),
                infoSpot.getPosterPlaceSize().toString().toLowerCase(),
                infoSpot.getPurpose(),
                infoSpot.getRailInformation(),
                infoSpot.getZoneLabel(),
                infoSpot.getSpeechProperty(),
                "NSR:InfoSpot:1",
                "NSR:Shelter:1",
                poster.getLabel(),
                poster.getPosterSize().toString().toLowerCase(),
                poster.getLines()
        );

        executeGraphqQLQueryOnly(graphQlJsonQuery)
                .rootPath("data.infoSpot[0]")
                .body("label", equalTo(infoSpot.getLabel()))
                .body("infoSpotType", equalTo(infoSpot.getInfoSpotType().value()))
                .body("backlight", equalTo(infoSpot.getBacklight()))
                .body("floor", equalTo(infoSpot.getFloor()))
                .body("description", equalTo(infoSpot.getDescription()))
                .body("maintenance", equalTo(infoSpot.getMaintenance()))
                .body("posterPlaceSize", equalTo(infoSpot.getPosterPlaceSize().value()))
                .body("purpose", equalTo(infoSpot.getPurpose()))
                .body("railInformation", equalTo(infoSpot.getRailInformation()))
                .body("zoneLabel", equalTo(infoSpot.getZoneLabel()))
                .body("speechProperty", equalTo(infoSpot.getSpeechProperty()))
                .appendRootPath("poster[0]")
                .body("label", equalTo(poster.getLabel()))
                .body("posterSize", equalTo(poster.getPosterSize().value()))
                .body("lines", equalTo(poster.getLines()));
    }

    private void insertInfoSpots() {
        // Insert base info spot
        String oldLocation1 = "NSR:Shelter:2";
        String oldLocation2 = "NSH:StopPlace:1";

        oldInfoSpot = new InfoSpot();
        oldInfoSpot.setNetexId(INFO_SPOT_ID);
        oldInfoSpot.setVersion(1);
        oldInfoSpot.setLabel("I9876");
        oldInfoSpot.setInfoSpotType(InfoSpotTypeEnumeration.STATIC);
        oldInfoSpot.setBacklight(true);
        oldInfoSpot.setFloor("2");
        oldInfoSpot.setDescription("Descriptive");
        oldInfoSpot.setMaintenance("Maintainer");
        oldInfoSpot.setPosterPlaceSize(PosterSizeEnumeration.CM80x120);
        oldInfoSpot.setPurpose("Purpose of info");
        oldInfoSpot.setRailInformation("Rail 1");
        oldInfoSpot.setZoneLabel("A");
        oldInfoSpot.setSpeechProperty(true);
        oldInfoSpot.setInfoSpotLocations(Set.of(oldLocation1, oldLocation2));

        oldPoster = new InfoSpotPoster();
        oldPoster.setNetexId("NSR:InfoSpotPoster:1");
        oldPoster.setVersion(1);
        oldPoster.setLabel("Informative poster");
        oldPoster.setPosterSize(PosterSizeEnumeration.A4);
        oldPoster.setLines("1, 2, 5");

        infoSpotPosterRepository.save(oldPoster);

        InfoSpotPosterRef oldPosterRef = new InfoSpotPosterRef(oldPoster);

        oldInfoSpot.setPosters(Set.of(oldPosterRef));

        infoSpotRepository.save(oldInfoSpot);


        // Override existing info spot with new version
        String location1 = "NSR:Shelter:4";
        String location2 = "NSH:StopPlace:4";

        updatedInfoSpot = new InfoSpot();
        updatedInfoSpot.setNetexId(INFO_SPOT_ID);
        updatedInfoSpot.setVersion(2);
        updatedInfoSpot.setLabel("I9877");
        updatedInfoSpot.setInfoSpotType(InfoSpotTypeEnumeration.STATIC);
        updatedInfoSpot.setBacklight(false);
        updatedInfoSpot.setFloor("3");
        updatedInfoSpot.setDescription("New Description");
        updatedInfoSpot.setMaintenance("New Maintainer");
        updatedInfoSpot.setPosterPlaceSize(PosterSizeEnumeration.A3);
        updatedInfoSpot.setPurpose("New purpose of info");
        updatedInfoSpot.setRailInformation("Rail 2");
        updatedInfoSpot.setZoneLabel("B");
        updatedInfoSpot.setSpeechProperty(false);
        updatedInfoSpot.setInfoSpotLocations(Set.of(location1, location2));

        updatedPoster = new InfoSpotPoster();
        updatedPoster.setNetexId("NSR:InfoSpotPoster:1");
        updatedPoster.setVersion(2);
        updatedPoster.setLabel("More informative poster");
        updatedPoster.setPosterSize(PosterSizeEnumeration.A3);
        updatedPoster.setLines("3, 4");

        infoSpotPosterRepository.save(updatedPoster);

        InfoSpotPosterRef posterRef = new InfoSpotPosterRef(updatedPoster);

        updatedInfoSpot.setPosters(Set.of(posterRef));

        infoSpotRepository.save(updatedInfoSpot);


        // Insert base info spot
        String otherLocation1 = "NSR:Shelter:5";
        String otherLocation2 = "NSH:StopPlace:7";

        otherInfoSpot = new InfoSpot();
        otherInfoSpot.setNetexId(INFO_SPOT2_ID);
        otherInfoSpot.setVersion(1);
        otherInfoSpot.setLabel("I1234");
        otherInfoSpot.setInfoSpotType(InfoSpotTypeEnumeration.DYNAMIC);
        otherInfoSpot.setBacklight(true);
        otherInfoSpot.setFloor("3");
        otherInfoSpot.setDescription("Other description");
        otherInfoSpot.setMaintenance("Maintainer three");
        otherInfoSpot.setPosterPlaceSize(PosterSizeEnumeration.A4);
        otherInfoSpot.setPurpose("Other purpose");
        otherInfoSpot.setRailInformation("Rail 3");
        otherInfoSpot.setZoneLabel("C");
        otherInfoSpot.setSpeechProperty(false);
        otherInfoSpot.setInfoSpotLocations(Set.of(otherLocation1, otherLocation2));

        otherPoster = new InfoSpotPoster();
        otherPoster.setNetexId("NSR:InfoSpotPoster:2");
        otherPoster.setVersion(1);
        otherPoster.setLabel("Other poster");
        otherPoster.setPosterSize(PosterSizeEnumeration.A4);
        otherPoster.setLines("7");

        infoSpotPosterRepository.save(otherPoster);

        InfoSpotPosterRef otherPosterRef = new InfoSpotPosterRef(otherPoster);

        otherInfoSpot.setPosters(Set.of(otherPosterRef));

        infoSpotRepository.save(otherInfoSpot);
    }
}

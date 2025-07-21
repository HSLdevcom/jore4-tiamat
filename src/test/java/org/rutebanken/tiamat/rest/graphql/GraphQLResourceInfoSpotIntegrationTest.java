package org.rutebanken.tiamat.rest.graphql;

import java.time.Instant;
import java.util.Set;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.rutebanken.tiamat.config.GeometryFactoryConfig;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.InfoSpot;
import org.rutebanken.tiamat.model.InfoSpotPoster;
import org.rutebanken.tiamat.model.InfoSpotPosterRef;
import org.rutebanken.tiamat.model.InfoSpotTypeEnumeration;
import org.rutebanken.tiamat.model.PosterSizeEnumeration;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.ValidBetween;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

public class GraphQLResourceInfoSpotIntegrationTest extends AbstractGraphQLResourceIntegrationTest {

    private final GeometryFactory geometryFactory = new GeometryFactoryConfig().geometryFactory();

    private static final Size SIZE_A3 = new Size(297, 420);
    private static final Size SIZE_A4 = new Size(210, 297);
    private static final Size SIZE_80_120 = new Size(800, 1200);

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
                    version
                    label
                    infoSpotType
                    backlight
                    floor
                    description {
                        value
                        lang
                    }
                    maintenance
                    width
                    height
                    purpose
                    railInformation
                    zoneLabel
                    speechProperty
                    infoSpotLocations
                    geometry {
                        coordinates
                        type
                    }
                    poster {
                        label
                        width
                        height
                        lines
                    }
                }
            }
        """;

        executeGraphqQLQueryOnly(graphQlJsonQuery)
                .body("data.infoSpots", hasSize(2))
                .rootPath("data.infoSpots[0]")
                .body("id", equalTo(updatedInfoSpot.getNetexId()))
                .body("version", equalTo(String.valueOf(updatedInfoSpot.getVersion())))
                .body("label", equalTo(updatedInfoSpot.getLabel()))
                .body("infoSpotType", equalTo(updatedInfoSpot.getInfoSpotType().value()))
                .body("backlight", equalTo(updatedInfoSpot.getBacklight()))
                .body("floor", equalTo(updatedInfoSpot.getFloor()))
                .body("description.value", equalTo(updatedInfoSpot.getDescription().getValue()))
                .body("maintenance", equalTo(updatedInfoSpot.getMaintenance()))
                // Deprecated field, test compatability
                .body("posterPlaceSize", equalTo(updatedInfoSpot.getPosterPlaceSize().value()))
                .body("width", equalTo(updatedInfoSpot.getWidth()))
                .body("height", equalTo(updatedInfoSpot.getHeight()))
                .body("purpose", equalTo(updatedInfoSpot.getPurpose()))
                .body("railInformation", equalTo(updatedInfoSpot.getRailInformation()))
                .body("zoneLabel", equalTo(updatedInfoSpot.getZoneLabel()))
                .body("speechProperty", equalTo(updatedInfoSpot.getSpeechProperty()))
                .body("infoSpotLocations", hasItems(updatedInfoSpot.getInfoSpotLocations().toArray()))
                .body("geometry.coordinates[0]", equalTo((float)updatedInfoSpot.getCentroid().getX()))
                .body("geometry.coordinates[1]", equalTo((float)updatedInfoSpot.getCentroid().getY()))
                .appendRootPath("poster[0]")
                .body("label", equalTo(updatedPoster.getLabel()))
                // Deprecated field, test compatability
                .body("posterSize", equalTo(updatedPoster.getPosterSize().value()))
                .body("width", equalTo(updatedPoster.getWidth()))
                .body("height", equalTo(updatedPoster.getHeight()))
                .body("lines", equalTo(updatedPoster.getLines()))
                .rootPath("data.infoSpots[1]")
                .body("id", equalTo(otherInfoSpot.getNetexId()))
                .body("version", equalTo(String.valueOf(otherInfoSpot.getVersion())))
                .body("label", equalTo(otherInfoSpot.getLabel()))
                .body("infoSpotType", equalTo(otherInfoSpot.getInfoSpotType().value()))
                .body("backlight", equalTo(otherInfoSpot.getBacklight()))
                .body("floor", equalTo(otherInfoSpot.getFloor()))
                .body("description.value", equalTo(otherInfoSpot.getDescription().getValue()))
                .body("maintenance", equalTo(otherInfoSpot.getMaintenance()))
                .body("width", equalTo(otherInfoSpot.getWidth()))
                .body("height", equalTo(otherInfoSpot.getHeight()))
                .body("purpose", equalTo(otherInfoSpot.getPurpose()))
                .body("railInformation", equalTo(otherInfoSpot.getRailInformation()))
                .body("zoneLabel", equalTo(otherInfoSpot.getZoneLabel()))
                .body("speechProperty", equalTo(otherInfoSpot.getSpeechProperty()))
                .body("infoSpotLocations", hasItems(otherInfoSpot.getInfoSpotLocations().toArray()))
                .body("geometry.coordinates[0]", equalTo((float)otherInfoSpot.getCentroid().getX()))
                .body("geometry.coordinates[1]", equalTo((float)otherInfoSpot.getCentroid().getY()))
                .appendRootPath("poster[0]")
                .body("label", equalTo(otherPoster.getLabel()))
                .body("width", equalTo(otherPoster.getWidth()))
                .body("height", equalTo(otherPoster.getHeight()))
                .body("lines", equalTo(otherPoster.getLines()));
    }

    @Test
    public void testQueryInfoSpotById() throws Exception {

        insertInfoSpots();

        String graphQlJsonQuery = """
            query {
                infoSpots (id: "%s"){
                    id
                    version
                    label
                    infoSpotType
                    backlight
                    floor
                    description {
                        value
                        lang
                    }
                    maintenance
                    width
                    height
                    purpose
                    railInformation
                    zoneLabel
                    speechProperty
                    infoSpotLocations
                    geometry {
                        coordinates
                        type
                    }
                    poster {
                        label
                        width
                        height
                        lines
                    }
                }
            }
        """.formatted(INFO_SPOT_ID);

        executeGraphqQLQueryOnly(graphQlJsonQuery)
                .body("data.infoSpots", hasSize(1))
                .rootPath("data.infoSpots[0]")
                .body("id", equalTo(INFO_SPOT_ID))
                .body("version", equalTo(String.valueOf(updatedInfoSpot.getVersion())))
                .body("label", equalTo(updatedInfoSpot.getLabel()))
                .body("infoSpotType", equalTo(updatedInfoSpot.getInfoSpotType().value()))
                .body("backlight", equalTo(updatedInfoSpot.getBacklight()))
                .body("floor", equalTo(updatedInfoSpot.getFloor()))
                .body("description.value", equalTo(updatedInfoSpot.getDescription().getValue()))
                .body("maintenance", equalTo(updatedInfoSpot.getMaintenance()))
                .body("width", equalTo(updatedInfoSpot.getWidth()))
                .body("height", equalTo(updatedInfoSpot.getHeight()))
                .body("purpose", equalTo(updatedInfoSpot.getPurpose()))
                .body("railInformation", equalTo(updatedInfoSpot.getRailInformation()))
                .body("zoneLabel", equalTo(updatedInfoSpot.getZoneLabel()))
                .body("speechProperty", equalTo(updatedInfoSpot.getSpeechProperty()))
                .body("infoSpotLocations", hasItems(updatedInfoSpot.getInfoSpotLocations().toArray()))
                .body("geometry.coordinates[0]", equalTo((float)updatedInfoSpot.getCentroid().getX()))
                .body("geometry.coordinates[1]", equalTo((float)updatedInfoSpot.getCentroid().getY()))
                .appendRootPath("poster[0]")
                .body("label", equalTo(updatedPoster.getLabel()))
                .body("width", equalTo(updatedPoster.getWidth()))
                .body("height", equalTo(updatedPoster.getHeight()))
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
        Size size = SIZE_80_120;
        String purpose = "Purpose of info";
        String railInformation = "Rail 1";
        String zoneLabel = "A";
        Boolean speechProperty = true;
        String location1 = "NSR:Shelter:1";
        String location2 = "NSH:StopPlace:2";

        String posterLabel = "Info Poster";
        Size posterSize = SIZE_A3;
        String posterLines = "1, 4, 5";
        Float lat = 60.0F;
        Float lon = 50.0F;

        String graphQlJsonQuery = """
            mutation {
                infoSpot: mutateInfoSpots(
                    infoSpot: {
                        label: "%s"
                        infoSpotType: %s
                        backlight: %s
                        floor: "%s"
                        description: {
                            value: "%s"
                        }
                        maintenance: "%s"
                        width: %s
                        height: %s
                        purpose: "%s"
                        railInformation: "%s"
                        zoneLabel: "%s"
                        speechProperty: %s
                        infoSpotLocations: [
                            "%s",
                            "%s"
                        ],
                        geometry: {
                            coordinates: [ %s, %s ],
                            type: Point
                        },
                        poster: [
                            {
                                label: "%s",
                                width: %s,
                                height: %s,
                                lines: "%s"
                            }
                        ]
                    }
                ) {
                    id
                    version
                    label
                    infoSpotType
                    backlight
                    floor
                    description {
                        value
                        lang
                    }
                    maintenance
                    width
                    height
                    purpose
                    railInformation
                    zoneLabel
                    speechProperty
                    infoSpotLocations
                    geometry {
                        coordinates
                        type
                    }
                    poster {
                        label
                        width
                        height
                        lines
                    }
                }
            }
            """.formatted(
            label,
            infoSpotType.value(),
            backlight.toString(),
            floor,
            description,
            maintenance,
            size.width(),
            size.height(),
            purpose,
            railInformation,
            zoneLabel,
            speechProperty,
            location1,
            location2,
            lat.toString(),
            lon.toString(),
            posterLabel,
            posterSize.width(),
            posterSize.height(),
            posterLines
        );

        executeGraphqQLQueryOnly(graphQlJsonQuery)
                .rootPath("data.infoSpot[0]")
                .body("version", equalTo("1"))
                .body("label", equalTo(label))
                .body("infoSpotType", equalTo(infoSpotType.value()))
                .body("backlight", equalTo(backlight))
                .body("floor", equalTo(floor))
                .body("description.value", equalTo(description))
                .body("maintenance", equalTo(maintenance))
                .body("width", equalTo(size.width()))
                .body("height", equalTo(size.height()))
                .body("purpose", equalTo(purpose))
                .body("railInformation", equalTo(railInformation))
                .body("zoneLabel", equalTo(zoneLabel))
                .body("speechProperty", equalTo(speechProperty))
                .body("infoSpotLocations", hasItems(location1, location2))
                .body("geometry.coordinates[0]", equalTo(lat))
                .body("geometry.coordinates[1]", equalTo(lon))
                .appendRootPath("poster[0]")
                .body("label", equalTo(posterLabel))
                .body("width", equalTo(posterSize.width()))
                .body("height", equalTo(posterSize.height()))
                .body("lines", equalTo(posterLines));
    }

    @Test
    public void updateInfoSpot() throws Exception {
        insertInfoSpots();

        long version = otherInfoSpot.getVersion() + 1;

        String netexId = INFO_SPOT2_ID;
        String floor = "2";
        String description = "Descriptive";
        String maintenance = "Maintainer";
        Size size = SIZE_80_120;
        String purpose = "Purpose of info";
        String railInformation = "Rail 1";
        String zoneLabel = "A";
        Boolean speechProperty = true;
        String location1 = "NSR:Shelter:2";
        String location2 = "NSH:StopPlace:1";
        float lat = 51.0F;
        float lon = 52.0F;

        String posterLabel = ("Informative poster");
        Size posterSize = SIZE_A4;
        String posterLines = "1, 2, 5";


        String graphQlJsonMutation = """
            mutation {
                infoSpot: mutateInfoSpots(
                    infoSpot: {
                        id: "%s"
                        floor: "%s"
                        description: {
                            value: "%s"
                        }
                        maintenance: "%s"
                        width: %s
                        height: %s
                        purpose: "%s"
                        railInformation: "%s"
                        zoneLabel: "%s"
                        speechProperty: %s
                        infoSpotLocations: [
                            "%s",
                            "%s"
                        ],
                        geometry: {
                            coordinates: [ %s, %s ],
                            type: Point
                        },
                        poster: [
                            {
                                label: "%s",
                                width: %s,
                                height: %s,
                                lines: "%s"
                            }
                        ]
                    }
                ) {
                    id
                    version
                    label
                    infoSpotType
                    backlight
                    floor
                    description {
                        value
                        lang
                    }
                    maintenance
                    width
                    height
                    purpose
                    railInformation
                    zoneLabel
                    speechProperty
                    infoSpotLocations
                    geometry {
                        coordinates
                        type
                    }
                    poster {
                        label
                        width
                        height
                        lines
                    }
                }
            }
            """.formatted(
            netexId,
            floor,
            description,
            maintenance,
            size.width(),
            size.height(),
            purpose,
            railInformation,
            zoneLabel,
            speechProperty,
            location1,
            location2,
            lat,
            lon,
            posterLabel,
            posterSize.width(),
            posterSize.height(),
            posterLines
        );

        executeGraphqQLQueryOnly(graphQlJsonMutation)
                .rootPath("data.infoSpot[0]")
                .body("id", equalTo(netexId))
                .body("version", equalTo(String.valueOf(version)))
                .body("label", equalTo(otherInfoSpot.getLabel()))
                .body("infoSpotType", equalTo(otherInfoSpot.getInfoSpotType().value()))
                .body("backlight", equalTo(otherInfoSpot.getBacklight()))
                .body("floor", equalTo(floor))
                .body("description.value", equalTo(description))
                .body("maintenance", equalTo(maintenance))
                .body("width", equalTo(size.width()))
                .body("height", equalTo(size.height()))
                .body("purpose", equalTo(purpose))
                .body("railInformation", equalTo(railInformation))
                .body("zoneLabel", equalTo(zoneLabel))
                .body("speechProperty", equalTo(speechProperty))
                .body("infoSpotLocations", hasItems(location1, location2))
                .body("geometry.coordinates[0]", equalTo(lat))
                .body("geometry.coordinates[1]", equalTo(lon))
                .body("poster", hasSize(1))
                .appendRootPath("poster[0]")
                .body("label", equalTo(posterLabel))
                .body("width", equalTo(posterSize.width()))
                .body("height", equalTo(posterSize.height()))
                .body("lines", equalTo(posterLines));


        // Make a new query to ensure all changes were persisted properly
        String graphQlJsonQuery = """
            query {
                infoSpots (id: "%s"){
                    id
                    version
                    label
                    infoSpotType
                    backlight
                    floor
                    description {
                        value
                        lang
                    }
                    maintenance
                    width
                    height
                    purpose
                    railInformation
                    zoneLabel
                    speechProperty
                    infoSpotLocations
                    geometry {
                        coordinates
                        type
                    }
                    poster {
                        label
                        width
                        height
                        lines
                    }
                }
            }
        """.formatted(netexId);

        executeGraphqQLQueryOnly(graphQlJsonQuery)
                .rootPath("data.infoSpots[0]")
                .body("id", equalTo(netexId))
                .body("version", equalTo(String.valueOf(version)))
                .body("label", equalTo(otherInfoSpot.getLabel()))
                .body("infoSpotType", equalTo(otherInfoSpot.getInfoSpotType().value()))
                .body("backlight", equalTo(otherInfoSpot.getBacklight()))
                .body("floor", equalTo(floor))
                .body("description.value", equalTo(description))
                .body("maintenance", equalTo(maintenance))
                .body("width", equalTo(size.width()))
                .body("height", equalTo(size.height()))
                .body("purpose", equalTo(purpose))
                .body("railInformation", equalTo(railInformation))
                .body("zoneLabel", equalTo(zoneLabel))
                .body("speechProperty", equalTo(speechProperty))
                .body("infoSpotLocations", hasItems(location1, location2))
                .body("geometry.coordinates[0]", equalTo(lat))
                .body("geometry.coordinates[1]", equalTo(lon))
                .body("poster", hasSize(1))
                .appendRootPath("poster[0]")
                .body("label", equalTo(posterLabel))
                .body("width", equalTo(posterSize.width()))
                .body("height", equalTo(posterSize.height()))
                .body("lines", equalTo(posterLines));
    }

    @Test
    public void updateInfoSpotByOldEnumApi() {
        insertInfoSpots();

        long version = otherInfoSpot.getVersion() + 1;
        String netexId = INFO_SPOT2_ID;
        String posterLabel = ("Informative poster");

        String graphQlJsonMutation = """
                mutation {
                    infoSpot: mutateInfoSpots(
                        infoSpot: {
                            id: "%s"
                            posterPlaceSize: %s
                            poster: [
                                {
                                    label: "%s"
                                    posterSize: %s
                                }
                            ]
                        }
                    ) {
                        id
                        posterPlaceSize
                        width
                        height
                        poster {
                            posterSize
                            width
                            height
                        }
                    }
                }
                """.formatted(
                netexId,
                PosterSizeEnumeration.A3.value(),
                posterLabel,
                PosterSizeEnumeration.A3.value()
        );

        executeGraphqQLQueryOnly(graphQlJsonMutation)
                .rootPath("data.infoSpot[0]")
                .body("id", equalTo(netexId))
                .body("width", equalTo(PosterSizeEnumeration.A3.width()))
                .body("height", equalTo(PosterSizeEnumeration.A3.height()))
                .body("posterPlaceSize", equalTo(PosterSizeEnumeration.A3.value()))
                .body("poster", hasSize(1))
                .appendRootPath("poster[0]")
                .body("width", equalTo(PosterSizeEnumeration.A3.width()))
                .body("height", equalTo(PosterSizeEnumeration.A3.height()))
                .body("posterSize", equalTo(PosterSizeEnumeration.A3.value()));
    }

    @Test
    public void listInfoSpotsForStopPlaceTest() {
        StopPlace stopPlace = new StopPlace();
        stopPlace.setCentroid(geometryFactory.createPoint(new Coordinate(5, 60)));
        stopPlace.setName(new EmbeddableMultilingualString("info stop place"));
        stopPlace.setValidBetween(new ValidBetween(Instant.now()));
        stopPlace = stopPlaceRepository.save(stopPlace);

        var infoSpot = new InfoSpot();
        infoSpot.setVersion(1);
        infoSpot.setLabel("I1111");
        infoSpot.setInfoSpotType(InfoSpotTypeEnumeration.STATIC);
        infoSpot.setBacklight(true);
        infoSpot.setFloor("2");
        infoSpot.setDescription(new EmbeddableMultilingualString("Descriptive"));
        infoSpot.setMaintenance("Maintainer");
        infoSpot.setWidth(SIZE_80_120.width());
        infoSpot.setHeight(SIZE_80_120.height());
        infoSpot.setPurpose("Purpose of info");
        infoSpot.setRailInformation("Rail 1");
        infoSpot.setZoneLabel("A");
        infoSpot.setInfoSpotLocations(Set.of(stopPlace.getNetexId()));
        infoSpot.setCentroid(stopPlace.getCentroid());

        infoSpot = infoSpotRepository.save(infoSpot);

        String graphQlJsonQuery = """
                  {
                  stopPlace: stopPlace (id:"%s") {
                    id
                    name { value }
                    infoSpots {
                        id
                        label
                        floor
                        }
                    }
                  }""".formatted(stopPlace.getNetexId());

        executeGraphqQLQueryOnly(graphQlJsonQuery)
                .body("data.stopPlace", hasSize(1))
                .rootPath("data.stopPlace[0]")
                .body("id", equalTo(stopPlace.getNetexId()))
                .body("name.value", equalTo(stopPlace.getName().getValue()))
                .body("infoSpots", hasSize(1))
                .appendRootPath("infoSpots[0]")
                .body("id", equalTo(infoSpot.getNetexId()))
                .body("label", equalTo(infoSpot.getLabel()))
                .body("floor", equalTo(infoSpot.getFloor()));
    }

    @Test
    public void listInfoSpotsForQuayTest() {
        StopPlace stopPlace = new StopPlace();
        stopPlace.setCentroid(geometryFactory.createPoint(new Coordinate(5, 60)));
        stopPlace.setName(new EmbeddableMultilingualString("info stop place"));
        stopPlace.setValidBetween(new ValidBetween(Instant.now()));

        Quay quay = new Quay();
        quay.setName(new EmbeddableMultilingualString("info quay"));

        stopPlace.setQuays(Set.of(quay));
        stopPlace = stopPlaceRepository.save(stopPlace);

        var infoSpot = new InfoSpot();
        infoSpot.setVersion(1);
        infoSpot.setLabel("I1111");
        infoSpot.setInfoSpotType(InfoSpotTypeEnumeration.STATIC);
        infoSpot.setBacklight(true);
        infoSpot.setFloor("2");
        infoSpot.setDescription(new EmbeddableMultilingualString("Descriptive"));
        infoSpot.setMaintenance("Maintainer");
        infoSpot.setWidth(SIZE_80_120.width());
        infoSpot.setHeight(SIZE_80_120.height());
        infoSpot.setPurpose("Purpose of info");
        infoSpot.setRailInformation("Rail 1");
        infoSpot.setZoneLabel("A");
        infoSpot.setInfoSpotLocations(Set.of(quay.getNetexId()));
        infoSpot.setCentroid(stopPlace.getCentroid());

        infoSpot = infoSpotRepository.save(infoSpot);

        String graphQlJsonQuery = """
                  {
                  stopPlace: stopPlace (query:"%s") {
                    ... on StopPlace {
                      id
                      name { value }
                      quays {
                        id
                        infoSpots {
                            id
                            label
                            floor
                          }
                        }
                      }
                    }
                  }""".formatted(quay.getNetexId());

        executeGraphqQLQueryOnly(graphQlJsonQuery)
                .body("data.stopPlace", hasSize(1))
                .rootPath("data.stopPlace[0]")
                .body("id", equalTo(stopPlace.getNetexId()))
                .body("name.value", equalTo(stopPlace.getName().getValue()))
                .body("quays", hasSize(1))
                .appendRootPath("quays[0]")
                .body("id", equalTo(quay.getNetexId()))
                .body("infoSpots", hasSize(1))
                .appendRootPath("infoSpots[0]")
                .body("id", equalTo(infoSpot.getNetexId()))
                .body("label", equalTo(infoSpot.getLabel()))
                .body("floor", equalTo(infoSpot.getFloor()));
    }

    @Test
    public void updatePosterWithoutChangingReferences() {
        insertInfoSpots();

        String newLines = "789";

        String graphQlJsonMutation = """
                mutation {
                    infoSpot: mutateInfoSpots(
                        infoSpot: {
                            id: "%s"
                            poster: [
                                {
                                    label: "%s",
                                    width: %s,
                                    height: %s,
                                    lines: "%s"
                                }
                            ]
                        }
                    ) {
                        id
                        poster {
                            version
                            label
                            width
                            height
                            lines
                        }
                    }
                }
                """.formatted(
                updatedInfoSpot.getNetexId(),
                updatedPoster.getLabel(),
                updatedPoster.getWidth(),
                updatedPoster.getHeight(),
                newLines
        );

        executeGraphqQLQueryOnly(graphQlJsonMutation)
                .rootPath("data.infoSpot[0]")
                .body("id", equalTo(updatedInfoSpot.getNetexId()))
                .body("poster", hasSize(1))
                .appendRootPath("poster[0]")
                .body("version", equalTo(Long.toString(updatedPoster.getVersion() + 1)))
                .body("label", equalTo(updatedPoster.getLabel()))
                .body("width", equalTo(updatedPoster.getWidth()))
                .body("height", equalTo(updatedPoster.getHeight()))
                .body("lines", equalTo(newLines));
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
        oldInfoSpot.setDescription(new EmbeddableMultilingualString("Descriptive"));
        oldInfoSpot.setMaintenance("Maintainer");
        oldInfoSpot.setWidth(SIZE_80_120.width());
        oldInfoSpot.setHeight(SIZE_80_120.height());
        oldInfoSpot.setPurpose("Purpose of info");
        oldInfoSpot.setRailInformation("Rail 1");
        oldInfoSpot.setZoneLabel("A");
        oldInfoSpot.setSpeechProperty(true);
        oldInfoSpot.setInfoSpotLocations(Set.of(oldLocation1, oldLocation2));
        oldInfoSpot.setCentroid(geometryFactory.createPoint(new Coordinate(60.00, 35.00)));

        oldPoster = new InfoSpotPoster();
        oldPoster.setNetexId("NSR:InfoSpotPoster:1");
        oldPoster.setVersion(1);
        oldPoster.setLabel("Informative poster");
        oldPoster.setWidth(SIZE_A4.width());
        oldPoster.setHeight(SIZE_A4.height());
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
        updatedInfoSpot.setDescription(new EmbeddableMultilingualString("New Description"));
        updatedInfoSpot.setMaintenance("New Maintainer");
        updatedInfoSpot.setWidth(SIZE_A3.width());
        updatedInfoSpot.setHeight(SIZE_A3.height());
        updatedInfoSpot.setPurpose("New purpose of info");
        updatedInfoSpot.setRailInformation("Rail 2");
        updatedInfoSpot.setZoneLabel("B");
        updatedInfoSpot.setSpeechProperty(false);
        updatedInfoSpot.setInfoSpotLocations(Set.of(location1, location2));
        updatedInfoSpot.setCentroid(geometryFactory.createPoint(new Coordinate(65.00, 40.00)));

        updatedPoster = new InfoSpotPoster();
        updatedPoster.setNetexId("NSR:InfoSpotPoster:1");
        updatedPoster.setVersion(2);
        updatedPoster.setLabel("More informative poster");
        updatedPoster.setWidth(SIZE_A3.width());
        updatedPoster.setHeight(SIZE_A3.height());
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
        otherInfoSpot.setDescription(new EmbeddableMultilingualString("Other description"));
        otherInfoSpot.setMaintenance("Maintainer three");
        otherInfoSpot.setWidth(SIZE_A4.width());
        otherInfoSpot.setHeight(SIZE_A4.height());
        otherInfoSpot.setPurpose("Other purpose");
        otherInfoSpot.setRailInformation("Rail 3");
        otherInfoSpot.setZoneLabel("C");
        otherInfoSpot.setSpeechProperty(false);
        otherInfoSpot.setInfoSpotLocations(Set.of(otherLocation1, otherLocation2));
        otherInfoSpot.setCentroid(geometryFactory.createPoint(new Coordinate(55.00, 30.00)));

        otherPoster = new InfoSpotPoster();
        otherPoster.setNetexId("NSR:InfoSpotPoster:2");
        otherPoster.setVersion(1);
        otherPoster.setLabel("Other poster");
        otherPoster.setWidth(SIZE_A4.width());
        otherPoster.setHeight(SIZE_A4.height());
        otherPoster.setLines("7");

        infoSpotPosterRepository.save(otherPoster);

        InfoSpotPosterRef otherPosterRef = new InfoSpotPosterRef(otherPoster);

        otherInfoSpot.setPosters(Set.of(otherPosterRef));

        infoSpotRepository.save(otherInfoSpot);
    }

    private record Size(Integer width, Integer height) {}
}

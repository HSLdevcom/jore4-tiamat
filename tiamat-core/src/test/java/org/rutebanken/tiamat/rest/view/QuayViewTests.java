package org.rutebanken.tiamat.rest.view;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.TupleTransformer;
import org.junit.Before;
import org.junit.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.model.hsl.HslAccessibilityProperties;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.shaded.org.hamcrest.Matchers;

import java.time.Instant;
import java.util.*;

import static org.testcontainers.shaded.org.hamcrest.CoreMatchers.*;
import static org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat;

@Transactional
public class QuayViewTests extends TiamatIntegrationTest {
    @Autowired
    private QuayRepository quayRepository;

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private DataBuilder dataBuilder;

    @Before
    public void setup() {
        this.dataBuilder = new DataBuilder(quayRepository, stopPlaceRepository, entityManager);
    }

    @Test
    public void viewShowsData_withQuaySaved() throws Exception {
        DataBuilder.TestData testData = this.dataBuilder.withDefaultQuay().asPersisted();
        verifyTestData(testData);

        List<Map<String, Object>> quayNewestVersions = queryAll();

        assertThat(quayNewestVersions.getFirst(), notNullValue());
    }

    @Test
    public void viewShowsData_withInstalledEquipment() throws Exception {
        DataBuilder.TestData testData = this.dataBuilder
                .withDefaultQuay()
                .withDefaultInstalledEquipment()
                .asPersisted();
        verifyTestData(testData);
        assertThat(countPlaceEquipment(), Matchers.is(2L));
        List<Map<String, Object>> placeEquipment = query("SELECT * FROM place_equipment");

        String netexId = placeEquipment.getFirst().get("netex_id").toString();
        assertThat(netexId, equalTo(testData.installedEquipments.getFirst().getNetexId()));
    }

    @Test
    public void viewShowsData_withAlternativeName() throws Exception {
        DataBuilder.TestData testData = this.dataBuilder
                .withDefaultQuay()
                .withDefaultAlternativeName()
                .asPersisted();
        verifyTestData(testData);

        List<Map<String, Object>> quayNewestVersions = queryAll();
        String location_swe = quayNewestVersions.getFirst().get("location_swe").toString();
        assertThat(location_swe, is(testData.alternativeName.getName().getValue()));
    }


    @Test
    public void viewShowsData_withStreetAddress() throws Exception {
        DataBuilder.TestData testData = this.dataBuilder
                .withDefaultQuay()
                .withDefaultStreetAddress()
                .asPersisted();
        verifyTestData(testData);

        List<Map<String, Object>> quayNewestVersions = queryAll();
        String streetAddress = quayNewestVersions.getFirst().get("street_address").toString();
        assertThat(streetAddress, equalTo(testData.streetAddress.getItems().stream().findFirst().orElse("Failed")));
    }

    @Test
    public void viewShowsData_withPriority() throws Exception {
        DataBuilder.TestData testData = this.dataBuilder
                .withDefaultQuay()
                .withDefaultPriority()
                .asPersisted();
        verifyTestData(testData);

        List<Map<String, Object>> quayNewestVersions = queryAll();
        String priority = quayNewestVersions.getFirst().get("priority").toString();
        assertThat(priority, equalTo(testData.priority.getItems().stream().findFirst().orElse("Failed")));
    }

    @Test
    public void viewShowsData_withValidityStart() throws Exception {
        DataBuilder.TestData testData = this.dataBuilder
                .withDefaultQuay()
                .withDefaultValidityStart()
                .asPersisted();
        verifyTestData(testData);

        List<Map<String, Object>> quayNewestVersions = queryAll();
        String validityStart = quayNewestVersions.getFirst().get("validity_start").toString();
        assertThat(validityStart, equalTo(testData.validityStart.getItems().stream().findFirst().orElse("Failed")));
    }

    @Test
    public void viewShowsData_withValidityEnd() throws Exception {
        DataBuilder.TestData testData = this.dataBuilder
                .withDefaultQuay()
                .withDefaultValidityEnd()
                .asPersisted();
        verifyTestData(testData);

        List<Map<String, Object>> quayNewestVersions = queryAll();
        String validityEnd = quayNewestVersions.getFirst().get("validity_end").toString();
        assertThat(validityEnd, equalTo(testData.validityEnd.getItems().stream().findFirst().orElse("Failed")));
    }

    @Test
    public void viewShowsData_withELYCode() throws Exception {
        DataBuilder.TestData testData = this.dataBuilder
                .withDefaultQuay()
                .withDefaultELYCode()
                .asPersisted();
        verifyTestData(testData);

        List<Map<String, Object>> quayNewestVersions = queryAll();
        String ELYCode = quayNewestVersions.getFirst().get("ely_code").toString();
        assertThat(ELYCode, equalTo(testData.ELYCode.getItems().stream().findFirst().orElse("Failed")));
    }

    @Test
    public void viewShowsData_withPostalCode() throws Exception {
        DataBuilder.TestData testData = this.dataBuilder
                .withDefaultQuay()
                .withDefaultPostalCode()
                .asPersisted();
        verifyTestData(testData);

        List<Map<String, Object>> quayNewestVersions = queryAll();
        String postalCode = quayNewestVersions.getFirst().get("postal_code").toString();
        assertThat(postalCode, equalTo(testData.postalCode.getItems().stream().findFirst().orElse("Failed")));
    }

    @Test
    public void viewShowsData_withFunctionalArea() throws Exception {
        DataBuilder.TestData testData = this.dataBuilder
                .withDefaultQuay()
                .withDefaultFunctionalArea()
                .asPersisted();
        verifyTestData(testData);

        List<Map<String, Object>> quayNewestVersions = queryAll();
        Object functionalArea = quayNewestVersions.getFirst().get("functional_area");
        assertThat(functionalArea, instanceOf(Double.class));

        final var expectedValue = testData
                .functionalArea
                .getItems()
                .stream()
                .findFirst()
                .map(Double::valueOf)
                .orElseThrow();
        assertThat(functionalArea, equalTo(expectedValue));
    }


    private static List<String> toStringList(Object[] objects) {
        return Arrays.stream(objects).map(String::valueOf).toList();
    }

    private List<Map<String, Object>> queryAll() throws Exception {
        return query("SELECT * FROM quay_newest_version");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> query(String sql) throws Exception {
        return entityManager.createNativeQuery(sql)
                .unwrap(NativeQuery.class)
                .setTupleTransformer(new DynamicMappingTransformer())
                .getResultList()
                .stream()
                .filter(row -> row != null) // Exclude null rows
                .toList();
    }


    private void verifyTestData(DataBuilder.TestData testData) {
        assertThat(countQuay(), Matchers.is(1L));
        assertThat(countStopPlace(), Matchers.is(1L));
        assertThat(count(), Matchers.is(1L));
        List<Quay> quay = quayRepository.findByNetexId(testData.quay.getNetexId());
        assertThat(quay.getFirst().getNetexId(), equalTo(testData.quay.getNetexId()));
    }

    private Long count() {
        return (Long) entityManager.createNativeQuery("SELECT count(*) FROM jore_quay_extensions")
                .getResultList().getFirst();
    }

    private Long countPlaceEquipment() {
        return (Long) entityManager.createNativeQuery("SELECT count(*) FROM place_equipment")
                .getResultList().getFirst();
    }

    private Long countQuay() {
        return (Long) entityManager.createNativeQuery("SELECT count(*) FROM quay")
                .getResultList().getFirst();
    }

    private Long countStopPlace() {
        return (Long) entityManager.createNativeQuery("SELECT count(*) FROM stop_place")
                .getResultList().getFirst();
    }

    public static class DynamicMappingTransformer implements TupleTransformer<Map<String, Object>> {

        @Override
        public Map<String, Object> transformTuple(Object[] tuple, String[] aliases) {
            Map<String, Object> result = new HashMap<>();

            for (int i = 0; i < aliases.length; i++) {
                String alias = aliases[i];
                Object value = tuple[i];

                if (value != null && !(value instanceof String && ((String) value).isEmpty())) {
                    addNestedValue(result, alias, value);
                }
            }

            return result.isEmpty() ? null : result;
        }

        @SuppressWarnings("unchecked")
        private void addNestedValue(Map<String, Object> map, String alias, Object value) {
            String[] keys = alias.split("\\.");
            Map<String, Object> current = map;

            for (int i = 0; i < keys.length - 1; i++) {
                String key = keys[i];
                current = (Map<String, Object>) current.computeIfAbsent(key, k -> new HashMap<>());
            }

            current.put(keys[keys.length - 1], value);
        }
    }


    private static class DataBuilder {

        private final StopPlaceRepository stopPlaceRepository;
        private final QuayRepository quayRepository;
        private final EntityManager entityManager;

        private final TestData testData;

        DataBuilder(QuayRepository quayRepository, StopPlaceRepository stopPlaceRepository, EntityManager entityManager) {
            this.quayRepository = quayRepository;
            this.stopPlaceRepository = stopPlaceRepository;
            this.entityManager = entityManager;

            this.testData = new TestData();
        }


        public DataBuilder withDefaultQuay() {
            Quay quay = new Quay();
            quay.setVersion(1L);
            quay.setNetexId("HSL:Quay:" + getNetexId());
            quay.setCreated(Instant.parse("2010-04-17T09:30:47Z"));
            quay.setDataSourceRef("test:dataSourceRef");
            quay.setResponsibilitySetRef("test:responsibilityRef");

            quay.setName(new EmbeddableMultilingualString("Test stop", "en"));
            quay.setShortName(new EmbeddableMultilingualString("Test short name", "en"));
            quay.setDescription(new EmbeddableMultilingualString("TestDescription", "en"));

            quay.setCovered(CoveredEnumeration.COVERED);
            quay.setLabel(new EmbeddableMultilingualString("Test label", "en"));
            quay.getKeyValues().putAll(Map.of(
                    "stopState", new Value("InOperation"),
                    "validityStart", new Value("1990-01-01"),
                    "priority", new Value("10")
            ));

            this.testData.quay = quay;

            withDefaultStopPlace();
            return this;
        }


        private DataBuilder withDefaultStopPlace() {
            StopPlace stopPlace = new StopPlace();
            stopPlace.setNetexId(this.testData.quay.getNetexId());
            stopPlace.setVersion(1);
            stopPlace.setPublicCode("Test public code");
            stopPlace.setTransportMode(VehicleModeEnumeration.BUS);
            this.testData.stopPlace = stopPlace;
            return this;
        }

        public DataBuilder withDefaultInstalledEquipment() {
            PlaceEquipment placeEquipment = new PlaceEquipment();
            placeEquipment.setNetexId("test:pe-netex-id:" + getNetexId());
            placeEquipment.setVersion(1L);

            TicketingEquipment ticketingEquipment = new TicketingEquipment();
            ticketingEquipment.setNetexId("test:te-netex-id:" + getNetexId());
            ticketingEquipment.setVersion(1L);

            TicketingEquipment ticketingEquipment1 = new TicketingEquipment();
            ticketingEquipment1.setNetexId("test:te-netex-id:" + getNetexId());
            ticketingEquipment1.setVersion(1L);

            this.testData.installedEquipments.add(ticketingEquipment);
            this.testData.installedEquipments.add(ticketingEquipment1);
            this.testData.placeEquipments = placeEquipment;
            return this;
        }

        public DataBuilder withDefaultAccessibilityAssignment() {
            HslAccessibilityProperties hap = new HslAccessibilityProperties();
            hap.setNetexId(testData.quay.getNetexId());
            AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
            accessibilityAssessment.setHslAccessibilityProperties(hap);
            testData.quay.setAccessibilityAssessment(accessibilityAssessment);
            return this;
        }

        public DataBuilder withDefaultAlternativeName() {
            AlternativeName alternativeName = new AlternativeName();
            alternativeName.setNameType(NameTypeEnumeration.OTHER);
            alternativeName.setLang("swe");
            EmbeddableMultilingualString multilingualString = new EmbeddableMultilingualString();
            multilingualString.setLang("swe");
            multilingualString.setValue("quay-test-alternative-name");
            alternativeName.setName(multilingualString);
            testData.alternativeName = alternativeName;
            return this;
        }


        public DataBuilder withDefaultStreetAddress() {
            testData.streetAddress = new Value("Test street 1");
            return this;
        }

        public DataBuilder withDefaultPriority() {
            testData.priority = new Value("20");
            return this;
        }

        public DataBuilder withDefaultValidityStart() {
            testData.validityStart = new Value("2000-01-01");
            return this;
        }

        public DataBuilder withDefaultValidityEnd() {
            testData.validityEnd = new Value("2000-12-30");
            return this;
        }

        public DataBuilder withDefaultELYCode() {
            testData.ELYCode = new Value("Test ELY 1");
            return this;
        }

        public DataBuilder withDefaultPostalCode() {
            testData.postalCode = new Value("Test postal code 1");
            return this;
        }

        public DataBuilder withDefaultFunctionalArea() {
            testData.functionalArea = new Value("123.0");
            return this;
        }

        private void compileFinalData() {
            if (testData.alternativeName != null) {
                testData.quay.getAlternativeNames().add(testData.alternativeName);
            }

            if (testData.placeEquipments != null && testData.installedEquipments != null) {
                testData.placeEquipments.getInstalledEquipment().addAll(testData.installedEquipments);
                testData.quay.setPlaceEquipments(testData.placeEquipments);

            }

            if (testData.streetAddress != null) {
                testData.quay.getKeyValues().put("streetAddress", testData.streetAddress);
            }

            if (testData.priority != null) {
                testData.quay.getKeyValues().put("priority", testData.priority);
            }

            if (testData.validityStart != null) {
                testData.quay.getKeyValues().put("validityStart", testData.validityStart);
            }

            if (testData.validityEnd != null) {
                testData.quay.getKeyValues().put("validityEnd", testData.validityEnd);
            }

            if (testData.ELYCode != null) {
                testData.quay.getKeyValues().put("elyNumber", testData.ELYCode);
            }

            if (testData.postalCode != null) {
                testData.quay.getKeyValues().put("postalCode", testData.postalCode);
            }

            if (testData.functionalArea != null) {
                testData.quay.getKeyValues().put("functionalArea", testData.functionalArea);
            }
        }

        public TestData asPersisted() {
            this.compileFinalData();

            Quay quay = quayRepository.save(testData.quay);
            testData.stopPlace.getQuays().add(quay);
            stopPlaceRepository.save(testData.stopPlace);

            // Normally when saving a Quay in Jore, the jore_quay_extensins
            // table gets updated at the end of the saving transactions.
            // But here in the test's we continue with the transaction and
            // run the fetch-saved-data queries in the same transaction.
            // Thus, when we do the data-fetch, the extensions table has not yet
            // been updated and the tests fail.
            // So, now the data has been saved by previous command,
            // and we need flush and trigger the constraint manually.
            entityManager
                    .createNativeQuery("SET CONSTRAINTS update_jore_extensions_on_quay_save IMMEDIATE;")
                    .executeUpdate();

            return testData;
        }

        private int runningId = 0;

        private int getNetexId() {
            return runningId++;
        }

        private static class TestData {
            public Quay quay;
            public AlternativeName alternativeName;
            public List<InstalledEquipment_VersionStructure> installedEquipments = new ArrayList<>();
            public PlaceEquipment placeEquipments;

            public Value streetAddress;
            public Value priority;
            public Value validityStart;
            public Value validityEnd;
            public Value ELYCode;
            public Value postalCode;
            public Value functionalArea;
            public StopPlace stopPlace;
        }
    }
}

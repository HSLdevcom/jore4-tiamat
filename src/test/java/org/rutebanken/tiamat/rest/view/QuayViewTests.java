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
import org.testcontainers.shaded.org.hamcrest.CoreMatchers;
import org.testcontainers.shaded.org.hamcrest.Matchers;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        this.dataBuilder = new DataBuilder(quayRepository, stopPlaceRepository);
    }


    @Test
    public void viewShowsData_withQuaySaved() throws Exception {
        DataBuilder.TestData testData = this.dataBuilder.withDefaultQuay().asPersisted();
        verifyTestData(testData);

        List<Map<String, Object>> quayNewestVersions = queryAll();

        assertThat(quayNewestVersions.getFirst(), CoreMatchers.notNullValue());
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
        assertThat(streetAddress, CoreMatchers.equalTo(testData.streetAddress.getItems().stream().findFirst().orElse("Failed")));
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
        assertThat(priority, CoreMatchers.equalTo(testData.priority.getItems().stream().findFirst().orElse("Failed")));
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
        assertThat(validityStart, CoreMatchers.equalTo(testData.validityStart.getItems().stream().findFirst().orElse("Failed")));
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
        assertThat(validityEnd, CoreMatchers.equalTo(testData.validityEnd.getItems().stream().findFirst().orElse("Failed")));
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
        assertThat(ELYCode, CoreMatchers.equalTo(testData.ELYCode.getItems().stream().findFirst().orElse("Failed")));
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
        assertThat(postalCode, CoreMatchers.equalTo(testData.postalCode.getItems().stream().findFirst().orElse("Failed")));
    }

    @Test
    public void viewShowsData_withFunctionalArea() throws Exception {
        DataBuilder.TestData testData = this.dataBuilder
                .withDefaultQuay()
                .withDefaultFunctionalArea()
                .asPersisted();
        verifyTestData(testData);

        List<Map<String, Object>> quayNewestVersions = queryAll();
        String functionalArea = quayNewestVersions.getFirst().get("functional_area").toString();
        assertThat(functionalArea, CoreMatchers.equalTo(testData.functionalArea.getItems().stream().findFirst().orElse("Failed")));
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
        List<Quay> quay = quayRepository.findByNetexId(testData.quay.getNetexId());
        assertThat(count(), Matchers.is(1L));
        assertThat(countQuay(), Matchers.is(1L));
        assertThat(countStopPlace(), Matchers.is(1L));
        assertThat(quay.getFirst().getNetexId(), CoreMatchers.equalTo(testData.quay.getNetexId()));
    }

    private Long count() {
        return (Long) entityManager.createNativeQuery("SELECT count(*) FROM quay_newest_version")
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

        private final TestData testData;

        DataBuilder(QuayRepository quayRepository, StopPlaceRepository stopPlaceRepository) {
            this.testData = new TestData();
            this.quayRepository = quayRepository;
            this.stopPlaceRepository = stopPlaceRepository;
        }


        public DataBuilder withDefaultQuay() {
            Quay quay = new Quay();
            quay.setVersion(1L);
            quay.setNetexId("test:quay-netex-id:" + getNetexId());
            quay.setCreated(Instant.parse("2010-04-17T09:30:47Z"));
            quay.setDataSourceRef("test:dataSourceRef");
            quay.setResponsibilitySetRef("test:responsibilityRef");

            quay.setName(new EmbeddableMultilingualString("Test stop", "en"));
            quay.setShortName(new EmbeddableMultilingualString("Test short name", "en"));
            quay.setDescription(new EmbeddableMultilingualString("TestDescription", "en"));

            quay.setCovered(CoveredEnumeration.COVERED);
            quay.setLabel(new EmbeddableMultilingualString("Test label", "en"));
            this.testData.quay = quay;

            withDefaultStopPlace();
            return this;
        }


        private DataBuilder withDefaultStopPlace() {
            StopPlace stopPlace = new StopPlace();
            stopPlace.setNetexId(this.testData.quay.getNetexId());
            stopPlace.setPublicCode("Test public code");
            this.testData.stopPlace = stopPlace;
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
            EmbeddableMultilingualString multilingualString = new EmbeddableMultilingualString();
            multilingualString.setLang("fi");
            multilingualString.setValue("quay-test-alternative");
            alternativeName.setName(multilingualString);
            testData.alternativeName = alternativeName;
            return this;
        }


        public DataBuilder withDefaultStreetAddress() {
            Value streetAddress = new Value();
            streetAddress.getItems().add("Test street 1");
            testData.streetAddress = streetAddress;
            return this;
        }

        public DataBuilder withDefaultPriority() {
            Value priority = new Value();
            priority.getItems().add("Testpriority 1");
            testData.priority = priority;
            return this;
        }

        public DataBuilder withDefaultValidityStart() {
            Value validityStart = new Value();
            validityStart.getItems().add("Teststart 1");
            testData.validityStart = validityStart;
            return this;
        }

        public DataBuilder withDefaultValidityEnd() {
            Value validityEnd = new Value();
            validityEnd.getItems().add("Testend 1");
            testData.validityEnd = validityEnd;
            return this;
        }

        public DataBuilder withDefaultELYCode() {
            Value ELYcode = new Value();
            ELYcode.getItems().add("Test ELY 1");
            testData.ELYCode = ELYcode;
            return this;
        }

        public DataBuilder withDefaultPostalCode() {
            Value postalCode = new Value();
            postalCode.getItems().add("Test postal code 1");
            testData.postalCode = postalCode;
            return this;
        }

        public DataBuilder withDefaultFunctionalArea() {
            Value functionalArea = new Value();
            functionalArea.getItems().add("Test functional area 1");
            testData.functionalArea = functionalArea;
            return this;
        }


        public TestData asPersisted() {
            if (testData.alternativeName != null) {
                testData.quay.getAlternativeNames().add(testData.alternativeName);
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
                testData.quay.getKeyValues().put("ELYCode", testData.ELYCode);
            }

            if (testData.postalCode != null) {
                testData.quay.getKeyValues().put("postalCode", testData.postalCode);
            }

            if (testData.functionalArea != null) {
                testData.quay.getKeyValues().put("functionalArea", testData.functionalArea);
            }

            Quay quay = quayRepository.save(testData.quay);
            testData.stopPlace.getQuays().add(quay);
            stopPlaceRepository.save(testData.stopPlace);

            return testData;
        }

        private int runningId = 0;

        private int getNetexId() {
            return runningId++;
        }

        private static class TestData {
            public Quay quay;
            public AlternativeName alternativeName;
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

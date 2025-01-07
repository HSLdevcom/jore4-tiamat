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

    @PersistenceContext
    private EntityManager entityManager;

    private DataBuilder dataBuilder;

    @Before
    public void setup() {
        this.dataBuilder = new DataBuilder(quayRepository);
    }


    @Test
    public void viewShowsData_withQuaySaved() throws Exception {
        DataBuilder.TestData testData = this.dataBuilder.withDefaultQuay().asPersisted();
        verifyTestData(testData);

        List<Map<String, Object>> quayNewestVersions = queryAll();

        assertThat(quayNewestVersions.getFirst(), CoreMatchers.notNullValue());
    }

    @Test
    public void viewShowsData_withAccessibilityPropertiesSaved() throws Exception {
        DataBuilder.TestData testData = this.dataBuilder
                .withDefaultQuay()
                .withDefaultAccessibilityAssignment()
                .asPersisted();
        verifyTestData(testData);

        List<Map<String, Object>> quayNewestVersions = queryAll();

        List<String> accessibilityProperties = toStringList((Object[]) quayNewestVersions.getFirst().get("hsl_accessibility_properties"));
        assertThat(accessibilityProperties, CoreMatchers.hasItem(testData.quay.getNetexId()));
    }

    @Test
    public void viewShowsData_withAlternativeName() throws Exception {
        DataBuilder.TestData testData = this.dataBuilder
                .withDefaultQuay()
                .withDefaultAlternativeName()
                .asPersisted();

        verifyTestData(testData);

        List<Map<String, Object>> quayNewestVersions = queryAll();

        List<String> alternativeNames = toStringList((Object[]) quayNewestVersions.getFirst().get("alternative_names"));
        assertThat(alternativeNames, CoreMatchers.hasItem(testData.alternativeName.getId().toString()));
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

        QuayRepository quayRepository;

        private TestData testData;

        DataBuilder(QuayRepository quayRepository) {
            this.testData = new TestData();
            this.quayRepository = quayRepository;
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

            quayRepository.save(testData.quay);

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
        }
    }
}

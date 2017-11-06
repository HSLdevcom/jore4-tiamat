/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.repository.search;

import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.netex.id.NetexIdHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.MERGED_ID_KEY;
import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;
import static org.rutebanken.tiamat.repository.StopPlaceRepositoryImpl.*;

/**
 * Builds query from stop place search params
 */
@Component
public class StopPlaceQueryFromSearchBuilder extends SearchBuilder {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceQueryFromSearchBuilder.class);

    /**
     * If searching for single tag in the query parameter, prefixed by #
     */
    public static final String SQL_SINGLE_TAG_QUERY = "netex_id in (select t.netex_reference from tag t where t.name = :query and t.removed is null)";

    /**
     * If using a list of tags as stop place search argument
     */
    public static final String SQL_MULTIPLE_TAG_QUERY = "netex_id in (select t.netex_reference from tag t where t.name in :tags and t.removed is null)";

    /**
     * List only tagged stops, regardless of the tag name
     */
    public static final String SQL_WITH_TAGS =
            "(p.netex_id in (select t.netex_reference from tag t where t.removed is null)" +
                    "OR (s.netex_id in (select t.netex_reference from tag t where t.removed is null)))";


    public static final String SQL_DUPLICATED_QUAY_IMPORTED_IDS = "SELECT sp1.netex_id " +
            "FROM stop_place sp1 " +
            "  INNER JOIN stop_place_quays spq1 " +
            "    ON sp1.id = spq1.stop_place_id " +
            "  INNER JOIN quay_key_values qkv1 " +
            "    ON qkv1.quay_id = spq1.quays_id " +
            "      AND qkv1.key_values_key = :originalIdKey " +
            "  INNER JOIN value_items vi1 " +
            "    ON vi1.value_id = qkv1.key_values_id " +
            "  INNER JOIN quay q1 " +
            "    ON q1.id = qkv1.quay_id " +
            "WHERE sp1.from_date <= :pointInTime AND (sp1.to_date is NULL OR sp1.to_date > :pointInTime) " +
            "AND EXISTS (SELECT sp2.netex_id " +
            "  FROM stop_place sp2 " +
            "    INNER JOIN stop_place_quays spq2 " +
            "      ON sp2.id = spq2.stop_place_id " +
            "    INNER JOIN quay_key_values qkv2 " +
            "      ON qkv2.quay_id = spq2.quays_id " +
            "        AND qkv2.key_values_key = :originalIdKey " +
            "    INNER JOIN value_items vi2 " +
            "      ON vi2.value_id = qkv2.key_values_id " +
            "    INNER JOIN quay q2 " +
            "      ON q2.id = qkv2.quay_id " +
            "    WHERE " +
            "     (sp2.netex_id != sp1.netex_id " +
            "       OR (sp2.netex_id = sp1.netex_id AND sp2.version = sp1.version AND q2.netex_id != q1.netex_id)) " +
            "       AND vi2.items = vi1.items " +
            "       AND sp2.from_date <= :pointInTime AND (sp2.to_date is NULL OR sp2.to_date > :pointInTime) " +
            "    ) " +
            "AND vi1.items != '' " +
            "GROUP By sp1.netex_id";

    private static final double NEARBY_DECIMAL_DEGREES = 0.04;
    private static final double NEARBY_NAME_SIMILARITY = 0.6;

    /**
     * It is possible to search for nearby duplicates by meters, but it has a performance impact.
     * That's why decimal degrees is used.
     * This join is using pointInTime. It should actually support allVersion=true and pointInTime=null.
     */
    public static final String SQL_NEARBY =
            "SELECT nearby.id " +
                    "FROM stop_place nearby " +
                    createLeftJoinParentStopQuery("nearbyparent") +
                    "WHERE nearby.netex_id != s.netex_id " +
                    " AND nearby.parent_stop_place = false " +
                    " AND nearby.stop_place_type = s.stop_place_type " +
                    " AND ST_Distance(s.centroid, nearby.centroid) < :nearbyThreshold " +
                    " AND s.name_value = nearby.name_value ";
    // Together with distinct and nearby search, the next line slows everything down too much:
//                     "  AND similarity(s.name_value , s2.name_value) > :similarityThreshold ";


    public Pair<String, Map<String, Object>> buildQueryString(ExportParams exportParams) {

        StopPlaceSearch stopPlaceSearch = exportParams.getStopPlaceSearch();

        StringBuilder queryString = new StringBuilder("select s.* from stop_place s ");

        List<String> wheres = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        List<String> operators = new ArrayList<>();
        List<String> orderByStatements = new ArrayList<>();

        queryString.append(SQL_LEFT_JOIN_PARENT_STOP);

        boolean hasIdFilter = stopPlaceSearch.getNetexIdList() != null && !stopPlaceSearch.getNetexIdList().isEmpty();

        if (hasIdFilter) {
            wheres.add("(s.netex_id in :netexIdList OR p.netex_id in :netexIdList)");
            parameters.put("netexIdList", stopPlaceSearch.getNetexIdList());
        } else {
            if (stopPlaceSearch.getQuery() != null) {
                createAndAddQueryCondition(stopPlaceSearch, operators, parameters, wheres, orderByStatements);
            }

            if (stopPlaceSearch.getStopTypeEnumerations() != null && !stopPlaceSearch.getStopTypeEnumerations().isEmpty()) {
                wheres.add("s.stop_place_type in :stopPlaceTypes");
                parameters.put("stopPlaceTypes", stopPlaceSearch.getStopTypeEnumerations().stream().map(StopTypeEnumeration::toString).collect(toList()));
                operators.add("and");
            }

            if (stopPlaceSearch.getTags() != null && !stopPlaceSearch.getTags().isEmpty()) {
                wheres.add("(s." + SQL_MULTIPLE_TAG_QUERY + " OR p." + SQL_MULTIPLE_TAG_QUERY + ")");
                parameters.put("tags", stopPlaceSearch.getTags());
                operators.add("and");
            }

            boolean hasMunicipalityFilter = exportParams.getMunicipalityReferences() != null && !exportParams.getMunicipalityReferences().isEmpty();
            boolean hasCountyFilter = exportParams.getCountyReferences() != null && !exportParams.getCountyReferences().isEmpty();

            if (hasMunicipalityFilter && !hasIdFilter) {
                String prefix;
                if (hasCountyFilter) {
                    operators.add("or");
                    prefix = "(";
                } else prefix = "";

                String municipalityQuery = "topographic_place_id in (select tp.id from topographic_place tp where tp.netex_id in :municipalityId)";
                wheres.add(prefix + "(s." + municipalityQuery + " or p." + municipalityQuery + ")");
                parameters.put("municipalityId", exportParams.getMunicipalityReferences());
            }

            if (hasCountyFilter && !hasIdFilter) {
                String suffix = hasMunicipalityFilter ? ")" : "";
                String countyQuery = "topographic_place_id in (select tp.id from topographic_place tp where tp.parent_ref in :countyId)";
                wheres.add("(s." + countyQuery + " or " + "p." + countyQuery + ")" + suffix);
                parameters.put("countyId", exportParams.getCountyReferences());
            }
        }

        if (stopPlaceSearch.getVersion() != null) {
            operators.add("and");
            wheres.add("s.version = :version");
            parameters.put("version", stopPlaceSearch.getVersion());
        } else if (!stopPlaceSearch.isAllVersions()
                && stopPlaceSearch.getPointInTime() == null &&
                (stopPlaceSearch.getVersionValidity() == null || ExportParams.VersionValidity.ALL.equals(stopPlaceSearch.getVersionValidity()))) {
            operators.add("and");
            wheres.add("s.version = (select max(sv.version) from stop_place sv where sv.netex_id = s.netex_id)");
        }

        if (stopPlaceSearch.getPointInTime() != null) {
            operators.add("and");
            //(from- and toDate is NULL), or (fromDate is set and toDate IS NULL or set)
            String pointInTimeCondition = createPointInTimeCondition("s", "p");
            parameters.put("pointInTime", Timestamp.from(stopPlaceSearch.getPointInTime()));
            wheres.add(pointInTimeCondition);
        } else if (stopPlaceSearch.getVersionValidity() != null) {
            operators.add("and");

            if (ExportParams.VersionValidity.CURRENT.equals(stopPlaceSearch.getVersionValidity())) {
                parameters.put("pointInTime", Date.from(Instant.now()));
                String currentQuery = "(%s.from_date <= :pointInTime AND (%s.to_date >= :pointInTime or %s.to_date IS NULL))";
                wheres.add("(" + formatRepeatedValue(currentQuery, "s", 3) + " or " + formatRepeatedValue(currentQuery, "p", 3) + ")");
            } else if (ExportParams.VersionValidity.CURRENT_FUTURE.equals(stopPlaceSearch.getVersionValidity())) {
                parameters.put("pointInTime", Date.from(Instant.now()));
                String futureQuery = "p.netex_id is null and (s.to_date >= :pointInTime OR s.to_date IS NULL)";
                String parentFutureQuery = "p.netex_id is not null and (p.to_date >= :pointInTime OR p.to_date IS NULL)";
                wheres.add("((" + futureQuery + ") or (" + parentFutureQuery + "))");
            }
        }

        if (stopPlaceSearch.isWithoutLocationOnly()) {
            operators.add("and");
            wheres.add("(s.centroid IS NULL or (p.id IS NOT NULL AND p.centroid IS NULL))");
        }

        if (stopPlaceSearch.isWithoutQuaysOnly()) {
            operators.add("and");
            wheres.add("not exists (select sq.quays_id from stop_place_quays sq where sq.stop_place_id = s.id)");
        }

        if(stopPlaceSearch.isWithTags()) {
            operators.add("and");
            wheres.add(SQL_WITH_TAGS);
        }

        if (stopPlaceSearch.isWithDuplicatedQuayImportedIds()) {
            operators.add("and");
            if (stopPlaceSearch.getPointInTime() == null) {
                throw new IllegalArgumentException("pointInTime must be set when searching for duplicated quay imported IDs");
            }
            parameters.put("originalIdKey", ORIGINAL_ID_KEY);
            wheres.add("s.netex_id IN (" + SQL_DUPLICATED_QUAY_IMPORTED_IDS + ")");
        }

        if (stopPlaceSearch.isWithNearbySimilarDuplicates()) {
            createAndAddNearbyCondition(stopPlaceSearch, operators, wheres, parameters, orderByStatements);
        }

        operators.add("and");
        wheres.add(SQL_NOT_PARENT_STOP_PLACE);

        addWheres(queryString, wheres, operators);

        orderByStatements.add("s.netex_id, s.version asc");
        queryString.append(" order by");

        for (int i = 0; i < orderByStatements.size(); i++) {
            if (i > 0) {
                queryString.append(',');
            }
            queryString.append(' ').append(orderByStatements.get(i)).append(' ');
        }

        final String generatedSql = basicFormatter.format(queryString.toString());

        logger.info("sql: {}\nparams: {}\nSearch object: {}", generatedSql, parameters.toString(), stopPlaceSearch);

        return Pair.of(generatedSql, parameters);
    }

    private void createAndAddQueryCondition(StopPlaceSearch stopPlaceSearch, List<String> operators, Map<String, Object> parameters, List<String> wheres, List<String> orderByStatements) {
        operators.add("and");

        if (stopPlaceSearch.getQuery().startsWith("#") && !stopPlaceSearch.getQuery().contains(" ")) {
            // Seems like we are searching for tags
            String hashRemoved = stopPlaceSearch.getQuery().substring(1);
            parameters.put("query", hashRemoved);
            wheres.add("(s." + SQL_SINGLE_TAG_QUERY + " OR p." + SQL_SINGLE_TAG_QUERY + ")");

        } else if (NetexIdHelper.isNetexId(stopPlaceSearch.getQuery())) {
            String netexId = stopPlaceSearch.getQuery();

            String netexIdType = NetexIdHelper.extractIdType(netexId);
            parameters.put("query", stopPlaceSearch.getQuery());

            if (!NetexIdHelper.isNsrId(stopPlaceSearch.getQuery())) {

                // Detect non NSR NetexId and search in original ID
                parameters.put("originalIdKey", ORIGINAL_ID_KEY);
                parameters.put("mergedIdKey", MERGED_ID_KEY);

                if (StopPlace.class.getSimpleName().equals(netexIdType)) {
                    String keyValuesQuery = "id in (select spkv.stop_place_id from stop_place_key_values spkv inner join value_items v on spkv.key_values_id = v.value_id where (spkv.key_values_key = :originalIdKey OR spkv.key_values_key = :mergedIdKey) and v.items = :query)";
                    wheres.add("(s." + keyValuesQuery + " OR p." + keyValuesQuery + ")");
                } else if (Quay.class.getSimpleName().equals(netexIdType)) {
                    wheres.add("s.id in (select spq.stop_place_id from stop_place_quays spq inner join quay_key_values qkv on spq.quays_id = qkv.quay_id inner join value_items v on qkv.key_values_id = v.value_id where (qkv.key_values_key = :originalIdKey OR qkv.key_values_key = :mergedIdKey) and v.items = :query)");
                } else {
                    logger.warn("Detected NeTEx ID {}, but type is not supported: {}", netexId, NetexIdHelper.extractIdType(netexId));
                }
            } else {
                // NSR ID detected

                if (StopPlace.class.getSimpleName().equals(netexIdType)) {
                    wheres.add("(s.netex_id = :query or p.netex_id = :query)");
                } else if (Quay.class.getSimpleName().equals(netexIdType)) {
                    wheres.add("s.id in (select spq.stop_place_id from stop_place_quays spq inner join quay q on spq.quays_id = q.id and q.netex_id = :query)");
                } else {
                    logger.warn("Detected NeTEx ID {}, but type is not supported: {}", netexId, NetexIdHelper.extractIdType(netexId));
                }
            }
        } else {
            parameters.put("query", stopPlaceSearch.getQuery());

            final String startingWithLowerMatchQuerySql = "concat(lower(:query), '%') ";
            final String containsLowerMatchQuerySql = "concat('%', lower(:query), '%') ";
            final String orNameMatchInParentStopSql = "or lower(p.name_value) like ";
            if (stopPlaceSearch.getQuery().length() <= 3) {
                wheres.add("(lower(s.name_value) like " + startingWithLowerMatchQuerySql + orNameMatchInParentStopSql + startingWithLowerMatchQuerySql + ")");
            } else {
                wheres.add("(lower(s.name_value) like " + containsLowerMatchQuerySql + orNameMatchInParentStopSql + containsLowerMatchQuerySql + ")");
            }

            orderByStatements.add("similarity(concat(s.name_value, p.name_value), :query) desc");
        }
    }

    private String createPointInTimeCondition(String stopPlaceAlias, String parentStopPlaceAlias) {
        String pointInTimeQueryTemplate = "((%s.from_date IS NULL AND %s.to_date IS NULL) OR (%s.from_date <= :pointInTime AND (%s.to_date IS NULL OR %s.to_date > :pointInTime)))";

        return "((p.id IS NULL AND "
                + formatRepeatedValue(pointInTimeQueryTemplate, stopPlaceAlias, 5)
                + ") or (p.id IS NOT NULL AND "
                + formatRepeatedValue(pointInTimeQueryTemplate, parentStopPlaceAlias, 5) + "))";
    }

    private void createAndAddNearbyCondition(StopPlaceSearch stopPlaceSearch, List<String> operators, List<String> wheres, Map<String, Object> parameters, List<String> orderByStatements) {
        operators.add("and");

        String sqlNearby = "exists (" + SQL_NEARBY;

        if (stopPlaceSearch.getPointInTime() == null) {
            sqlNearby += "  AND nearby.version = (select max(s3.version) from stop_place s3 where s3.netex_id = nearby.netex_id) ";
        } else {
            sqlNearby += " AND " + createPointInTimeCondition("nearby", "nearbyparent");
            parameters.put("pointInTime", Timestamp.from(stopPlaceSearch.getPointInTime()));

        }
        parameters.put("nearbyThreshold", NEARBY_DECIMAL_DEGREES);

        // parameters.put("similarityThreshold", NEARBY_NAME_SIMILARITY);

        wheres.add(sqlNearby + ")");
        orderByStatements.add("s.name_value");
        orderByStatements.add("s.centroid");
    }

    private String formatRepeatedValue(String format, String value, int repeated) {
        Object[] args = new Object[repeated];
        for (int i = 0; i < repeated; i++) {
            args[i] = value;
        }
        return String.format(format, args);
    }
}

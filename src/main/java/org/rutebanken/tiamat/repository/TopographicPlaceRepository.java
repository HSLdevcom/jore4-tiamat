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

package org.rutebanken.tiamat.repository;

import jakarta.persistence.QueryHint;
import org.locationtech.jts.geom.Point;
import org.rutebanken.tiamat.exporter.params.TopographicPlaceSearch;
import org.rutebanken.tiamat.model.IanaCountryTldEnumeration;
import org.rutebanken.tiamat.model.TopographicPlace;
import org.rutebanken.tiamat.model.TopographicPlaceTypeEnumeration;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TopographicPlaceRepository extends EntityInVersionRepository<TopographicPlace>, TopographicPlaceRepositoryCustom {

    /**
     * Should only be one per name, country and placeType...
     */
    List<TopographicPlace> findByNameValueAndCountryRefRefAndTopographicPlaceType(String name, IanaCountryTldEnumeration ianCountryTld, TopographicPlaceTypeEnumeration placeType);

        @QueryHints(value = { @QueryHint(name = "org.hibernate.cacheable", value = "true")}, forCounting = false)
        @Query(value = "select tp.* from topographic_place tp " +
            "inner join persistable_polygon pp on tp.polygon_id = pp.id " +
            "where public.ST_Contains(pp.polygon, :#{#point}) = TRUE " +
            "and tp.version = (SELECT MAX(tpv.version) FROM topographic_place tpv WHERE tpv.netex_id = tp.netex_id)",
            nativeQuery = true)
    List<TopographicPlace> findByPoint(@Param("point") Point point);

    @QueryHints(value = { @QueryHint(name = "org.hibernate.cacheable", value = "true")}, forCounting = false)
    @Query("select tp from TopographicPlace tp WHERE tp.version = (SELECT MAX(tpv.version) FROM TopographicPlace tpv WHERE tpv.netexId = tp.netexId)")
    List<TopographicPlace> findAllMaxVersion();

    int countByTopographicPlaceTypeAndNetexIdStartingWith(TopographicPlaceTypeEnumeration topographicPlaceTypeEnumeration, String idPrefix);


    void deleteAllByTopographicPlaceTypeAndNetexIdStartingWith(TopographicPlaceTypeEnumeration topographicPlaceTypeEnumeration, String idPrefix);

    @Override
    List<TopographicPlace> findTopographicPlace(TopographicPlaceSearch topographicPlaceSearch);
}

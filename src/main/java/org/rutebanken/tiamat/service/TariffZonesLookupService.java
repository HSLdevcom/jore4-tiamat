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

package org.rutebanken.tiamat.service;


import com.google.common.collect.Sets;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.rutebanken.tiamat.general.ResettableMemoizer;
import org.rutebanken.tiamat.model.EntityInVersionStructure;
import org.rutebanken.tiamat.model.FareZone;
import org.rutebanken.tiamat.model.ScopingMethodEnumeration;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.TariffZone;
import org.rutebanken.tiamat.model.TariffZoneRef;
import org.rutebanken.tiamat.repository.FareZoneRepository;
import org.rutebanken.tiamat.repository.TariffZoneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.stream.Collectors.*;

@Service
@Transactional
public class TariffZonesLookupService {

    private static final Logger logger = LoggerFactory.getLogger(TariffZonesLookupService.class);

    private final ResettableMemoizer<List<Pair<String, Polygon>>> tariffZones = new ResettableMemoizer<>(getTariffZones());
    private final ResettableMemoizer<List<Pair<String, Polygon>>> fareZones = new ResettableMemoizer<>(getFareZones());

    private final TariffZoneRepository tariffZoneRepository;
    private final FareZoneRepository fareZoneRepository;

    private final boolean removeExistingReferences;

    @Autowired
    public TariffZonesLookupService(TariffZoneRepository tariffZoneRepository,
                                    FareZoneRepository fareZoneRepository,
                                    @Value("${tariffzoneLookupService.resetReferences:false}") boolean removeExistingReferences) {
        this.tariffZoneRepository = tariffZoneRepository;
        this.fareZoneRepository =fareZoneRepository;
        this.removeExistingReferences = removeExistingReferences;
    }

    public boolean populateTariffZone(StopPlace stopPlace) {
        if(stopPlace.getCentroid() != null) {

            if(stopPlace.getTariffZones() == null) {
                stopPlace.setTariffZones(new HashSet<>());
            }

            Set<String> refsBefore = mapToIdStrings(stopPlace.getTariffZones());

            if(removeExistingReferences) {
                stopPlace.getTariffZones().clear();
            }

            Set<TariffZoneRef> tariffZoneMatches = findTariffZones(stopPlace.getCentroid())
                    .stream()
                    .filter(tariffZone -> stopPlace.getTariffZones().isEmpty() || stopPlace.getTariffZones()
                            .stream()
                            .noneMatch(tariffZoneRef -> tariffZone.getNetexId().equals(tariffZoneRef.getRef()) && tariffZoneRef.getVersion().equals(String.valueOf(tariffZone.getVersion()))))
                    .map(TariffZoneRef::new)
                    .collect(toSet());

            Set<TariffZoneRef> allMatches = new HashSet<>(tariffZoneMatches);

            Set<TariffZoneRef> fareZoneMatches = findFareZones(stopPlace.getCentroid())
                    .stream()
                    .filter(fareZone -> stopPlace.getTariffZones().isEmpty() || isNoneMatch(stopPlace, fareZone))
                    .map(TariffZoneRef::new)
                    .collect(toSet());


            allMatches.addAll(fareZoneMatches);

            stopPlace.getTariffZones().addAll(allMatches);

            Set<String> refsAfter = mapToIdStrings(stopPlace.getTariffZones());

            return !Sets.symmetricDifference(refsBefore, refsAfter).isEmpty();
        }
        return false;
    }

    private boolean isNoneMatch(StopPlace stopPlace, FareZone fareZone) {
        if (fareZone.getScopingMethod().equals(ScopingMethodEnumeration.IMPLICIT_SPATIAL_PROJECTION)) {
            return stopPlace.getTariffZones()
                    .stream()
                    .noneMatch(tariffZoneRef -> fareZone.getNetexId().equals(tariffZoneRef.getRef()) && tariffZoneRef.getVersion().equals(String.valueOf(fareZone.getVersion())));
        }
        if (fareZone.getScopingMethod().equals(ScopingMethodEnumeration.EXPLICIT_STOPS) && !fareZone.getFareZoneMembers().isEmpty()) {
            return fareZone.getFareZoneMembers().stream()
                    .anyMatch(member -> member.getRef().equals(stopPlace.getNetexId()));
        }
        return true;

    }
    private Set<String> mapToIdStrings(Set<TariffZoneRef> tariffZoneRefs) {
        return tariffZoneRefs.stream().map(tzr -> tzr.getRef()).collect(toSet());
    }

    public List<TariffZone> findTariffZones(Point point) {
        return tariffZones.get()
                       .stream()
                       .filter(pair -> point.coveredBy(pair.getSecond()))
                       .map(pair -> tariffZoneRepository.findValidTariffZone(pair.getFirst()).orElse(null))
                       .filter(Objects::nonNull)
                       .collect(toList());
    }

    public List<FareZone> findFareZones(Point point) {
        return fareZones.get()
                .stream()
                .filter(pair -> point.coveredBy(pair.getSecond()))
                .map(pair -> fareZoneRepository.findValidFareZone(pair.getFirst()).orElse(null))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    public Supplier<List<Pair<String, Polygon>>> getTariffZones() {
        return () -> {
            logger.info("Fetching and memoizing tariff zones from repository");
            return tariffZoneRepository.findAll()
                    .stream()
                    .filter(tariffZone -> tariffZone.getPolygon() != null)
                    .collect(
                            groupingBy(TariffZone::getNetexId,
                                    maxBy(Comparator.comparingLong(EntityInVersionStructure::getVersion))))
                    .values()
                    .stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .peek(tariffZone -> logger.debug("Memoizing tariff zone {} {}", tariffZone.getNetexId(), tariffZone.getVersion()))
                    .map(tariffZone -> Pair.of(tariffZone.getNetexId(), tariffZone.getPolygon()))
                    .collect(toList());

        };
    }

    public Supplier<List<Pair<String, Polygon>>> getFareZones() {
        return () -> {
            logger.info("Fetching and memoizing fare zones from repository");
            return fareZoneRepository.findAll()
                    .stream()
                    .filter(fareZone -> fareZone.getPolygon() != null)
                    .collect(
                            groupingBy(FareZone::getNetexId,
                                    maxBy(Comparator.comparingLong(EntityInVersionStructure::getVersion))))
                    .values()
                    .stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .peek(fareZone -> logger.debug("Memoizing fare zone {} {}", fareZone.getNetexId(), fareZone.getVersion()))
                    .map(fareZone -> Pair.of(fareZone.getNetexId(), fareZone.getPolygon()))
                    .collect(toList());

        };
    }

    public void resetTariffZone() {
        tariffZones.reset();
    }

    public void resetFareZone() {
        fareZones.reset();
    }


}

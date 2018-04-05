package org.rutebanken.tiamat.service.batch;

import org.hibernate.Session;
import org.hibernate.internal.SessionImpl;
import org.rutebanken.tiamat.exporter.async.ParentStopFetchingIterator;
import org.rutebanken.tiamat.exporter.eviction.SessionEntitiesEvictor;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.rutebanken.tiamat.lock.TimeoutMaxLeaseTimeLock;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.service.TariffZonesLookupService;
import org.rutebanken.tiamat.service.TopographicPlaceLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.swing.text.html.Option;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for updating references from stop places to tariff zone and topographic place based on polygons
 */
@Transactional
@Service
public class StopPlaceRefUpdaterService {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceRefUpdaterService.class);
    public static final int FLUSH_EACH = 100;
    public static final int MAX_LEASE_TIME_SECONDS = 36000;
    public static final int WAIT_TIMEOUT_SECONDS = 1000;
    public static final String BACKGROUND_UPDATE_STOPS_LOCK = "background-update-stops-lock";

    private final StopPlaceRepository stopPlaceRepository;
    private final TariffZonesLookupService tariffZonesLookupService;
    private final TopographicPlaceLookupService topographicPlaceLookupService;
    private final EntityManager entityManager;
    private final TimeoutMaxLeaseTimeLock timeoutMaxLeaseTimeLock;

    @Autowired
    public StopPlaceRefUpdaterService(StopPlaceRepository stopPlaceRepository,
                                      TariffZonesLookupService tariffZonesLookupService,
                                      TopographicPlaceLookupService topographicPlaceLookupService,
                                      EntityManager entityManager,
                                      TimeoutMaxLeaseTimeLock timeoutMaxLeaseTimeLock) {
        this.stopPlaceRepository = stopPlaceRepository;
        this.tariffZonesLookupService = tariffZonesLookupService;
        this.topographicPlaceLookupService = topographicPlaceLookupService;
        this.entityManager = entityManager;
        this.timeoutMaxLeaseTimeLock = timeoutMaxLeaseTimeLock;
    }

    public void updateAllStopPlaces() {

        timeoutMaxLeaseTimeLock.executeInLock(() -> {
            long startTime = System.currentTimeMillis();

            Session session = entityManager.unwrap(Session.class);
            logger.info("About to update all currently valid stop places (tariff zone and topographic place refs)");

            SessionEntitiesEvictor sessionEntitiesEvictor = new SessionEntitiesEvictor((SessionImpl) session);

            ExportParams exportParams = ExportParams.newExportParamsBuilder()
                    .setStopPlaceSearch(
                            StopPlaceSearch.newStopPlaceSearchBuilder()
                                    .setVersionValidity(ExportParams.VersionValidity.CURRENT_FUTURE)
                                    .build())
                    .build();
            logger.info("Created export params search for scrolling stop places {}", exportParams);

            ParentStopFetchingIterator stopPlaceIterator = new ParentStopFetchingIterator(stopPlaceRepository.scrollStopPlaces(exportParams), stopPlaceRepository);

            AtomicInteger updatedBecauseOfTariffZoneRefChange = new AtomicInteger();
            AtomicInteger updatedBecauseOfTopographicPlaceChange = new AtomicInteger();
            AtomicInteger stopsSavedTotal = new AtomicInteger();
            AtomicInteger stopsCounter = new AtomicInteger();
            PerSecondLogger perSecondsLogger = new PerSecondLogger(startTime, stopsSavedTotal, "Progress while updating stop places references");

            while (stopPlaceIterator.hasNext()) {
                try {
                    stopsCounter.incrementAndGet();
                    Optional<StopPlace> optionalStopPlace = new StopPlaceRefUpdater(
                            tariffZonesLookupService,
                            topographicPlaceLookupService,
                            stopPlaceIterator.next(),
                            updatedBecauseOfTariffZoneRefChange,
                            updatedBecauseOfTopographicPlaceChange)
                            .call();

                    if (optionalStopPlace.isPresent()) {
                        stopsSavedTotal.incrementAndGet();
                        StopPlace stopPlace = optionalStopPlace.get();
                        stopPlace.setChanged(Instant.now());
                        stopPlaceRepository.save(stopPlace);
                        logger.trace("Saved stop {}", stopPlace);
                        perSecondsLogger.log();
                        session.flush();
                        if (stopsCounter.get() % FLUSH_EACH == 0 && !stopPlaceIterator.hasNextParent()) {
                            logger.trace("Flushing and clearing session at count {}", stopsCounter.get());
                            session.clear();
                        } else {
                            sessionEntitiesEvictor.evictKnownEntitiesFromSession(stopPlace);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            long timeSpent = System.currentTimeMillis() - startTime;

            logger.info("Updated {} stops in {} ms. Stops updated because of change in tariff zone refs: {}, " +
                            "stops updated because of change in topographic place ref: {}",
                    stopsSavedTotal, timeSpent, updatedBecauseOfTariffZoneRefChange, updatedBecauseOfTopographicPlaceChange);
            return Optional.empty();
        }, BACKGROUND_UPDATE_STOPS_LOCK, WAIT_TIMEOUT_SECONDS, MAX_LEASE_TIME_SECONDS);
    }
}
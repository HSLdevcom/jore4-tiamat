package org.rutebanken.tiamat.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Set;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.NotImplementedException;
import org.rutebanken.tiamat.model.InfoSpot;
import org.springframework.stereotype.Repository;

@Repository
@Transactional
public class InfoSpotRepositoryImpl implements InfoSpotRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public String findFirstByKeyValues(String key, Set<String> originalIds) {
        throw new NotImplementedException("findFirstByKeyValues not implemented for " + this.getClass().getSimpleName());
    }

    @Override
    public List<InfoSpot> findForAssociation(String netexId) {
        String sql = """
            select infoSpot.* from info_spot infoSpot
            inner join info_spot_location isl on isl.info_spot_id = infoSpot.id
            where
                isl.location_netex_id = :netexId
                and infoSpot.version = (
                    SELECT MAX(version)
                    FROM info_spot
                    WHERE netex_id = infoSpot.netex_id
                )
        """;

        return entityManager.createNativeQuery(sql, InfoSpot.class)
                .setParameter("netexId", netexId)
                .getResultList();
    }

    @Override
    public List<InfoSpot> findForAssociationWithVersion(String netexId, Long version) {
        // Use version-based deletion tracking. When an InfoSpot is deleted, a new version
        // is created with empty locationRefs and deletion tracking fields set.
        // We find the latest version WITH refs, then check if a newer version exists
        // that marks it as deleted for this entity at this version or earlier.
        String sql = """
            SELECT DISTINCT infoSpot.* 
            FROM info_spot infoSpot
            INNER JOIN info_spot_location isl ON isl.info_spot_id = infoSpot.id
            WHERE isl.location_netex_id = :netexId
              AND (isl.version IS NULL OR CAST(isl.version AS INTEGER) <= :version)
              AND infoSpot.version = (
                  SELECT MAX(spotv.version)
                  FROM info_spot spotv
                  INNER JOIN info_spot_location islv ON islv.info_spot_id = spotv.id
                  WHERE spotv.netex_id = infoSpot.netex_id
                    AND islv.location_netex_id = :netexId
                    AND (islv.version IS NULL OR CAST(islv.version AS INTEGER) <= :version)
              )
              AND NOT EXISTS (
                  SELECT 1 FROM info_spot deleted
                  WHERE deleted.netex_id = infoSpot.netex_id
                    AND deleted.version > infoSpot.version
                    AND deleted.deleted_at_entity_ref = :netexId
                    AND deleted.deleted_at_entity_version <= :version
              )
        """;

        return entityManager.createNativeQuery(sql, InfoSpot.class)
                .setParameter("netexId", netexId)
                .setParameter("version", version)
                .getResultList();
    }
}

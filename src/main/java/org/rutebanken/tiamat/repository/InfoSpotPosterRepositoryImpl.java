package org.rutebanken.tiamat.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
@Transactional
public class InfoSpotPosterRepositoryImpl implements InfoSpotPosterRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public String findFirstByKeyValues(String key, Set<String> originalIds) {

        Query query = entityManager.createNativeQuery("SELECT p.netex_id " +
                "FROM info_spot_poster p " +
                "INNER JOIN info_spot_poster_key_values pkv " +
                "ON pkv.info_spot_poster_id = p.id " +
                "INNER JOIN value_items v " +
                "ON pkv.key_values_id = v.value_id " +
                "WHERE pkv.key_values_key = :key " +
                "AND v.items IN ( :values ) " +
                "AND p.version = (SELECT MAX(pv.version) FROM parking pv WHERE pv.netex_id = p.netex_id)");

        query.setParameter("key", key);
        query.setParameter("values", originalIds);

        try {
            @SuppressWarnings("unchecked")
            List<String> results = query.getResultList();

            if (results.isEmpty()) {
                return null;
            }
            return results.getFirst();
        } catch (NoResultException noResultException) {
            return null;
        }
    }
}

package org.rutebanken.tiamat.repository;

import java.util.List;
import org.rutebanken.tiamat.model.InfoSpot;

public interface InfoSpotRepositoryCustom extends DataManagedObjectStructureRepository<InfoSpot> {

    List<InfoSpot> findForAssociation(String netexId);

    /**
     * Find InfoSpots associated with a specific version of an entity.
     * Only returns InfoSpots whose locationRefs match both the netexId AND version.
     */
    List<InfoSpot> findForAssociationWithVersion(String netexId, Long version);
}

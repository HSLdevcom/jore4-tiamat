package org.rutebanken.tiamat.auth;

import org.locationtech.jts.geom.Point;
import org.rutebanken.tiamat.diff.generic.SubmodeEnumuration;
import org.rutebanken.tiamat.model.EntityStructure;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collection;
import java.util.Set;

/**
 * Authorize operations for the current user.
 */
public interface AuthorizationService {

    /**
     * Verify that the current user have right to edit any entity?
     */
    boolean verifyCanEditAllEntities();


    /**
     * Does the current user have edit right on all the given entities?
     */
    boolean canEditEntities(Collection<? extends EntityStructure> entities);

    /**
     * Verify that the current user has edit right on all the given entities.
     * @throws AccessDeniedException if not.
     */
    void verifyCanEditEntities(Collection<? extends EntityStructure> entities);

    /**
     * Verify that the current user has delete right on all the given entities.
     * @throws AccessDeniedException if not.
     */
    void verifyCanDeleteEntities(Collection<? extends EntityStructure> entities);

    /**
     * Verify that the current user has right to delete the given entity.
     */
    boolean canDeleteEntity(EntityStructure entity);

    /**
     * Verify that the current user has right to edit the given entity.
     */
    boolean canEditEntity(EntityStructure entity);

    boolean canEditEntity(Point point);

    Set<StopTypeEnumeration> getAllowedStopPlaceTypes(EntityStructure entity);

    Set<StopTypeEnumeration> getLocationAllowedStopPlaceTypes(boolean canEdit, Point point);

    Set<StopTypeEnumeration> getBannedStopPlaceTypes(EntityStructure entity);

    Set<StopTypeEnumeration> getLocationBannedStopPlaceTypes(boolean canEdit, Point point);

    Set<SubmodeEnumuration> getAllowedSubmodes(EntityStructure entity);

    Set<SubmodeEnumuration> getLocationAllowedSubmodes(boolean canEdit, Point point);

    Set<SubmodeEnumuration> getBannedSubmodes(EntityStructure entity);

    Set<SubmodeEnumuration> getLocationBannedSubmodes(boolean canEdit, Point point);


    boolean isGuest();
}

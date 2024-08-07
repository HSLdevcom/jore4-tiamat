package org.rutebanken.tiamat.model;

import jakarta.persistence.Entity;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity
public class InfoSpotPoster extends InfoSpotPoster_VersionStructure implements Serializable {

}

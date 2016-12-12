package org.rutebanken.tiamat.model;

import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;


@Entity
@SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "seq_boarding_position")
public class BoardingPosition
        extends BoardingPosition_VersionStructure {


}

package org.rutebanken.tiamat.model;

import javax.xml.bind.JAXBElement;


public class JourneyPatternLayoverStructure
        extends JourneyLayoverStructure {

    protected JAXBElement<? extends JourneyPatternRefStructure> journeyPatternRef;

    public JAXBElement<? extends JourneyPatternRefStructure> getJourneyPatternRef() {
        return journeyPatternRef;
    }

    public void setJourneyPatternRef(JAXBElement<? extends JourneyPatternRefStructure> value) {
        this.journeyPatternRef = value;
    }

}

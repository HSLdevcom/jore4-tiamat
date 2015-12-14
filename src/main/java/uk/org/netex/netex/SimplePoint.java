//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package uk.org.netex.netex;

import com.vividsolutions.jts.geom.Point;
import org.hibernate.annotations.Type;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a Simple POINT.
 * 
 * <p>Java class for SimplePoint complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SimplePoint">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}EntityInVersionStructure">
 *       &lt;sequence>
 *         &lt;element name="Name" type="{http://www.netex.org.uk/netex}MultilingualString" minOccurs="0"/>
 *         &lt;element name="Location" type="{http://www.netex.org.uk/netex}LocationStructure" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SimplePoint_VersionStructure", propOrder = {
    "name",
    "location"
})
@Entity
public class SimplePoint
    extends EntityInVersionStructure
{

    @XmlElement(name = "Name")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    protected MultilingualString name;

/*    @XmlElement(name = "Location")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    protected LocationStructure location;*/

    @Type(type="org.hibernate.spatial.GeometryType")
    private Point location;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualString }
     *     
     */
    public MultilingualString getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualString }
     *     
     */
    public void setName(MultilingualString value) {
        this.name = value;
    }

    /**
     * Gets the value of the location property.
     * 
     * @return
     *     possible object is
     *     {@link LocationStructure }
     *     
     */
    public Point getLocation() {
        return location;
    }

    /**
     * Sets the value of the location property.
     * 
     * @param value
     *     allowed object is
     *     {@link LocationStructure }
     *     
     */
    public void setLocation(Point value) {
        this.location = value;
    }


}

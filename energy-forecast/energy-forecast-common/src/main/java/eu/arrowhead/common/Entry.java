package eu.arrowhead.common;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "entryType", propOrder = {
        "outTemp",
        "inTemp",
        "total",
        "water",
        "timestamp",
        "building"
})
@XmlRootElement
public class Entry {

    @XmlElement(name = "OutTemp")
    protected Float outTemp;
    @XmlElement(name = "InTemp")
    protected Float inTemp;
    @XmlElement(name = "Total")
    protected Float total;
    @XmlElement(name = "Water")
    protected Float water;
    @XmlElement(name = "Timestamp", required = true)
    protected Long timestamp;
    @XmlElement(name = "Building", required = true)
    protected Long building;

    /**
     * Gets the value of the outTemp property.
     *
     */
    public Float getOutTemp() {
        return outTemp;
    }

    /**
     * Sets the value of the outTemp property.
     *
     */
    public void setOutTemp(Float value) {
        this.outTemp = value;
    }

    /**
     * Gets the value of the inTemp property.
     *
     */
    public Float getInTemp() {
        return inTemp;
    }

    /**
     * Sets the value of the inTemp property.
     *
     */
    public void setInTemp(Float value) {
        this.inTemp = value;
    }

    /**
     * Gets the value of the total property.
     *
     */
    public Float getTotal() {
        return total;
    }

    /**
     * Sets the value of the total property.
     *
     */
    public void setTotal(Float value) {
        this.total = value;
    }

    /**
     * Gets the value of the water property.
     *
     */
    public Float getWater() {
        return water;
    }

    /**
     * Sets the value of the water property.
     *
     */
    public void setWater(Float value) {
        this.water = value;
    }

    /**
     * Gets the value of the timestamp property.
     *
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the value of the timestamp property.
     *
     */
    public void setTimestamp(Long value) {
        this.timestamp = value;
    }

    /**
     * Gets the value of the building property.
     *
     */
    public Long getBuilding() {
        return building;
    }

    /**
     * Sets the value of the building property.
     *
     */
    public void setBuilding(Long value) {
        this.building = value;
    }

}


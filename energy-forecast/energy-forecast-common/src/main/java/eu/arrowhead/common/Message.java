package eu.arrowhead.common;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "responseType", propOrder = {
        "entry"
})
@XmlRootElement(name = "response")
public class Message {

    protected List<Entry> entry;
    @XmlAttribute(name = "Tstart")
    protected Long tstart;
    @XmlAttribute(name = "Tend")
    protected Long tend;

    public List<Entry> getEntry() {
        if (entry == null) {
            entry = new ArrayList<Entry>();
        }
        return this.entry;
    }

    public void setEntries(List<Entry> entries) {
        this.entry = entries;
    }

    public Long getTstart() {
        return tstart;
    }

    public void setTstart(Long value) {
        this.tstart = value;
    }

    public Long getTend() {
        return tend;
    }

    public void setTend(Long value) {
        this.tend = value;
    }

}


package de.ii.ldproxy.target.gml;

import de.ii.ldproxy.wfs3.api.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.Wfs3Link;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * @author zahnen
 */
@XmlRootElement(name = "Collection")
@XmlType(propOrder = {"name", "title", "description", "links", "extent", "crs"})
public class Wfs3CollectionXml {
    private final Wfs3Collection wfs3Collection;

    public Wfs3CollectionXml() {
        this.wfs3Collection = null;
    }
    public Wfs3CollectionXml(Wfs3Collection wfs3Collection) {
        this.wfs3Collection = wfs3Collection;
    }

    @XmlElement(name = "Name")
    public String getName() {
        return wfs3Collection.getName();
    }

    @XmlElement(name = "Title")
    public String getTitle() {
        return wfs3Collection.getTitle();
    }

    @XmlElement(name = "Description")
    public String getDescription() {
        return wfs3Collection.getDescription();
    }

    @XmlElement(name = "Extent")
    public Wfs3ExtentXml getExtent() {
        return new Wfs3ExtentXml(wfs3Collection.getExtent());
    }

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    public List<Wfs3Link> getLinks() {
        return wfs3Collection.getLinks();
    }

    @XmlElement(name = "crs")
    public List<String> getCrs() {
        return wfs3Collection.getCrs();
    }
}

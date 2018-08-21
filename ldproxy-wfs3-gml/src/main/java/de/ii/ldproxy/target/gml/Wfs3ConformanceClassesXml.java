package de.ii.ldproxy.target.gml;

import de.ii.ldproxy.wfs3.api.ImmutableWfs3Link;
import de.ii.ldproxy.wfs3.api.Wfs3ConformanceClasses;
import de.ii.ldproxy.wfs3.api.Wfs3Link;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@XmlRootElement(name = "ConformsTo")
public class Wfs3ConformanceClassesXml implements Wfs3Xml {
    private final Wfs3ConformanceClasses wfs3ConformanceClasses;

    public Wfs3ConformanceClassesXml() {
        this.wfs3ConformanceClasses = null;
    }

    public Wfs3ConformanceClassesXml(Wfs3ConformanceClasses wfs3ConformanceClasses) {
        this.wfs3ConformanceClasses = wfs3ConformanceClasses;
    }

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    public List<Wfs3Link> getConformsToAsXml() {
        return wfs3ConformanceClasses.getConformsTo().stream().map(link -> ImmutableWfs3Link.builder().href(link).build()).collect(Collectors.toList());
    }
}

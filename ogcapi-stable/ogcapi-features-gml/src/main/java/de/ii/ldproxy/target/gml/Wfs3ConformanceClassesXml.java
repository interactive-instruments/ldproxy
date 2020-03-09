/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import de.ii.ldproxy.ogcapi.domain.ConformanceDeclaration;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@XmlRootElement(name = "ConformsTo")
public class Wfs3ConformanceClassesXml implements Wfs3Xml {
    private final ConformanceDeclaration ogcApiConformanceDeclaration;

    public Wfs3ConformanceClassesXml() {
        this.ogcApiConformanceDeclaration = null;
    }

    public Wfs3ConformanceClassesXml(ConformanceDeclaration ogcApiConformanceDeclaration) {
        this.ogcApiConformanceDeclaration = ogcApiConformanceDeclaration;
    }

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    public List<Wfs3LinkXml> getConformsToAsXml() {
        return ogcApiConformanceDeclaration.getConformsTo()
                                     .stream()
                                     .map(Wfs3LinkXml::new)
                                     .collect(Collectors.toList());
    }

    @XmlAttribute(name="service")
    public String getService() {
        return "OGCAPI-Features";
    }

    @XmlAttribute(name="version")
    public String getVersion() {
        return "1.0.0";
    }

}

/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.app.xml;

import de.ii.ldproxy.ogcapi.common.domain.ConformanceDeclaration;
import de.ii.ldproxy.ogcapi.common.domain.xml.OgcApiXml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.stream.Collectors;

@XmlRootElement(name = "ConformsTo", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
public class OgcApiConformanceClassesXml implements OgcApiXml {
    private final ConformanceDeclaration ogcApiConformanceDeclaration;

    public OgcApiConformanceClassesXml() {
        this.ogcApiConformanceDeclaration = null;
    }

    public OgcApiConformanceClassesXml(ConformanceDeclaration ogcApiConformanceDeclaration) {
        this.ogcApiConformanceDeclaration = ogcApiConformanceDeclaration;
    }

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    public List<OgcApiLinkXml> getConformsToAsXml() {
        return ogcApiConformanceDeclaration.getConformsTo()
                                     .stream()
                                     .map(OgcApiLinkXml::new)
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

    @XmlAttribute(name = "schemaLocation", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public String getSchemaLocation() {
        return "http://www.opengis.net/ogcapi-features-1/1.0 http://schemas.opengis.net/ogcapi/features/part1/1.0/xml/core.xsd";
    }
}

/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.domain;

import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiXml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;
import java.util.stream.Collectors;

@XmlRootElement(name = "Collections", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
@XmlType(propOrder = {"links", "collections"})
public class OgcApiCollectionsXml implements OgcApiXml {

    private final Collections collections;

    public OgcApiCollectionsXml() {
        this.collections = null;
    }

    public OgcApiCollectionsXml(Collections collections) {
        this.collections = collections;
    }

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    public List<OgcApiLink> getLinks() {
        return collections.getLinks();
    }

    @XmlElement(name = "Collection", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
    public List<OgcApiCollectionXml> getCollections() {
        return collections.getCollections()
                          .stream()
                          .map(OgcApiCollectionXml::new)
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

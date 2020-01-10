/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import de.ii.ldproxy.ogcapi.domain.Collections;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@XmlRootElement(name = "Collections")
@XmlType(propOrder = {"links", "collections"})
public class Wfs3CollectionsXml implements Wfs3Xml {

    private final Collections wfs3Collections;

    public Wfs3CollectionsXml() {
        this.wfs3Collections = null;
    }

    public Wfs3CollectionsXml(Collections wfs3Collections) {
        this.wfs3Collections = wfs3Collections;
    }

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    public List<OgcApiLink> getLinks() {
        return wfs3Collections.getLinks();
    }

    @XmlElement(name = "Collection")
    public List<Wfs3CollectionXml> getCollections() {
        return wfs3Collections.getCollections()
                .stream()
                .map(Wfs3CollectionXml::new)
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

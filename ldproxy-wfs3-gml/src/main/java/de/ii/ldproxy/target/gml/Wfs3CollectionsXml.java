/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import de.ii.ldproxy.wfs3.api.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.Wfs3Collections;
import de.ii.ldproxy.wfs3.api.Wfs3Link;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@XmlRootElement(name = "Collections")
@XmlType(propOrder = {"links", "crs", "collections"})
public class Wfs3CollectionsXml implements Wfs3Xml {

    private final Wfs3Collections wfs3Collections;

    public Wfs3CollectionsXml() {
        this.wfs3Collections = null;
    }

    public Wfs3CollectionsXml(Wfs3Collections wfs3Collections) {
        this.wfs3Collections = wfs3Collections;
    }

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    public List<Wfs3Link> getLinks() {
        return wfs3Collections.getLinks();
    }

    @XmlElement(name = "crs")
    public List<String> getCrs() {
        return wfs3Collections.getCrs();
    }

    @XmlElement(name = "Collection")
    public List<Wfs3CollectionXml> getCollections() {
        return wfs3Collections.getCollections().stream().map(wfs3Collection -> new Wfs3CollectionXml(wfs3Collection)).collect(Collectors.toList());
    }
}

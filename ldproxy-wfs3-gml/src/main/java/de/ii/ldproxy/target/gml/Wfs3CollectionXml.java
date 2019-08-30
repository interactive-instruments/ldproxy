/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import de.ii.ldproxy.ogcapi.domain.Wfs3Link;
import de.ii.ldproxy.ogcapi.domain.Wfs3Collection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * @author zahnen
 */
@XmlRootElement(name = "Collection")
@XmlType(propOrder = {"id", "title", "description", "links", "extent", "crs"})
public class Wfs3CollectionXml {
    private final Wfs3Collection wfs3Collection;

    public Wfs3CollectionXml() {
        this.wfs3Collection = null;
    }
    public Wfs3CollectionXml(Wfs3Collection wfs3Collection) {
        this.wfs3Collection = wfs3Collection;
    }

    @XmlElement(name = "Id")
    public String getId() {
        return wfs3Collection.getId();
    }

    @XmlElement(name = "Title")
    public String getTitle() {
        return wfs3Collection.getTitle().orElse(null);
    }

    @XmlElement(name = "Description")
    public String getDescription() {
        return wfs3Collection.getDescription().orElse(null);
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

/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiCollection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * @author zahnen
 */
@XmlRootElement(name = "Collection")
@XmlType(propOrder = {"id", "title", "description", "links", "extent"})
public class Wfs3CollectionXml {
    private final OgcApiCollection ogcApiCollection;

    public Wfs3CollectionXml() {
        this.ogcApiCollection = null;
    }
    public Wfs3CollectionXml(OgcApiCollection ogcApiCollection) {
        this.ogcApiCollection = ogcApiCollection;
    }

    @XmlElement(name = "Id")
    public String getId() {
        return ogcApiCollection.getId();
    }

    @XmlElement(name = "Title")
    public String getTitle() {
        return ogcApiCollection.getTitle().orElse(null);
    }

    @XmlElement(name = "Description")
    public String getDescription() {
        return ogcApiCollection.getDescription().orElse(null);
    }

    @XmlElement(name = "Extent")
    public Wfs3ExtentXml getExtent() {
        return new Wfs3ExtentXml(ogcApiCollection.getExtent().orElse(null));
    }

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    public List<OgcApiLink> getLinks() {
        return ogcApiCollection.getLinks();
    }

}

/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.app.xml;

import de.ii.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ogcapi.foundation.domain.Link;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlRootElement(name = "Collection")
@XmlType(propOrder = {"id", "title", "description", "links", "extent"})
public class OgcApiCollectionXml {
    private final OgcApiCollection ogcApiCollection;

    public OgcApiCollectionXml() {
        this.ogcApiCollection = null;
    }
    public OgcApiCollectionXml(OgcApiCollection ogcApiCollection) {
        this.ogcApiCollection = ogcApiCollection;
    }

    @XmlElement(name = "Id", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
    public String getId() {
        return ogcApiCollection.getId();
    }

    @XmlElement(name = "Title", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
    public String getTitle() {
        return ogcApiCollection.getTitle().orElse(null);
    }

    @XmlElement(name = "Description", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
    public String getDescription() {
        return ogcApiCollection.getDescription().orElse(null);
    }

    @XmlElement(name = "Extent", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
    public OgcApiExtentXml getExtent() {
        return new OgcApiExtentXml(ogcApiCollection.getExtent().orElse(null));
    }

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    public List<Link> getLinks() {
        return ogcApiCollection.getLinks();
    }

}

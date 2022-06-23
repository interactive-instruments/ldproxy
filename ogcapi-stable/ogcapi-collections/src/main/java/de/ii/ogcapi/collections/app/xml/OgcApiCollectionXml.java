/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.app.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import de.ii.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ogcapi.foundation.domain.Link;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "Collection", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
@XmlType(propOrder = {"id", "title", "description", "links", "extent"})
public class OgcApiCollectionXml {
  private final OgcApiCollection ogcApiCollection;

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

  @XmlElement(name = "atom:link", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
  @JacksonXmlElementWrapper(useWrapping = false)
  public List<Link> getLinks() {
    return ogcApiCollection.getLinks();
  }
}

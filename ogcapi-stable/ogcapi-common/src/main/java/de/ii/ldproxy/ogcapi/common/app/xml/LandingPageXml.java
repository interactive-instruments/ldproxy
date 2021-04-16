/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.app.xml;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.common.domain.xml.OgcApiXml;
import de.ii.ldproxy.ogcapi.domain.Link;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlRootElement(name = "LandingPage", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
@XmlType(propOrder = {"title", "description", "links"})
public class LandingPageXml implements OgcApiXml {

    private List<Link> links;
    private String title;
    private String description;

    public LandingPageXml() {

    }

    public LandingPageXml(List<Link> links, String title, String description) {
        this.links = ImmutableList.copyOf(links);
        this.title = title;
        this.description = description;
    }

    @XmlElement(name = "Title", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
    public String getTitle() {
        return title;
    }

    @XmlElement(name = "Description", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
    public String getDescription() {
        return description;
    }

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    public List<Link> getLinks() {
        return links;
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

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

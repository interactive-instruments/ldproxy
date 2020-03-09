/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * @author zahnen
 */
@XmlRootElement(name = "LandingPage")
@XmlType(propOrder = {"title", "description", "links"})
public class LandingPageXml implements Wfs3Xml {

    private List<OgcApiLink> links;
    private String title;
    private String description;

    public LandingPageXml() {

    }

    public LandingPageXml(List<OgcApiLink> links, String title, String description) {
        this.links = ImmutableList.copyOf(links);
        this.title = title;
        this.description = description;
    }

    @XmlElement(name = "Title")
    public String getTitle() {
        return title;
    }

    @XmlElement(name = "Description")
    public String getDescription() {
        return description;
    }

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    public List<OgcApiLink> getLinks() {
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


    public void setLinks(List<OgcApiLink> links) {
        this.links = links;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

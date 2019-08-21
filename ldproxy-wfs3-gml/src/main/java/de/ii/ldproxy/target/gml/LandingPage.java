/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.Wfs3Link;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author zahnen
 */
@XmlRootElement(name = "LandingPage")
public class LandingPage implements Wfs3Xml {
    private List<Wfs3Link> links;

    public LandingPage() {

    }

    public LandingPage(List<Wfs3Link> links) {
        this.links = ImmutableList.copyOf(links);
    }

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    public List<Wfs3Link> getLinks() {
        return links;
    }

    public void setLinks(List<Wfs3Link> links) {
        this.links = links;
    }
}

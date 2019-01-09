/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.api.Wfs3LinksGenerator;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
@XmlRootElement(name = "LandingPage")
public class LandingPage implements Wfs3Xml {
    private List<Wfs3Link> links;

    public LandingPage() {

    }

    public LandingPage(final URICustomizer uriCustomizer, final Wfs3ServiceData serviceData, final Wfs3MediaType mediaType, final Wfs3MediaType[] alternativeMediaTypes) {
        final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();

        //TODO: create abstraction for loading live data from provider - new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType()).getAsUrl()
        //TODO check links
        this.links =  wfs3LinksGenerator.generateDatasetLinks(uriCustomizer.copy(), Optional.empty(), mediaType, false, alternativeMediaTypes);
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

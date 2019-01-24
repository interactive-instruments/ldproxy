/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.sitemaps;

import com.google.common.collect.ImmutableList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author zahnen
 */
@XmlRootElement(name = "urlset")
public class Sitemap {
    private final List<Site> sites;

    public Sitemap() {
        this.sites = ImmutableList.of();
    }

    public Sitemap(List<Site> sites) {
        this.sites = sites;
    }

    @XmlElement(name = "url")
    public List<Site> getSites() {
        return sites;
    }
}

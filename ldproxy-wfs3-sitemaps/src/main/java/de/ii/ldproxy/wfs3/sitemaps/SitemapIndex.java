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
@XmlRootElement(name = "sitemapindex")
public class SitemapIndex {
    private final List<Site> sitemaps;

    public SitemapIndex() {
        this.sitemaps = ImmutableList.of();
    }

    public SitemapIndex(List<Site> sitemaps) {
        this.sitemaps = sitemaps;
    }

    @XmlElement(name = "sitemap")
    public List<Site> getSitemaps() {
        return sitemaps;
    }
}

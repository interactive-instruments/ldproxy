/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.sitemaps;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * @author zahnen
 */
@XmlRootElement(name = "sitemapindex")
@XmlType(propOrder = {"loc", "lastmod", "changefreq", "priority"})
public class Site {

    private final String loc;
    private final String lastmod = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())
                                                                         .format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
    private final String changefreq = "never";
    private final String priority = "0.8";

    public Site() {
        this.loc = "";
    }

    public Site(String loc) {
        this.loc = loc;
    }

    @XmlElement(name = "loc")
    public String getLoc() {
        return loc;
    }

    @XmlElement(name = "lastmod")
    public String getLastmod() {
        return lastmod;
    }

    @XmlElement(name = "changefreq")
    public String getChangefreq() {
        return changefreq;
    }

    @XmlElement(name = "priority")
    public String getPriority() {
        return priority;
    }
}

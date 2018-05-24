/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * @author zahnen
 */

@XmlType(propOrder={"rel","type","title", "href"})
public class Wfs3Link {
    @XmlAttribute
    public String href;
    @XmlAttribute
    public String rel;
    @XmlAttribute
    public String type;
    @XmlAttribute
    public String title;

    public Wfs3Link() {}

    public Wfs3Link(String href, String rel, String type, String title) {
        this.href = href;
        this.rel = rel;
        this.type = type;
        this.title = title;
    }
}

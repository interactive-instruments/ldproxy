/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.app.xml;

import javax.xml.bind.annotation.XmlAttribute;

public class OgcApiLinkXml {

    private final String href;

    public OgcApiLinkXml(String href) {
        this.href = href;
    }

    @XmlAttribute
    public String getHref() {
        return href;
    }
}

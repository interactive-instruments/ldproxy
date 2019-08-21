/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import javax.xml.bind.annotation.XmlAttribute;

public class Wfs3LinkXml {

    private final String href;

    public Wfs3LinkXml(String href) {
        this.href = href;
    }

    @XmlAttribute
    public String getHref() {
        return href;
    }
}

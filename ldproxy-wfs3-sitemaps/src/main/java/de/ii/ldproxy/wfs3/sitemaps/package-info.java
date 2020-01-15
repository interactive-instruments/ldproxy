/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * @author zahnen
 */
@XmlSchema(
        namespace = "http://www.sitemaps.org/schemas/sitemap/0.9",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(prefix="sm", namespaceURI="http://www.sitemaps.org/schemas/sitemap/0.9")
        }
)
package de.ii.ldproxy.wfs3.sitemaps;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
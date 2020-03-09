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
        namespace = "http://www.opengis.net/wfs/3.0",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(prefix="wfs", namespaceURI="http://www.opengis.net/wfs/3.0"),
                @XmlNs(prefix="atom", namespaceURI="http://www.w3.org/2005/Atom"),
                @XmlNs(prefix="xsi", namespaceURI="http://www.w3.org/2001/XMLSchema-instance")
        }
)
package de.ii.ldproxy.wfs3;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
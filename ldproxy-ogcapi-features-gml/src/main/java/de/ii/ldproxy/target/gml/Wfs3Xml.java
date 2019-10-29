/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * @author zahnen
 */
public interface Wfs3Xml {

    @XmlAttribute(name = "service")
    default String getServiceType() {
        return "WFS";
    }

    @XmlAttribute(name = "version")
    default String getServiceVersion() {
        return "3.0.0";
    }

    @XmlAttribute(name = "schemaLocation", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    default String getSchemaLocation() {
        return "http://www.opengis.net/ogcapi-features-1/1.0 https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/xml/core.xsd";
    }
}

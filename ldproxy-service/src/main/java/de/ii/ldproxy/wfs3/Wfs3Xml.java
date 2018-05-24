/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * @author zahnen
 */
public class Wfs3Xml {

    public Wfs3Xml() {}

    @JsonIgnore
    @XmlAttribute(name = "service")
    public String getServiceType() {
        return "WFS";
    }

    @JsonIgnore
    @XmlAttribute(name = "version")
    public String getServiceVersion() {
        return "3.0.0";
    }

    @JsonIgnore
    @XmlAttribute(name = "schemaLocation", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public String getSchemaLocation() {
        return "http://www.opengis.net/wfs/3.0 https://raw.githubusercontent.com/opengeospatial/WFS_FES/master/core/xml/wfs.xsd";
    }
}

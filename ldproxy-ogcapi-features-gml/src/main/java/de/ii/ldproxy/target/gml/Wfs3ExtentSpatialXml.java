/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "Spatial")
@XmlType(propOrder = {"LowerCorner", "UpperCorner"})
public class Wfs3ExtentSpatialXml {

    @XmlAttribute
    public String crs = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
    public String LowerCorner;
    public String UpperCorner;

    public Wfs3ExtentSpatialXml() {
    }

    public Wfs3ExtentSpatialXml(String lowerCorner, String upperCorner) {
        this.LowerCorner = lowerCorner;
        this.UpperCorner = upperCorner;
    }

    public Wfs3ExtentSpatialXml(String lowerCorner, String upperCorner, String crs) {
        this.LowerCorner = lowerCorner;
        this.UpperCorner = upperCorner;
        this.crs = crs;
    }

    /*
    @XmlElement(name = "LowerCorner")
    public String getLowerCorner() {
        return LowerCorner;
    }

    @XmlElement(name = "UpperCorner")
    public String getUpperCorner() {
        return UpperCorner;
    }
    */
}

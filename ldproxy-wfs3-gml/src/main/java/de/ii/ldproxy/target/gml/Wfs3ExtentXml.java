/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import de.ii.ldproxy.wfs3.api.Wfs3Extent;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.Locale;
import java.util.Objects;

/**
 * @author zahnen
 */
public class Wfs3ExtentXml {
    private final Wfs3Extent extent;

    public Wfs3ExtentXml(Wfs3Extent extent) {
        this.extent = extent;
    }

    @XmlElement(name = "Spatial")
    public SpatialExtent getSpatial() {
        return new SpatialExtent(String.format(Locale.US, "%f %f",extent.getSpatial()[0], extent.getSpatial()[1]), String.format(Locale.US, "%f %f", extent.getSpatial()[2], extent.getSpatial()[3]));
    }

    @XmlElement(name = "Temporal")
    public TemporalExtent getTemporal() {
        return Objects.nonNull(extent.getTemporal()) ? new TemporalExtent(extent.getTemporal()[0], extent.getTemporal()[1]) : null;
    }

    public static class SpatialExtent {
        @XmlAttribute
        public String crs = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
        public String LowerCorner;
        public String UpperCorner;

        public SpatialExtent() {
        }

        public SpatialExtent(String lowerCorner, String upperCorner) {
            this.LowerCorner = lowerCorner;
            this.UpperCorner = upperCorner;
        }
    }

    public static class TemporalExtent {
        @XmlAttribute
        public String trs = "http://www.opengis.net/def/uom/ISO-8601/0/Gregorian";
        public String begin;
        public String end;

        public TemporalExtent() {
        }

        public TemporalExtent(String begin, String end) {
            this.begin = begin;
            this.end = end;
        }
    }
}

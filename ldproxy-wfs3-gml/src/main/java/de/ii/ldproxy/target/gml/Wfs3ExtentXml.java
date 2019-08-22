/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import de.ii.ldproxy.ogcapi.domain.Wfs3Extent;
import de.ii.ldproxy.ogcapi.domain.Wfs3ExtentSpatial;
import de.ii.ldproxy.ogcapi.domain.Wfs3ExtentTemporal;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.Locale;
import java.util.Objects;

/**
 * @author zahnen
 */
public class Wfs3ExtentXml {
    private final Wfs3Extent extent;
    private final Wfs3ExtentSpatial spatial;
    private final Wfs3ExtentTemporal temporal;

    public Wfs3ExtentXml(Wfs3Extent extent) {
        this.extent = extent;
        this.spatial = extent.getSpatial();
        this.temporal = extent.getTemporal();
    }

    @XmlElement(name = "Spatial")
    public SpatialExtent getSpatial() {
        return new SpatialExtent(String.format(Locale.US, "%f %f", spatial.getBbox()[0][0], spatial.getBbox()[0][1]), String.format(Locale.US, "%f %f", spatial.getBbox()[0][2], spatial.getBbox()[0][3]));
    }

    @XmlElement(name = "Temporal")
    public TemporalExtent getTemporal() {
        return Objects.nonNull(temporal) ? new TemporalExtent(temporal.getInterval()[0][0], temporal.getInterval()[0][1]) : null;
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

        public SpatialExtent(String lowerCorner, String upperCorner, String crs) {
            this.LowerCorner = lowerCorner;
            this.UpperCorner = upperCorner;
            this.crs = crs;
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

        public TemporalExtent(String begin, String end, String trs) {
            this.begin = begin;
            this.end = end;
            this.trs = trs;
        }
    }
}

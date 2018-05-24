/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * @author zahnen
 */
@XmlType(propOrder = {"name", "title", "description", "links", "extent"})
public class Wfs3Collection {

    private String name;
    private String title;
    private String description;
    private Extent extent;
    private List<Wfs3Link> links;
    private String prefixedName;

    public Wfs3Collection() {

    }

    public Wfs3Collection(String name, String title, String description, Extent extent, List<Wfs3Link> links, String prefixedName) {
        this.name = name;
        this.title = title;
        this.description = description;
        this.extent = extent;
        this.links = links;
        this.prefixedName = prefixedName;
    }

    @XmlElement(name = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "Title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @XmlElement(name = "Description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @XmlElement(name = "Extent")
    public Extent getExtent() {
        return extent;
    }

    public void setExtent(Extent extent) {
        this.extent = extent;
    }

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    public List<Wfs3Link> getLinks() {
        return links;
    }

    public void setLinks(List<Wfs3Link> links) {
        this.links = links;
    }

    @JsonIgnore
    @XmlTransient
    public String getPrefixedName() {
        return prefixedName;
    }

    public void setPrefixedName(String prefixedName) {
        this.prefixedName = prefixedName;
    }

    public static class Extent {
        private double[] spatial;
        private String[] temporal;

        public Extent() {

        }

        public Extent(String xmin, String ymin, String xmax, String ymax) {
            try {
                this.spatial = ImmutableList.of(xmin, ymin, xmax, ymax)
                                            .stream()
                                            .mapToDouble(Double::parseDouble)
                                            .toArray();
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        public Extent(long begin, long end) {
            final String[] t = {Instant.ofEpochMilli(begin)
                                       .truncatedTo(ChronoUnit.SECONDS).toString(), Instant.ofEpochMilli(end)
                                                                                           .truncatedTo(ChronoUnit.SECONDS).toString()};
            this.temporal = t;
        }

        @XmlTransient
        public double[] getSpatial() {
            return spatial;
        }

        @JsonIgnore
        @XmlElement(name = "Spatial")
        public Extent.SpatialExtent getSpatialXml() {
            return new Extent.SpatialExtent(String.format(Locale.US, "%f %f", spatial[0], spatial[1]), String.format(Locale.US, "%f %f", spatial[2], spatial[3]));
        }

        public void setSpatial(double[] spatial) {
            this.spatial = spatial;
        }

        @XmlTransient
        public String[] getTemporal() {
            return temporal;
        }

        @JsonIgnore
        @XmlElement(name = "Temporal")
        public Extent.TemporalExtent getTemporalAsXml() {
            return new Extent.TemporalExtent(temporal[0], temporal[1]);
        }

        public void setTemporal(String[] temporal) {
            this.temporal = temporal;
        }

        public static class SpatialExtent {
            @XmlAttribute
            public String crs = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
            public String LowerCorner;
            public String UpperCorner;

            public SpatialExtent() {}

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

            public TemporalExtent() {}

            public TemporalExtent(String begin, String end) {
                this.begin = begin;
                this.end = end;
            }
        }
    }
}

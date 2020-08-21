/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.target.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {"begin", "end"})
public class OgcApiExtentTemporalXml {

    @XmlAttribute
    public String trs = null;

    private String begin = null;
    private String end = null;

    public OgcApiExtentTemporalXml() {
    }

    public OgcApiExtentTemporalXml(String begin, String end) {
        this.begin = begin;
        this.end = end;
        this.trs = "http://www.opengis.net/def/uom/ISO-8601/0/Gregorian";
    }

    public OgcApiExtentTemporalXml(String begin, String end, String trs) {
        this.begin = begin;
        this.end = end;
        this.trs = trs;
    }

    @XmlElement(name = "begin", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
    public String getBegin() {
        return begin;
    }

    @XmlElement(name = "end", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
    public String getEnd() {
        return end;
    }
}

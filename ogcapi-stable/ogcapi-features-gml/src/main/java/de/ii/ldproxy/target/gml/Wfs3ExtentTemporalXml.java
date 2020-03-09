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

@XmlRootElement(name = "Temporal")
@XmlType(propOrder = {"begin", "end"})
public class Wfs3ExtentTemporalXml {

    @XmlAttribute
    public String trs = "http://www.opengis.net/def/uom/ISO-8601/0/Gregorian";
    public String begin;
    public String end;

    public Wfs3ExtentTemporalXml() {
    }

    public Wfs3ExtentTemporalXml(String begin, String end) {
        this.begin = begin;
        this.end = end;
    }

    public Wfs3ExtentTemporalXml(String begin, String end, String trs) {
        this.begin = begin;
        this.end = end;
        this.trs = trs;
    }

    /*
    @XmlElement(name = "begin")
    public String getBegin() {
        return begin;
    }

    @XmlElement(name = "end")
    public String getEnd() {
        return end;
    }
     */
}

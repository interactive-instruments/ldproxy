/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.app.xml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {"begin", "end"})
@SuppressWarnings("PMD.FieldNamingConventions")
public class OgcApiExtentTemporalXml {

  @XmlAttribute public String trs;

  private String begin;
  private String end;

  @SuppressWarnings("unused")
  public OgcApiExtentTemporalXml() {}

  public OgcApiExtentTemporalXml(String begin, String end) {
    this.begin = begin;
    this.end = end;
    this.trs = "http://www.opengis.net/def/uom/ISO-8601/0/Gregorian";
  }

  @SuppressWarnings("unused")
  public OgcApiExtentTemporalXml(String begin, String end, String trs) {
    this.begin = begin;
    this.end = end;
    this.trs = trs;
  }

  @Nullable
  @JsonInclude(Include.NON_NULL)
  @XmlElement(name = "begin", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
  public String getBegin() {
    return begin;
  }

  @Nullable
  @JsonInclude(Include.NON_NULL)
  @XmlElement(name = "end", namespace = "http://www.opengis.net/ogcapi-features-1/1.0")
  public String getEnd() {
    return end;
  }
}

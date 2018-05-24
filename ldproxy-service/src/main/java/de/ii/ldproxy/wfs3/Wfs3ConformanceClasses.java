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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@XmlRootElement(name = "ConformsTo")
public class Wfs3ConformanceClasses extends Wfs3Xml {
    public Wfs3ConformanceClasses() {}

    private static final List<String> classes = ImmutableList.<String>builder()
            .add("http://www.opengis.net/spec/wfs-1/3.0/req/core")
            .add("http://www.opengis.net/spec/wfs-1/3.0/req/oas30")
            .add("http://www.opengis.net/spec/wfs-1/3.0/req/html")
            .add("http://www.opengis.net/spec/wfs-1/3.0/req/geojson")
            .add("http://www.opengis.net/spec/wfs-1/3.0/req/gmlsf2")
            .build();

    @XmlTransient
    public List<String> getConformsTo() {
        return classes;
    }

    @JsonIgnore
    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    public List<Wfs3Link> getConformsToAsXml() {
        return classes.stream().map(link -> new Wfs3Link(link, null, null, null)).collect(Collectors.toList());
    }
}

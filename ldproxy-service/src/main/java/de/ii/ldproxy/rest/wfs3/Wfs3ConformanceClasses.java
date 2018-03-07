/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest.wfs3;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * @author zahnen
 */
public class Wfs3ConformanceClasses {
    private final List<String> classes = ImmutableList.<String>builder()
            .add("http://www.opengis.net/spec/wfs-1/3.0/req/core")
            .add("http://www.opengis.net/spec/wfs-1/3.0/req/oas30")
            .add("http://www.opengis.net/spec/wfs-1/3.0/req/html")
            .add("http://www.opengis.net/spec/wfs-1/3.0/req/geojson")
            .add("http://www.opengis.net/spec/wfs-1/3.0/req/gmlsf2")
            .build();

    public List<String> getConformsTo() {
        return classes;
    }
}

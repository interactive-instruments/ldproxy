/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import com.google.common.collect.ImmutableMap;

import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author zahnen
 */
public class Wfs3MediaTypes {

    public static final String JSON = MediaType.APPLICATION_JSON;
    public static final String XML = MediaType.APPLICATION_XML;
    public static final String HTML = MediaType.TEXT_HTML;
    public static final String GEO_JSON = "application/geo+json";
    public static final String GML = "application/gml+xml;version=3.2;profile=http://www.opengis.net/def/profile/ogc/2.0/gml-sf2";
    public static final String MVT = "application/vnd.mapbox-vector-tile";

    public static final Map<String, String> NAMES = new ImmutableMap.Builder<String, String>()
            .put(JSON, "JSON")
            .put(XML, "XML")
            .put(HTML, "HTML")
            .put(GEO_JSON, "GeoJSON")
            .put(GML, "GML")
            .put(MVT, "Mapbox Vector Tile")
            .build();

    public static final Map<String, String> FORMATS = new ImmutableMap.Builder<String, String>()
            .put(JSON, "json")
            .put(XML, "xml")
            .put(HTML, "html")
            .put(GEO_JSON, "json")
            .put(GML, "xml")
            .put(MVT, "mvt")
            .build();

    public static final Map<String, String> FEATURE = new ImmutableMap.Builder<String, String>()
            .put(JSON, GEO_JSON)
            .put(XML, GML)
            .put(HTML, HTML)
            .put(GML, "xml")
            .build();
}

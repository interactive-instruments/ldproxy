package de.ii.ldproxy.rest.wfs3;

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
    public static final String GEO_JSON = new MediaType("application", "geo+json").toString();
    public static final String GML = new MediaType("application", "gml+xml", ImmutableMap.of("version", "3.2", "profile", "http://www.opengis.net/def/profile/ogc/2.0/gml-sf2")).toString();

    public static final Map<String, String> NAMES = new ImmutableMap.Builder<String, String>()
            .put(JSON, "JSON")
            .put(XML, "XML")
            .put(HTML, "HTML")
            .put(GEO_JSON, "GeoJSON")
            .put(GML, "GML")
            .build();

    public static final Map<String, String> FORMATS = new ImmutableMap.Builder<String, String>()
            .put(JSON, "json")
            .put(XML, "xml")
            .put(HTML, "html")
            .put(GEO_JSON, "json")
            .put(GML, "xml")
            .build();
}

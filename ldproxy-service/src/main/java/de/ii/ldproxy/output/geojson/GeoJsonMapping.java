package de.ii.ldproxy.output.geojson;

import de.ii.ogc.wfs.proxy.TargetMapping;

/**
 * @author zahnen
 */
public interface GeoJsonMapping extends TargetMapping {
    String getName();
    GeoJsonPropertyMapping.GEO_JSON_TYPE getType();
}

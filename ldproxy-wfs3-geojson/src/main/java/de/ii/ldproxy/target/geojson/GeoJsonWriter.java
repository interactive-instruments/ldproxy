package de.ii.ldproxy.target.geojson;

import de.ii.ldproxy.wfs3.api.FeatureWriterGeoJson;

/**
 * @author zahnen
 */
public interface GeoJsonWriter extends FeatureWriterGeoJson<FeatureTransformationContextGeoJson> {
    GeoJsonWriter create();
}

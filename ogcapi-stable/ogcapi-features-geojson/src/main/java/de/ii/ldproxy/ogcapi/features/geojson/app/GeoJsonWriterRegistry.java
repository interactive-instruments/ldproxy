package de.ii.ldproxy.ogcapi.features.geojson.app;

import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;

import java.util.List;

public interface GeoJsonWriterRegistry {
    List<GeoJsonWriter> getGeoJsonWriters();
}

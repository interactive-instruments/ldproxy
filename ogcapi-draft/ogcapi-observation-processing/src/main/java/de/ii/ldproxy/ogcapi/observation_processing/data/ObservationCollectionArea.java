package de.ii.ldproxy.ogcapi.observation_processing.data;

import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;

public class ObservationCollectionArea extends ObservationCollection {
    private final GeometryMultiPolygon area;

    public ObservationCollectionArea(GeometryMultiPolygon area, TemporalInterval interval) {
        super(interval);
        this.area = area;
    }

    public GeometryMultiPolygon getGeometry() {
        return area;
    }
}

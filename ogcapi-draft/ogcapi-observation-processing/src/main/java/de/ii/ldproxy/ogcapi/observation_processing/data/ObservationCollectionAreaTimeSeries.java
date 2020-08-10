package de.ii.ldproxy.ogcapi.observation_processing.data;

public class ObservationCollectionAreaTimeSeries extends ObservationCollectionTimeSeries {
    private final GeometryMultiPolygon area;

    public ObservationCollectionAreaTimeSeries(GeometryMultiPolygon area) {
        super();
        this.area = area;
    }

    @Override
    public GeometryMultiPolygon getGeometry() {
        return area;
    }
}

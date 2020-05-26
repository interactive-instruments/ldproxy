package de.ii.ldproxy.ogcapi.observation_processing.data;

import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;

import java.util.Optional;

public class ObservationCollectionPoint extends ObservationCollection {
    private final GeometryPoint point;
    private Optional<String> code;
    private Optional<String> name;

    public ObservationCollectionPoint(GeometryPoint point, TemporalInterval interval, Optional<String> code, Optional<String> name) {
        super(interval);
        this.point = point;
        this.code = code;
        this.name = name;
    }

    public GeometryPoint getGeometry() {
        return point;
    }

    public Optional<String> getCode() {
        return code;
    }

    public Optional<String> getName() {
        return name;
    }
}

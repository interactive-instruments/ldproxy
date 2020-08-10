package de.ii.ldproxy.ogcapi.observation_processing.data;

import java.util.Optional;

public class ObservationCollectionPointTimeSeries extends ObservationCollectionTimeSeries {
    private final GeometryPoint point;
    private Optional<String> code;
    private Optional<String> name;

    ObservationCollectionPointTimeSeries(GeometryPoint point, String code, String name) {
        super();
        this.point = point;
        this.code = Optional.ofNullable(code);
        this.name = Optional.ofNullable(name);
    }

    @Override
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

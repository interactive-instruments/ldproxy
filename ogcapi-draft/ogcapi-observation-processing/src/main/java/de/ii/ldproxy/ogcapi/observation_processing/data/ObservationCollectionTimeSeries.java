package de.ii.ldproxy.ogcapi.observation_processing.data;

import java.time.temporal.Temporal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class ObservationCollectionTimeSeries {
    ConcurrentMap<Temporal, ConcurrentMap<String, Number>> values;

    ObservationCollectionTimeSeries() {
        this.values = new ConcurrentHashMap<>();
    }

    public ConcurrentMap<Temporal, ConcurrentMap<String, Number>> getValues() {
        return values;
    }

    public abstract Geometry getGeometry();

    public void addTimeStep(Temporal time) {
        values.put(time, new ConcurrentHashMap<>());
    }

    public void addValue(Temporal time, String property, Number value) {
        if (values.containsKey(time)) {
            values.get(time).put(property, value);
        } else {
            // TODO handle error
        }
    }
}

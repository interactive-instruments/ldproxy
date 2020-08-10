package de.ii.ldproxy.ogcapi.observation_processing.data;

import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class ObservationCollection {
    final TemporalInterval interval;
    ConcurrentMap<String, Number> values;

    ObservationCollection(TemporalInterval interval) {
        this.interval = interval;
        this.values = new ConcurrentHashMap<>();
    }

    public void put(String variable_function, Number value) { values.put(variable_function, value); }

    public ConcurrentMap<String, Number> getValues() { return values; }

    public abstract Geometry getGeometry();

    public TemporalInterval getInterval() {
        return interval;
    }
}

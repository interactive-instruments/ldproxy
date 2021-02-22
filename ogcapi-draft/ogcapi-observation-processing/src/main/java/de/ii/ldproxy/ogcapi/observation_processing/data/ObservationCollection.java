/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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

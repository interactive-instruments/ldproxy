/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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

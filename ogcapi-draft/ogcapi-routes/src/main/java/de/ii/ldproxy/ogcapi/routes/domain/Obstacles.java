/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableWaypoints.Builder.class)
public abstract class Waypoints {
    public final String getType() { return "MultiPoint"; }
    public abstract List<List<Float>> getCoordinates();
    @Value.Default
    public String getCoordRefSys() { return OgcCrs.CRS84_URI; }

    @Value.Check
    void check() {
        Preconditions.checkState(getType().equals("MultiPoint"), "Waypoints is not a MultiPoint geometry. Found: {}.", getType());
        Preconditions.checkState(getCoordinates().size()>=2, "At least two waypoints are required. Found: {}.", getCoordinates().size());
        getCoordinates().forEach(waypoint -> {
            Preconditions.checkState(waypoint.size()>=2, "At least two coordinates are required per waypoint. Found: {}.", waypoint.size());
            Preconditions.checkState(waypoint.size()<=3, "At most three coordinates are required per waypoint. Found: {}.", waypoint.size());
        });
    }
}

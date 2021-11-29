/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.ii.xtraplatform.cql.domain.Geometry;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableRouteDefinition.Builder.class)
public interface RouteDefinition {
    @Nullable
    String getName();
    @Nullable
    String getPreference();
    @Nullable
    Double getWeight();
    @Nullable
    Double getHeight();
    @Nullable
    Geometry.MultiPolygon getObstacles(); // TODO use proper class, after consolidation of geometry types?
    List<String> getAdditionalFlags();
    WaypointWrapper getWaypoints();
}

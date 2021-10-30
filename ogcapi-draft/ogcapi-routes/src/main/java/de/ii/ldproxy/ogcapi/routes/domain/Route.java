/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableRouteDefinition.Builder.class)
public abstract class Route {

    public enum STATUS {accepted, running, successful, failed}

    public final String getType() { return "FeatureCollection"; }
    public abstract Optional<String> getName();
    public abstract STATUS getStatus();
    public abstract List<Double> getBbox();
    public abstract List<RouteComponent> getFeatures();
}

/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.Link;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableRoute.Builder.class)
public abstract class Route {

    public final String getType() { return "FeatureCollection"; }
    public abstract Optional<String> getName();
    public abstract List<Double> getBbox();
    public abstract List<RouteComponent> getFeatures();
    public abstract List<Link> getLinks();

}

/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableRouteComponent.Builder.class)
public abstract class RouteComponent<T> {

  public final String getType() {
    return "Feature";
  }

  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  public abstract Optional<Integer> getId();

  public abstract T getGeometry();

  public abstract Map<String, Object> getProperties();
}

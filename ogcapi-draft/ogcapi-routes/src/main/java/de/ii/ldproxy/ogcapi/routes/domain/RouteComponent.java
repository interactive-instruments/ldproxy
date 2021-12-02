/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@JsonSubTypes({
    @JsonSubTypes.Type(value = RouteOverview.class, name = "overview"),
    @JsonSubTypes.Type(value = RouteStart.class, name = "start"),
    @JsonSubTypes.Type(value = RouteEnd.class, name = "end"),
    @JsonSubTypes.Type(value = RouteSegment.class, name = "segment")
})
public abstract class RouteComponent {

  public final String getType() { return "Feature"; }

  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  public abstract Optional<Integer> getId();
}

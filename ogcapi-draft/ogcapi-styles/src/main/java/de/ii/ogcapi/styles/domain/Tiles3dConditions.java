/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableTiles3dConditions.class)
public abstract class Tiles3dConditions {

  public abstract List<List<String>> getConditions();

  public abstract Map<String, Object> getExtensions();

  public abstract Map<String, Object> getExtras();

  @Value.Check
  public void check() {
    Preconditions.checkState(
        getConditions().stream().allMatch(cond -> cond.size() == 2),
        "Each condition must have two items, found: %s.",
        getConditions().stream().filter(cond -> cond.size() != 2).findFirst().get().size());
  }
}

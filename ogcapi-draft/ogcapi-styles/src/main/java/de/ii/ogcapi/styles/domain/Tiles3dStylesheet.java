/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableTiles3dStylesheet.class)
public abstract class Tiles3dStylesheet {

  public static final String SCHEMA_REF = "#/components/schemas/3dTilesStylesheet";

  // 'defines' is not yet supported

  // oneOf Tiles3dConditions, String, Boolean
  @Value.Default
  public Object getShow() {
    return true;
  }

  // oneOf Tiles3dConditions, String
  @Value.Default
  public Object getColor() {
    return "color('#FFFFFF')";
  }

  public abstract Map<String, Object> getMeta();

  public abstract Map<String, Object> getExtensions();

  public abstract Map<String, Object> getExtras();
}

/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.values.domain.StoredValue;
import de.ii.xtraplatform.values.domain.ValueBuilder;
import de.ii.xtraplatform.values.domain.ValueEncoding.FORMAT;
import de.ii.xtraplatform.values.domain.annotations.FromValueStore;
import de.ii.xtraplatform.values.domain.annotations.FromValueStore.FormatAlias;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, builder = "new", deepImmutablesDetection = true)
@FromValueStore(
    type = "3dtiles-styles",
    formatAliases = {@FormatAlias(extension = "3dtiles", format = FORMAT.JSON)})
@JsonDeserialize(builder = ImmutableTiles3dStylesheet.Builder.class)
public abstract class Tiles3dStylesheet implements StoredValue {

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

  abstract static class Builder implements ValueBuilder<Tiles3dStylesheet> {}
}

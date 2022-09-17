/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.flatgeobuf.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableFlatgeobufConfiguration.Builder.class)
public interface FlatgeobufConfiguration extends ExtensionConfiguration, PropertyTransformations {

  /**
   * @langEn Name of the property to use for the geometry. Multiple properties can be provided, the
   *     first property that has a value is used. The default value is the primary geometry
   *     property.
   * @langDe Name der Eigenschaft, die für die Geometrie verwendet werden soll. Es können mehrere
   *     Eigenschaften angegeben werden; die erste Eigenschaft, die einen Wert hat, wird verwendet.
   *     Der Standardwert ist die primäre Geometrieeigenschaft.
   * @default `{}`
   */
  List<String> getGeometryProperty();

  /**
   * @return If the data is flattened and the feature schema includes arrays, {@code
   *     maxMultiplicity} properties will be created for each array property. If an instance has
   *     more values in an array, only the first values are included in the data.
   */
  @Nullable
  Integer getMaxMultiplicity();

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableFlatgeobufConfiguration.Builder();
  }
}

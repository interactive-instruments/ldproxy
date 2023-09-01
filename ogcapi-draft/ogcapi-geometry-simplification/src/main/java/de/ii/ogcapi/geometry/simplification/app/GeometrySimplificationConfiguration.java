/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.geometry.simplification.app;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.geometry.simplification.app.ImmutableGeometrySimplificationConfiguration.Builder;
import de.ii.xtraplatform.docs.JsonDynamicSubType;
import org.immutables.value.Value;

/**
 * @buildingBlock GEOMETRY_SIMPLIFICATION
 * @examplesAll <code>
 * ```yaml
 * - buildingBlock: GEOMETRY_SIMPLIFICATION
 *   enabled: true
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDynamicSubType(superType = ExtensionConfiguration.class, id = "GEOMETRY_SIMPLIFICATION")
@JsonDeserialize(builder = Builder.class)
public interface GeometrySimplificationConfiguration extends ExtensionConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableGeometrySimplificationConfiguration.Builder();
  }
}

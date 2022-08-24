/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableTiles3dConfiguration.Builder.class)
public interface Tiles3dConfiguration extends ExtensionConfiguration {

  @Nullable
  Integer getFirstLevelWithContent();

  @Nullable
  Integer getMaxLevel();

  List<String> getContentFilters();

  List<String> getTileFilters();

  Float getGeometricErrorRoot();

  @Nullable
  Integer getSubtreeLevels();

  Optional<SeedingOptions> getSeedingOptions();

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableTiles3dConfiguration.Builder();
  }

  @Value.Check
  default void check() {
    Preconditions.checkState(
        Objects.requireNonNull(getMaxLevel()) <= 16,
        "The maximum level that is supported is 16. Found: {}.",
        getMaxLevel());
    Preconditions.checkState(
        getContentFilters().isEmpty()
            || getContentFilters().size() == getMaxLevel() - getFirstLevelWithContent() + 1,
        "The length of 'additionalFilters' must be the same as the levels with content. Found: {} and {}.",
        getContentFilters(),
        getMaxLevel() - getFirstLevelWithContent() + 1);
  }
}

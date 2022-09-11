/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tilematrixsets.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import java.util.List;
import org.immutables.value.Value;

/**
 * @langEn TODO
 * @langDe TODO
 * @example <code>
 * TODO
 * </code>
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableTileMatrixSetsConfiguration.Builder.class)
public interface TileMatrixSetsConfiguration extends ExtensionConfiguration, CachingConfiguration {

  enum TileCacheType {
    FILES,
    MBTILES,
    NONE
  }

  List<String> getIncludePredefined();

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableTileMatrixSetsConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableTileMatrixSetsConfiguration.Builder builder =
        ((ImmutableTileMatrixSetsConfiguration.Builder) source.getBuilder())
            .from(source)
            .from(this);

    return builder.build();
  }
}

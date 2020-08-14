/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.xtraplatform.entity.api.maptobuilder.BuildableBuilder;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableTilesConfiguration.Builder.class)
public interface TilesConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    Optional<String> getFeatureProvider();

    @Nullable
    Integer getLimit();

    @Nullable
    Integer getMaxPointPerTileDefault();

    @Nullable
    Integer getMaxLineStringPerTileDefault();

    @Nullable
    Integer getMaxPolygonPerTileDefault();

    @Nullable
    Boolean getMultiCollectionEnabled();

    @JsonMerge(value = OptBoolean.FALSE)
    @Nullable
    List<String> getFormats();

    @Nullable
    Map<String, MinMax> getSeeding();

    @Nullable
    Map<String, MinMax> getZoomLevels();

    @Nullable
    Map<String, List<PredefinedFilter>> getFilters();

    @Nullable
    double[] getCenter();

    @Override
    default Builder getBuilder() {
        return new ImmutableTilesConfiguration.Builder();
    }

    //TODO: this is a work-around for default from behaviour (map is not reset, which leads to duplicates in ImmutableMap)
    // try to find a better solution that also enables deep merges
    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        return ((ImmutableTilesConfiguration.Builder)source.getBuilder())
                     .from(source)
                     .from(this)
                     .seeding(getSeeding())
                     .zoomLevels(getZoomLevels())
                     .filters(getFilters())
                     .build();
    }
}
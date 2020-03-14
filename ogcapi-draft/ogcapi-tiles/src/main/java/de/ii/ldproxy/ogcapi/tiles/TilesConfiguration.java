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
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableTilesConfiguration.Builder.class)
public abstract class TilesConfiguration implements ExtensionConfiguration {

    @Value.Default
    @Override
    public boolean getEnabled() {
        return false;
    }

    public abstract Optional<String> getFeatureProvider();

    @Value.Default
    public boolean getMultiTilesEnabled() {
        return false;
    }

    @Value.Default
    public boolean getMultiCollectionEnabled() {
        return false;
    }

    @JsonMerge(value = OptBoolean.FALSE)
    @Nullable
    public abstract List<String> getFormats();

    @Nullable
    public abstract Map<String, MinMax> getSeeding();

    @Nullable
    public abstract Map<String, MinMax> getZoomLevels();

    @Nullable
    public abstract Map<String, List<PredefinedFilter>> getFilters();

    @Override
    public <T extends ExtensionConfiguration> T mergeDefaults(T baseConfiguration) {
        boolean enabled = this.getEnabled();
        Map<String, MinMax> seeding = this.getSeeding();
        Map<String, MinMax> zoomLevels = this.getZoomLevels();
        Map<String, List<PredefinedFilter>> predefFilters = this.getFilters();
        ImmutableTilesConfiguration.Builder configBuilder = new ImmutableTilesConfiguration.Builder().from(baseConfiguration);

        if (Objects.nonNull(enabled))
            configBuilder.enabled(enabled);
        if (Objects.nonNull(seeding))
            configBuilder.seeding(seeding);
        if (Objects.nonNull(zoomLevels))
            configBuilder.zoomLevels(zoomLevels);
        if (Objects.nonNull(predefFilters))
            configBuilder.filters(predefFilters);

        return (T) configBuilder.build();
    }

}
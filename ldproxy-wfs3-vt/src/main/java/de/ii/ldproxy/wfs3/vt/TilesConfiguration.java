/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableTilesConfiguration.Builder.class)
public abstract class TilesConfiguration implements ExtensionConfiguration {

    @Value.Default
    @Override
    public boolean getEnabled() {
        return false;
    }

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

    @Override
    public TilesConfiguration mergeDefaults(ExtensionConfiguration extensionConfigurationDefault) {

        //TODO
        /*return ImmutableTilesConfiguration.builder()
                .from(extensionConfigurationDefault)
                .from(this)
                .build();*/

        return this;
    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableMinMax.Builder.class)
    interface MinMax {
        int getMin();

        int getMax();
    }

}
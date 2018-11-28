/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationExtension;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableTilesConfiguration.class)
public abstract class TilesConfiguration implements FeatureTypeConfigurationExtension {

    public static final String EXTENSION_KEY = "tilesExtension";

    public abstract List<Tiles> getTiles();

    @Value.Immutable
    @Value.Modifiable
    @JsonDeserialize(as = ModifiableTiles.class)
    public static abstract class Tiles {

        public abstract int getId();

        @Value.Default
        public boolean getEnabled(){return true;}

        @JsonMerge(value = OptBoolean.FALSE)
        @Nullable
        public abstract List<String> getFormats();
        @Value.Immutable
        @Value.Modifiable
        @Value.Style(deepImmutablesDetection = true)
        @JsonDeserialize(as = ImmutableMinMax.class)
        public static abstract class MinMax{
            public abstract int getMin();
            public abstract int getMax();
        }
        @Nullable
        public abstract Map<String,MinMax> getSeeding();
        @Nullable
        public abstract Map<String,MinMax> getZoomLevels();

    }


}
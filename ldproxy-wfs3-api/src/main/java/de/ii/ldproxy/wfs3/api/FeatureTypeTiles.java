/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableFeatureTypeTiles.class)
public abstract class FeatureTypeTiles{



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
    public abstract LinkedHashMap<String,MinMax> getSeeding();
    @Nullable
    public abstract Map<String,MinMax> getZoomLevels();
}
/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import de.ii.xtraplatform.feature.transformer.api.TemporalExtent;
import org.immutables.value.Value;

import java.util.Map;
import javax.annotation.Nullable;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableFeatureTypeConfigurationWfs3.class)
public abstract class FeatureTypeConfigurationWfs3 extends FeatureTypeConfiguration {

    public abstract FeatureTypeExtent getExtent();

    public abstract Map<String, FeatureTypeConfigurationExtension> getExtensions();


    @Value.Immutable
    @Value.Modifiable
    @JsonDeserialize(as = ModifiableFeatureTypeExtent.class)
    public static abstract class FeatureTypeExtent {

        public abstract TemporalExtent getTemporal();

        @Nullable
        public abstract BoundingBox getSpatial();

        @Value.Default
        public boolean getSpatialComputed(){return false;}

    }

}


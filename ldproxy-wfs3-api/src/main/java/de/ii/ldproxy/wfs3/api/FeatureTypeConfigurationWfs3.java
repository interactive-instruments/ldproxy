/**
 * Copyright 2018 interactive instruments GmbH
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

    //TODO
    public static class FeatureTypeExtent {
        TemporalExtent temporal;

        BoundingBox spatial;

        public FeatureTypeExtent() {
        }

        public FeatureTypeExtent(TemporalExtent temporal, BoundingBox spatial) {
            this.temporal = temporal;
            this.spatial = spatial;
        }

        public TemporalExtent getTemporal() {
            return temporal;
        }

        public void setTemporal(TemporalExtent temporal) {
            this.temporal = temporal;
        }

        public BoundingBox getSpatial() {
            return spatial;
        }

        public void setSpatial(BoundingBox spatial) {
            this.spatial = spatial;
        }
    }
}
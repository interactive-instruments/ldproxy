/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilder;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueInstance;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;

/**
 * @author zahnen
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableFeatureTypeConfigurationOgcApi.Builder.class)
public interface FeatureTypeConfigurationOgcApi extends FeatureTypeConfiguration, ExtendableConfiguration, ValueInstance {

    abstract class Builder implements ValueBuilder<FeatureTypeConfigurationOgcApi> {}

    @Override
    default ImmutableFeatureTypeConfigurationOgcApi.Builder toBuilder() {
        return new ImmutableFeatureTypeConfigurationOgcApi.Builder().from(this);
    }

    CollectionExtent getExtent();

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableCollectionExtent.Builder.class)
    interface CollectionExtent {

        TemporalExtent getTemporal();

        @Nullable
        BoundingBox getSpatial();

        @Value.Default
        default boolean getSpatialComputed(){return false;}

        // TODO support computed temporal extent
        // TODO do not use EPOCH for open start
        // TODO do not use now for open end

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableTemporalExtent.Builder.class)
    interface TemporalExtent {

        @Value.Default
        default long getStart() {
            return 0;
        }

        @Value.Default
        default long getEnd() {
            return 0;
        }

        @JsonIgnore
        @Value.Derived
        default long getComputedEnd() {
            return getEnd() == 0 ? Instant.now().toEpochMilli() : getEnd();
        }

    }

}


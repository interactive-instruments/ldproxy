/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilder;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueInstance;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;


@Value.Immutable
@JsonDeserialize(builder = ImmutableFeatureTypeConfigurationOgcApi.Builder.class)
public interface FeatureTypeConfigurationOgcApi extends FeatureTypeConfiguration, ExtendableConfiguration, ValueInstance {

    abstract class Builder implements ValueBuilder<FeatureTypeConfigurationOgcApi> {}

    @Override
    default ImmutableFeatureTypeConfigurationOgcApi.Builder toBuilder() {
        return new ImmutableFeatureTypeConfigurationOgcApi.Builder().from(this);
    }

    Optional<String> getPersistentUriTemplate();

    CollectionExtent getExtent();

    @JsonProperty(value = "api")
    @JsonAlias(value = "capabilities")
    @Override
    List<ExtensionConfiguration> getExtensions();

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableCollectionExtent.Builder.class)
    interface CollectionExtent {

        @Nullable
        TemporalExtent getTemporal();

        @Nullable
        BoundingBox getSpatial();

        @Value.Default
        default boolean getSpatialComputed(){return false;}

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableTemporalExtent.Builder.class)
    interface TemporalExtent {

        @Value.Default
        @Nullable
        default Long getStart() {
            return null;
        }

        @Value.Default
        @Nullable
        default Long getEnd() {
            return null;
        }

        // TODO: temporal: support computed temporal extent
    }

}


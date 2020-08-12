/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.entities.domain.maptobuilder.Buildable;
import de.ii.xtraplatform.entities.domain.maptobuilder.BuildableBuilder;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;


@Value.Immutable
@JsonDeserialize(builder = ImmutableFeatureTypeConfigurationOgcApi.Builder.class)
public interface FeatureTypeConfigurationOgcApi extends FeatureTypeConfiguration, ExtendableConfiguration, Buildable<FeatureTypeConfigurationOgcApi> {

    abstract class Builder implements BuildableBuilder<FeatureTypeConfigurationOgcApi> {

        // jackson should append to instead of replacing extensions
        @JsonIgnore
        public abstract Builder extensions(Iterable<? extends ExtensionConfiguration> elements);

        @JsonProperty("api")
        public abstract Builder addAllExtensions(Iterable<? extends ExtensionConfiguration> elements);

    }

    @Override
    default ImmutableFeatureTypeConfigurationOgcApi.Builder getBuilder() {
        return new ImmutableFeatureTypeConfigurationOgcApi.Builder().from(this);
    }

    @Value.Default
    default boolean getEnabled() {
        return true;
    }

    Optional<String> getPersistentUriTemplate();

    Optional<CollectionExtent> getExtent();

    @JsonProperty(value = "api")
    @JsonAlias(value = "capabilities")
    @Override
    List<ExtensionConfiguration> getExtensions();

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableCollectionExtent.Builder.class)
    interface CollectionExtent {

        Optional<TemporalExtent> getTemporal();

        Optional<BoundingBox> getSpatial();

        @Value.Default
        default boolean getSpatialComputed() {
            return false;
        }

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

    @JsonIgnore
    List<ExtensionConfiguration> getParentExtensions();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    @Override
    default List<ExtensionConfiguration> getMergedExtensions() {
        return getMergedExtensions(Lists.newArrayList(Iterables.concat(getParentExtensions(), getExtensions())));
    }
}


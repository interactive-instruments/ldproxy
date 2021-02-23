/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@JsonDeserialize(builder = ImmutableFeaturesCoreConfiguration.Builder.class)
public interface FeaturesCoreConfiguration extends ExtensionConfiguration, FeatureTransformations {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    enum DefaultCrs {CRS84, CRS84h}

    int MINIMUM_PAGE_SIZE = 1;
    int DEFAULT_PAGE_SIZE = 10;
    int MAX_PAGE_SIZE = 10000;
    String PARAMETER_BBOX = "bbox";
    String PARAMETER_DATETIME = "datetime";
    String DATETIME_INTERVAL_SEPARATOR = "/";

    Optional<String> getFeatureProvider();

    Optional<String> getFeatureType();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Value.Default
    default List<String> getFeatureTypes() {
        return getFeatureType().isPresent() ? ImmutableList.of(getFeatureType().get()) : ImmutableList.of();
    }

    @Nullable
    DefaultCrs getDefaultCrs();

    @Nullable
    Integer getMinimumPageSize();

    @Nullable
    Integer getDefaultPageSize();

    @Nullable
    Integer getMaximumPageSize();

    @Nullable
    Boolean getShowsFeatureSelfLink();

    Optional<FeaturesCollectionQueryables> getQueryables();

    @Override
    Map<String, PropertyTransformation> getTransformations();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default ImmutableEpsgCrs getDefaultEpsgCrs() {
        return ImmutableEpsgCrs.copyOf(getDefaultCrs() == DefaultCrs.CRS84h ? OgcCrs.CRS84h : OgcCrs.CRS84);
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default Map<String, String> getAllFilterParameters() {
        if (getQueryables().isPresent()) {
            FeaturesCollectionQueryables queryables = getQueryables().get();
            ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();

            if (!queryables.getSpatial()
                           .isEmpty()) {
                builder.put(PARAMETER_BBOX, queryables.getSpatial()
                                                      .get(0));
            } else {
                builder.put(PARAMETER_BBOX, FeatureQueryTransformer.PROPERTY_NOT_AVAILABLE);
            }

            if (queryables.getTemporal()
                          .size() > 1) {
                builder.put(PARAMETER_DATETIME, String.format("%s%s%s", queryables.getTemporal()
                                                                                  .get(0), DATETIME_INTERVAL_SEPARATOR, queryables.getTemporal()
                                                                                                                                  .get(1)));
            } else if (!queryables.getTemporal()
                                  .isEmpty()) {
                builder.put(PARAMETER_DATETIME, queryables.getTemporal()
                                                          .get(0));
            } else {
                builder.put(PARAMETER_DATETIME, FeatureQueryTransformer.PROPERTY_NOT_AVAILABLE);
            }

            queryables.getSpatial()
                      .forEach(spatial -> builder.put(spatial, spatial));
            queryables.getTemporal()
                      .forEach(temporal -> builder.put(temporal, temporal));
            queryables.getOther()
                      .forEach(other -> builder.put(other, other));

            return builder.build();
        }

        return ImmutableMap.of(
                PARAMETER_BBOX, FeatureQueryTransformer.PROPERTY_NOT_AVAILABLE,
                PARAMETER_DATETIME, FeatureQueryTransformer.PROPERTY_NOT_AVAILABLE
        );
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default Map<String, String> getOtherFilterParameters() {
        if (getQueryables().isPresent()) {
            FeaturesCollectionQueryables queryables = getQueryables().get();
            ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();

            queryables.getOther()
                      .forEach(other -> builder.put(other, other));

            return builder.build();
        }

        return ImmutableMap.of();
    }

    @Override
    default Builder getBuilder() {
        return new ImmutableFeaturesCoreConfiguration.Builder().from(this);
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableFeaturesCoreConfiguration.Builder builder = new ImmutableFeaturesCoreConfiguration.Builder().from(source)
                                                                                                             .from(this);

        Map<String, PropertyTransformation> mergedTransformations = new LinkedHashMap<>(((FeaturesCoreConfiguration) source).getTransformations());
        getTransformations().forEach((key, transformation) -> {
            if (mergedTransformations.containsKey(key)) {
                mergedTransformations.put(key, transformation.mergeInto(mergedTransformations.get(key)));
            } else {
                mergedTransformations.put(key, transformation);
            }
        });
        builder.transformations(mergedTransformations);

        if (getQueryables().isPresent() && ((FeaturesCoreConfiguration) source).getQueryables().isPresent()) {
            builder.queryables(getQueryables().get().mergeInto(((FeaturesCoreConfiguration) source).getQueryables()
                                                                                                   .get()));
        }

        return builder.build();
    }
}

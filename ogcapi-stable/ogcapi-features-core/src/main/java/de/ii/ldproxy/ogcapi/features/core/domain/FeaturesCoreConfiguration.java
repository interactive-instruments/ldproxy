/**
 * Copyright 2020 interactive instruments GmbH
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
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableFeaturesCoreConfiguration.Builder.class)
public interface FeaturesCoreConfiguration extends ExtensionConfiguration, FeatureTransformations {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    List<String> ITEM_TYPES = ImmutableList.of("feature", "record");

    enum DefaultCrs {CRS84, CRS84h}
    enum ItemType {feature, record}

    int MINIMUM_PAGE_SIZE = 1;
    int DEFAULT_PAGE_SIZE = 10;
    int MAX_PAGE_SIZE = 10000;
    String PARAMETER_Q = "q";
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

    Optional<ItemType> getItemType();

    Optional<FeaturesCollectionQueryables> getQueryables();

    @Override
    Map<String, FeatureTypeMapping2> getTransformations();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default EpsgCrs getDefaultEpsgCrs() {
        return getDefaultCrs() == DefaultCrs.CRS84h ? OgcCrs.CRS84h : OgcCrs.CRS84;
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
                      .forEach(property -> builder.put(property, property));
            queryables.getTemporal()
                      .forEach(property -> builder.put(property, property));
            queryables.getQ()
                      .forEach(property -> builder.put(property, property));
            queryables.getOther()
                      .forEach(property -> builder.put(property, property));

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

            queryables.getQ()
                      .forEach(property -> builder.put(property, property));
            queryables.getOther()
                      .forEach(property -> builder.put(property, property));

            return builder.build();
        }

        return ImmutableMap.of();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default List<String> getQProperties() {
        if (getQueryables().isPresent()) {
            return getQueryables().get().getQ();
        }

        return ImmutableList.of();
    }

    default boolean hasDeprecatedQueryables() {
        return getQueryables().orElse(FeaturesCollectionQueryables.of())
                              .getAll()
                              .stream()
                              .anyMatch(key -> key.matches(".*\\[[^\\]]*\\].*"));
    }

    default List<String> normalizeQueryables(List<String> queryables, String collectionId) {
        return queryables.stream()
                         .map(queryable -> {
                             if (queryable.matches(".*\\[[^\\]]*\\].*")) {
                                 // TODO use info for now, but upgrade to warn after some time
                                 LOGGER.info("The queryable '{}' in collection '{}' uses a deprecated style that includes square brackets for arrays. The brackets have been dropped during hydration.", queryable, collectionId);
                                 return queryable.replaceAll("\\[[^\\]]*\\]", "");
                             }
                             return queryable;
                         })
                         .collect(Collectors.toUnmodifiableList());
    }

    default Optional<FeaturesCollectionQueryables> normalizeQueryables(String collectionId) {
        Optional<FeaturesCollectionQueryables> queryables = getQueryables();
        if (queryables.isPresent()) {
            List<String> spatial = normalizeQueryables(queryables.get().getSpatial(), collectionId);
            List<String> temporal = normalizeQueryables(queryables.get().getTemporal(), collectionId);
            List<String> q = normalizeQueryables(queryables.get().getQ(), collectionId);
            List<String> other = normalizeQueryables(queryables.get().getOther(), collectionId);
            queryables = Optional.of(new ImmutableFeaturesCollectionQueryables.Builder()
                                             .spatial(spatial)
                                             .temporal(temporal)
                                             .q(q)
                                             .other(other)
                                             .build());
        }
        return queryables;
    }

    default List<String> removeQueryables(List<String> queryables, Collection<String> queryablesToRemove) {
        return queryables.stream()
                         .filter(queryable -> !queryablesToRemove.contains(queryable))
                         .collect(Collectors.toUnmodifiableList());
    }

    default Optional<FeaturesCollectionQueryables> removeQueryables(Collection<String> queryablesToRemove) {
        Optional<FeaturesCollectionQueryables> queryables = getQueryables();
        if (queryables.isPresent()) {
            List<String> spatial = removeQueryables(queryables.get().getSpatial(), queryablesToRemove);
            List<String> temporal = removeQueryables(queryables.get().getTemporal(), queryablesToRemove);
            List<String> q = removeQueryables(queryables.get().getQ(), queryablesToRemove);
            List<String> other = removeQueryables(queryables.get().getOther(), queryablesToRemove);
            queryables = Optional.of(new ImmutableFeaturesCollectionQueryables.Builder()
                                             .spatial(spatial)
                                             .temporal(temporal)
                                             .q(q)
                                             .other(other)
                                             .build());
        }
        return queryables;
    }

    @Override
    default Builder getBuilder() {
        return new ImmutableFeaturesCoreConfiguration.Builder();
    }

}

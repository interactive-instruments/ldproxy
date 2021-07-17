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
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
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

    Map<String, Integer> getCoordinatePrecision();

    @Override
    Map<String, PropertyTransformation> getTransformations();

    List<Listener> getListeners();

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
            Map<String, String> parameters = new LinkedHashMap<>();

            if (!queryables.getSpatial()
                           .isEmpty()) {
                parameters.put(PARAMETER_BBOX, queryables.getSpatial()
                                                      .get(0));
            } else {
                parameters.put(PARAMETER_BBOX, FeatureQueryTransformer.PROPERTY_NOT_AVAILABLE);
            }

            if (queryables.getTemporal()
                          .size() > 1) {
                parameters.put(PARAMETER_DATETIME, String.format("%s%s%s", queryables.getTemporal()
                                                                                  .get(0), DATETIME_INTERVAL_SEPARATOR, queryables.getTemporal()
                                                                                                                                  .get(1)));
            } else if (!queryables.getTemporal()
                                  .isEmpty()) {
                parameters.put(PARAMETER_DATETIME, queryables.getTemporal()
                                                          .get(0));
            } else {
                parameters.put(PARAMETER_DATETIME, FeatureQueryTransformer.PROPERTY_NOT_AVAILABLE);
            }

            queryables.getSpatial()
                      .forEach(property -> parameters.put(property, property));
            queryables.getTemporal()
                      .forEach(property -> parameters.put(property, property));
            queryables.getQ()
                      .forEach(property -> parameters.put(property, property));
            queryables.getOther()
                      .forEach(property -> parameters.put(property, property));

            return parameters;
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
            Map<String, String> parameters = new LinkedHashMap<>();

            queryables.getQ()
                      .forEach(property -> parameters.put(property, property));
            queryables.getOther()
                      .forEach(property -> parameters.put(property, property));

            return parameters;
        }

        return ImmutableMap.of();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default List<String> getQProperties() {
        if (getQueryables().isPresent()) {
            return getQueryables().get()
                                  .getQ();
        }

        return ImmutableList.of();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
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
            List<String> spatial = normalizeQueryables(queryables.get()
                                                                 .getSpatial(), collectionId);
            List<String> temporal = normalizeQueryables(queryables.get()
                                                                  .getTemporal(), collectionId);
            List<String> q = normalizeQueryables(queryables.get()
                                                           .getQ(), collectionId);
            List<String> other = normalizeQueryables(queryables.get()
                                                               .getOther(), collectionId);
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
            List<String> spatial = removeQueryables(queryables.get()
                                                              .getSpatial(), queryablesToRemove);
            List<String> temporal = removeQueryables(queryables.get()
                                                               .getTemporal(), queryablesToRemove);
            List<String> q = removeQueryables(queryables.get()
                                                        .getQ(), queryablesToRemove);
            List<String> other = removeQueryables(queryables.get()
                                                            .getOther(), queryablesToRemove);
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

        if (getQueryables().isPresent() && ((FeaturesCoreConfiguration) source).getQueryables()
                                                                               .isPresent()) {
            builder.queryables(getQueryables().get()
                                              .mergeInto(((FeaturesCoreConfiguration) source).getQueryables()
                                                                                             .get()));
        }

        Map<String, Integer> mergedCoordinatePrecision = new LinkedHashMap<>(((FeaturesCoreConfiguration) source).getCoordinatePrecision());
        mergedCoordinatePrecision.putAll(getCoordinatePrecision());
        builder.coordinatePrecision(mergedCoordinatePrecision);

        return builder.build();
    }
}

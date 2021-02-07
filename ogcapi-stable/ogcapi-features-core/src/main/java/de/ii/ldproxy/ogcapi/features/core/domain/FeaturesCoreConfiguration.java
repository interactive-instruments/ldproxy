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
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.features.domain.SchemaBase.Type.BOOLEAN;
import static de.ii.xtraplatform.features.domain.SchemaBase.Type.DATETIME;
import static de.ii.xtraplatform.features.domain.SchemaBase.Type.FLOAT;
import static de.ii.xtraplatform.features.domain.SchemaBase.Type.GEOMETRY;
import static de.ii.xtraplatform.features.domain.SchemaBase.Type.INTEGER;
import static de.ii.xtraplatform.features.domain.SchemaBase.Type.STRING;
import static de.ii.xtraplatform.features.domain.SchemaBase.Type.VALUE_ARRAY;

@Value.Immutable
@Value.Style(builder = "new")
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

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default List<String> validateProperties(List<String> properties, String collectionId, FeatureSchema schema, Set<SchemaBase.Type> validTypes, String propertyType) {
        final List<String> propertyNames = SchemaInfo.getPropertyNames(schema, false);
        Map<String, SchemaBase.Type> propertyTypes = SchemaInfo.getPropertyTypes(schema, false);
        return new ImmutableList.Builder<String>()
                .addAll(properties.stream()
                                  // normalize property names
                                  .map(prop -> prop.replaceAll("\\[[^\\]]*\\]", ""))
                                  .filter(prop -> {
                                      if (!propertyNames.stream()
                                                        .anyMatch(schemaProperty -> schemaProperty.equals(prop))) {
                                          LOGGER.warn("The {} '{}' in collection '{}' has been removed, because the property was not found in the schema of feature type '{}'.", propertyType, prop, collectionId, schema.getName());
                                          return false;
                                      }
                                      return true;
                                  })
                                  .filter(prop -> {
                                      SchemaBase.Type type = propertyTypes.get(prop);
                                      if (Objects.isNull(type))
                                          return false;
                                      if (!validTypes.contains(type)) {
                                          LOGGER.warn("The {} '{}' in collection '{}' has been removed, because the property has type '{}', which is not one of the valid types: {}.", propertyType, prop, collectionId, type.toString(), validTypes.toString());
                                          return false;
                                      }
                                      return true;
                                  })
                                  .collect(Collectors.toUnmodifiableList()))
                .build();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default Optional<FeaturesCollectionQueryables> validateQueryables(String collectionId, FeatureSchema schema) {
        Optional<FeaturesCollectionQueryables> queryables = getQueryables();
        if (queryables.isPresent()) {
            List<String> spatial = validateProperties(queryables.get().getSpatial(), collectionId, schema, ImmutableSet.of(GEOMETRY), "spatial queryable");
            List<String> temporal = validateProperties(queryables.get().getTemporal(), collectionId, schema, ImmutableSet.of(DATETIME), "temporal queryable");
            List<String> other = validateProperties(queryables.get().getOther(), collectionId, schema, ImmutableSet.of(INTEGER, FLOAT, STRING, BOOLEAN, VALUE_ARRAY), "queryable");
            queryables = Optional.of(new ImmutableFeaturesCollectionQueryables.Builder()
                    .spatial(spatial)
                    .temporal(temporal)
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

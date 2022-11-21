/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.foundation.domain.TypedQueryParameter;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileGenerationUserParameters;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationSchema;
import de.ii.xtraplatform.features.domain.FeatureTypeConfiguration;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @langEn The collections that should be included. The parameter value is a comma-separated list of
 *     collection names.
 * @langDe Todo
 * @name collections
 * @endpoints collections
 */
@Singleton
@AutoBind
public class QueryParameterCollections extends ApiExtensionCache
    implements OgcApiQueryParameter,
        ConformanceClass,
        TypedQueryParameter<List<String>>,
        TileGenerationUserParameter {

  private final SchemaValidator schemaValidator;

  @Inject
  QueryParameterCollections(SchemaValidator schemaValidator) {
    this.schemaValidator = schemaValidator;
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of(
        "http://www.opengis.net/spec/ogcapi-tiles-1/0.0/conf/collection-selection");
  }

  @Override
  public String getName() {
    return "collections";
  }

  @Override
  public String getDescription() {
    return "The collections that should be included. The parameter value is a comma-separated list of collection names.";
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(),
        () ->
            isEnabledForApi(apiData)
                && method == HttpMethods.GET
                && definitionPath.equals(
                    "/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"));
  }

  private final Map<Integer, Schema<?>> schemaMap = new ConcurrentHashMap<>();
  private final Map<Integer, List<String>> collectionsMap = new ConcurrentHashMap<>();

  private List<String> getCollectionIds(OgcApiDataV2 apiData) {
    int apiHashCode = apiData.hashCode();
    if (!collectionsMap.containsKey(apiHashCode)) {
      collectionsMap.put(
          apiHashCode,
          apiData.getCollections().values().stream()
              .filter(collection -> apiData.isCollectionEnabled(collection.getId()))
              .filter(
                  collection ->
                      collection
                          .getExtension(TilesConfiguration.class)
                          .filter(ExtensionConfiguration::isEnabled)
                          .isPresent())
              .map(FeatureTypeConfiguration::getId)
              .collect(Collectors.toList()));
    }
    return collectionsMap.get(apiHashCode);
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    int apiHashCode = apiData.hashCode();
    if (!schemaMap.containsKey(apiHashCode)) {
      schemaMap.put(
          apiHashCode,
          new ArraySchema().items(new StringSchema()._enum(getCollectionIds(apiData))));
    }
    return schemaMap.get(apiHashCode);
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    Optional<TilesConfiguration> config = apiData.getExtension(TilesConfiguration.class);
    return config.isPresent()
        && config.get().isEnabled()
        && config.get().isMultiCollectionEnabled()
        && config.get().getTileProvider().requiresQuerySupport();
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TilesConfiguration.class;
  }

  @Override
  public List<String> parse(String value, OgcApiDataV2 apiData) {
    try {
      List<String> collections = getCollectionIds(apiData);
      return Splitter.on(',').omitEmptyStrings().trimResults().splitToList(value).stream()
          .filter(collections::contains)
          .collect(ImmutableList.toImmutableList());
    } catch (Throwable e) {
      throw new IllegalArgumentException(
          String.format("Invalid value for query parameter '%s'.", getName()), e);
    }
  }

  @Override
  public void applyTo(
      ImmutableTileGenerationUserParameters.Builder userParametersBuilder,
      QueryParameterSet parameters,
      Optional<TileGenerationSchema> generationSchema) {
    parameters.getValue(this).ifPresent(userParametersBuilder::addAllLayers);
  }
}

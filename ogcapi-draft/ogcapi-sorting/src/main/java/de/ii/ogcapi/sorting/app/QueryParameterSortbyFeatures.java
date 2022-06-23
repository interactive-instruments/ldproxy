/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.sorting.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.ItemTypeSpecificConformanceClass;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.sorting.domain.SortingConfiguration;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import de.ii.xtraplatform.features.domain.SortKey;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @langEn * `sortby` (resource *Features*): If the parameter is specified, the features are
 *     returned sorted according to the attributes specified in a comma-separated list. The
 *     attribute name can be preceded by `+` (ascending, the default behavior) or `-` (descending).
 *     Example: `sortby=type,-name`.
 * @langDe * `sortby` (Ressource "Features"): Ist der Parameter angegeben, werden die Features
 *     sortiert zurückgegeben. Sortiert wird nach den in einer kommaseparierten Liste angegebenen
 *     Attributen. Dem Attributnamen kann ein `+` (aufsteigend, das Standardverhalten) oder ein `-`
 *     (absteigend) vorangestellt werden. Beispiel: `sortby=type,-name`.
 * @name sortby
 * @endpoints Features, sortby
 */
@Singleton
@AutoBind
public class QueryParameterSortbyFeatures extends ApiExtensionCache
    implements OgcApiQueryParameter, ItemTypeSpecificConformanceClass {

  static final Splitter KEYS_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();
  private final SchemaValidator schemaValidator;

  @Inject
  QueryParameterSortbyFeatures(SchemaValidator schemaValidator) {
    this.schemaValidator = schemaValidator;
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();

    // TODO add feature-specific conformance class once we have a draft spec

    if (isItemTypeUsed(apiData, FeaturesCoreConfiguration.ItemType.record))
      builder.add("http://www.opengis.net/spec/ogcapi-records-1/0.0/conf/sorting");

    return builder.build();
  }

  @Override
  public String getId(String collectionId) {
    return "sortby_" + collectionId;
  }

  @Override
  public String getName() {
    return "sortby";
  }

  @Override
  public String getDescription() {
    return "Sort the results based on the properties identified by this parameter. "
        + "The parameter value is a comma-separated list of property names that can be used to sort results (sortables), "
        + "where each parameter name "
        + "may be preceeded by a '+' (ascending, default) or '-' (descending).";
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(),
        () ->
            apiData.getCollections().entrySet().stream()
                    .anyMatch(entry -> isEnabledForApi(apiData, entry.getKey()))
                && method == HttpMethods.GET
                && definitionPath.equals("/collections/{collectionId}/items"));
  }

  @Override
  public boolean isApplicable(
      OgcApiDataV2 apiData, String definitionPath, String collectionId, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName()
            + apiData.hashCode()
            + definitionPath
            + collectionId
            + method.name(),
        () ->
            isEnabledForApi(apiData, collectionId)
                && method == HttpMethods.GET
                && definitionPath.equals("/collections/{collectionId}/items"));
  }

  private final ConcurrentMap<Integer, ConcurrentMap<String, Schema<?>>> schemaMap =
      new ConcurrentHashMap<>();

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    int apiHashCode = apiData.hashCode();
    if (!schemaMap.containsKey(apiHashCode)) schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
    if (!schemaMap.get(apiHashCode).containsKey("*")) {
      List<String> sortables =
          apiData
              .getExtension(SortingConfiguration.class)
              .map(SortingConfiguration::getSortables)
              .orElse(ImmutableList.of());
      if (sortables.isEmpty())
        schemaMap.get(apiHashCode).put("*", new ArraySchema().items(new StringSchema()));
      else
        schemaMap
            .get(apiHashCode)
            .put(
                "*",
                new ArraySchema()
                    .items(
                        new StringSchema()
                            ._enum(
                                sortables.stream()
                                    .map(p -> ImmutableList.of(p, "+" + p, "-" + p))
                                    .flatMap(Collection::stream)
                                    .collect(Collectors.toUnmodifiableList()))));
    }
    return schemaMap.get(apiHashCode).get("*");
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
    int apiHashCode = apiData.hashCode();
    if (!schemaMap.containsKey(apiHashCode)) schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
    if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
      List<String> sortables =
          apiData
              .getCollections()
              .get(collectionId)
              .getExtension(SortingConfiguration.class)
              .map(SortingConfiguration::getSortables)
              .orElse(ImmutableList.of());
      schemaMap
          .get(apiHashCode)
          .put(
              collectionId,
              new ArraySchema()
                  .items(
                      new StringSchema()
                          ._enum(
                              sortables.stream()
                                  .map(p -> ImmutableList.of(p, "+" + p, "-" + p))
                                  .flatMap(Collection::stream)
                                  .collect(Collectors.toUnmodifiableList()))));
    }
    return schemaMap.get(apiHashCode).get(collectionId);
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return SortingConfiguration.class;
  }

  @Override
  public ImmutableFeatureQuery.Builder transformQuery(
      FeatureTypeConfigurationOgcApi featureTypeConfiguration,
      ImmutableFeatureQuery.Builder queryBuilder,
      Map<String, String> parameters,
      OgcApiDataV2 datasetData) {
    if (!isExtensionEnabled(
        datasetData.getCollections().get(featureTypeConfiguration.getId()),
        SortingConfiguration.class)) {
      return queryBuilder;
    }
    if (parameters.containsKey("sortby")) {
      // the validation against the schema has verified that only valid properties are listed
      for (String key : KEYS_SPLITTER.split(parameters.get("sortby"))) {
        if (key.startsWith("-")) {
          queryBuilder.addSortKeys(SortKey.of(key.substring(1), SortKey.Direction.DESCENDING));
        } else {
          if (key.startsWith("+")) key = key.substring(1);
          queryBuilder.addSortKeys(SortKey.of(key));
        }
      }
    }

    return queryBuilder;
  }
}

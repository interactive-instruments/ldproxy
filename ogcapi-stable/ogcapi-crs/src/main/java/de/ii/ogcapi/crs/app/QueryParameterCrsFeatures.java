/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crs.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.crs.domain.CrsConfiguration;
import de.ii.ogcapi.crs.domain.CrsSupport;
import de.ii.ogcapi.features.core.domain.FeatureQueryTransformer;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title crs
 * @endpoints Features, Feature
 * @langEn The coordinate reference system of the returned features. Default is WGS84
 *     longitude/latitude (with or without height).
 * @langDe Das Koordinatenreferenzsystem der zurückgegebenen Features. Default ist WGS84
 *     longitude/latitude (mit oder ohne Höhe).
 */
@Singleton
@AutoBind
public class QueryParameterCrsFeatures extends ApiExtensionCache
    implements OgcApiQueryParameter, ConformanceClass, FeatureQueryTransformer {

  public static final String CRS = "crs";
  public static final String CRS84 = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
  public static final String CRS84H = "http://www.opengis.net/def/crs/OGC/0/CRS84h";

  private final CrsSupport crsSupport;
  private final SchemaValidator schemaValidator;

  @Inject
  public QueryParameterCrsFeatures(CrsSupport crsSupport, SchemaValidator schemaValidator) {
    this.crsSupport = crsSupport;
    this.schemaValidator = schemaValidator;
  }

  @Override
  public String getId(String collectionId) {
    return CRS + "Features_" + collectionId;
  }

  @Override
  public String getName() {
    return CRS;
  }

  @Override
  public String getDescription() {
    return "The coordinate reference system of the response geometries. Default is WGS84 longitude/latitude (with or without height).";
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(),
        () ->
            isEnabledForApi(apiData)
                && method == HttpMethods.GET
                && (definitionPath.equals("/collections/{collectionId}/items")
                    || definitionPath.equals("/collections/{collectionId}/items/{featureId}")));
  }

  private final ConcurrentMap<Integer, ConcurrentMap<String, Schema<?>>> schemaMap =
      new ConcurrentHashMap<>();

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
    int apiHashCode = apiData.hashCode();
    if (!schemaMap.containsKey(apiHashCode)) schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
    if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
      List<String> crsList =
          crsSupport
              .getSupportedCrsList(apiData, apiData.getCollections().get(collectionId))
              .stream()
              .map(EpsgCrs::toUriString)
              .collect(ImmutableList.toImmutableList());
      String defaultCrs =
          apiData
              .getExtension(FeaturesCoreConfiguration.class, collectionId)
              .map(FeaturesCoreConfiguration::getDefaultEpsgCrs)
              .map(EpsgCrs::toUriString)
              .orElse(CRS84);
      schemaMap
          .get(apiHashCode)
          .put(collectionId, new StringSchema()._enum(crsList)._default(defaultCrs));
    }
    return schemaMap.get(apiHashCode).get(collectionId);
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return CrsConfiguration.class;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return super.isEnabledForApi(apiData)
        && apiData
            .getExtension(FeaturesCoreConfiguration.class)
            .map(ExtensionConfiguration::isEnabled)
            .orElse(true);
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return super.isEnabledForApi(apiData, collectionId)
        && apiData
            .getExtension(FeaturesCoreConfiguration.class, collectionId)
            .map(ExtensionConfiguration::isEnabled)
            .orElse(true);
  }

  @Override
  public ImmutableFeatureQuery.Builder transformQuery(
      ImmutableFeatureQuery.Builder queryBuilder,
      Map<String, String> parameters,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi featureTypeConfiguration) {

    if (isEnabledForApi(apiData, featureTypeConfiguration.getId()) && parameters.containsKey(CRS)) {
      EpsgCrs targetCrs;
      try {
        targetCrs = EpsgCrs.fromString(parameters.get(CRS));
      } catch (Throwable e) {
        throw new IllegalArgumentException(
            String.format("The parameter '%s' is invalid: %s", CRS, e.getMessage()), e);
      }
      if (!crsSupport.isSupported(apiData, featureTypeConfiguration, targetCrs)) {
        throw new IllegalArgumentException(
            String.format(
                "The parameter '%s' is invalid: the crs '%s' is not supported",
                CRS, targetCrs.toUriString()));
      }

      queryBuilder.crs(targetCrs);
    }

    return queryBuilder;
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-2/1.0/conf/crs");
  }
}

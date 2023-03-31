/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.geometry.simplification.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeatureQueryTransformer;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.math.BigDecimal;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title maxAllowableOffset
 * @endpoints Features, Feature
 * @langEn All geometries are simplified using the [Douglas Peucker
 *     algorithm](https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm).
 *     The value defines the maximum distance between original and simplified geometry ([Hausdorff
 *     distance](https://en.wikipedia.org/wiki/Hausdorff_distance)). The value has to use the unit
 *     of the given coordinate reference system (`CRS84` or the value of parameter `crs`).
 * @langDe Alle Geometrien werden mit dem
 *     [Douglas-Peucker-Algorithmus](https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm)
 *     vereinfacht. Der Wert von `maxAllowableOffset` legt den maximalen Abstand zwischen der
 *     Originalgeometrie und der vereinfachten Geometrie fest
 *     ([Hausdorff-Abstand](https://en.wikipedia.org/wiki/Hausdorff_distance)). Der Wert ist in den
 *     Einheiten des Koordinatenreferenzsystems der Ausgabe (`CRS84` bzw. der Wert des Parameters
 *     Query-Parameters `crs`) angegeben.
 */
@Singleton
@AutoBind
public class QueryParameterMaxAllowableOffsetFeatures extends ApiExtensionCache
    implements OgcApiQueryParameter, FeatureQueryTransformer {

  private final SchemaValidator schemaValidator;

  @Inject
  QueryParameterMaxAllowableOffsetFeatures(SchemaValidator schemaValidator) {
    this.schemaValidator = schemaValidator;
  }

  @Override
  public String getName() {
    return "maxAllowableOffset";
  }

  @Override
  public String getDescription() {
    return "This option can be used to specify the maxAllowableOffset to be used for simplifying the geometries in the response. "
        + "The maxAllowableOffset is in the units of the response coordinate reference system.";
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

  private final Schema<?> schema = new NumberSchema()._default(BigDecimal.valueOf(0)).example(0.05);

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    return schema;
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
    return schema;
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return GeometrySimplificationConfiguration.class;
  }

  @Override
  public ImmutableFeatureQuery.Builder transformQuery(
      ImmutableFeatureQuery.Builder queryBuilder,
      Map<String, String> parameters,
      OgcApiDataV2 datasetData,
      FeatureTypeConfigurationOgcApi featureTypeConfiguration) {
    if (!isExtensionEnabled(
        datasetData.getCollections().get(featureTypeConfiguration.getId()),
        GeometrySimplificationConfiguration.class)) {
      return queryBuilder;
    }
    if (parameters.containsKey("maxAllowableOffset")) {
      try {
        queryBuilder.maxAllowableOffset(Double.valueOf(parameters.get("maxAllowableOffset")));
      } catch (NumberFormatException e) {
        // ignore
      }
    }

    return queryBuilder;
  }
}

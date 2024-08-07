/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.app.QueryParameterResultTypeFeatures.ResultType;
import de.ii.ogcapi.features.core.domain.FeatureQueryParameter;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.EnumSchema;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.foundation.domain.SpecificationMaturity;
import de.ii.ogcapi.foundation.domain.TypedQueryParameter;
import de.ii.xtraplatform.features.domain.FeatureProvider;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery.Builder;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title result-type
 * @endpoints Features
 * @langEn This parameter is only enabled for SQL feature providers with `computeNumberMatched`
 *     enabled. One of `hits-only` and `items`. `items` returns the features, `hits-only` returns no
 *     features, but only the information about the number features matched by the request. For
 *     feature formats that do not support returning the number of matched features, `hits-only`
 *     will be rejected. `hitsOnly` is a deprecated alias for `hits-only`.
 * @langDe Dieser Parameter ist nur für SQL-Feature-Provider aktiviert, bei denen
 *     `computeNumberMatched` aktiviert ist. Werte sind `hits-only` und `items`. `items` gibt die
 *     Features zurück, `hits-only` gibt keine Features zurück, sondern nur die Information über die
 *     Anzahl der Features, die mit der Anfrage übereinstimmen. Bei Feature-Formaten, die die
 *     Rückgabe der Anzahl der übereinstimmenden Features nicht unterstützen, wird `hits-only`
 *     zurückgewiesen. `hitsOnly` ist eine veralteter Alias für `hits-only`.
 * @default `items`
 */
@Singleton
@AutoBind
public class QueryParameterResultTypeFeatures extends ApiExtensionCache
    implements OgcApiQueryParameter, TypedQueryParameter<ResultType>, FeatureQueryParameter {

  public enum ResultType {
    ITEMS,
    HITS_ONLY
  }

  private static final Schema<?> SCHEMA =
      new EnumSchema("hitsOnly", "hits-only", "items")._default("items");

  private final SchemaValidator schemaValidator;
  private final FeaturesCoreProviders providers;

  @Inject
  QueryParameterResultTypeFeatures(
      SchemaValidator schemaValidator, FeaturesCoreProviders providers) {
    this.schemaValidator = schemaValidator;
    this.providers = providers;
  }

  @Override
  public String getId() {
    return "resultTypeFeatures";
  }

  @Override
  public String getName() {
    return "result-type";
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return super.isEnabledForApi(apiData)
        && providers
            .getFeatureProvider(apiData)
            .map(FeatureProvider::supportsHitsOnly)
            .orElse(false);
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return super.isEnabledForApi(apiData)
        && apiData
            .getCollectionData(collectionId)
            .flatMap(
                cd ->
                    providers
                        .getFeatureProvider(apiData, cd)
                        .map(FeatureProvider::supportsHitsOnly))
            .orElse(
                providers
                    .getFeatureProvider(apiData)
                    .map(FeatureProvider::supportsHitsOnly)
                    .orElse(false));
  }

  @Override
  public ResultType parse(
      String value,
      Map<String, Object> typedValues,
      OgcApi api,
      Optional<FeatureTypeConfigurationOgcApi> optionalCollectionData) {
    if ("hits-only".equals(value) || "hitsOnly".equals(value)) {
      return ResultType.HITS_ONLY;
    }
    return null;
  }

  @Override
  public String getDescription() {
    return "One of `hits-only` and `items`. `items` returns the features, `hits-only` returns no features, but only the information about the number features matched by the request. For feature formats that do not support returning the number of matched features, `hits-only` will be rejected. `hitsOnly` is a deprecated alias for `hits-only`.";
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(),
        () ->
            isEnabledForApi(apiData)
                && method == HttpMethods.GET
                && definitionPath.equals("/collections/{collectionId}/items"));
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    return SCHEMA;
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return FeaturesCoreConfiguration.class;
  }

  @Override
  public void applyTo(
      Builder queryBuilder,
      QueryParameterSet parameters,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData) {
    parameters
        .getValue(this)
        .ifPresent(value -> queryBuilder.hitsOnly(Objects.equals(ResultType.HITS_ONLY, value)));
  }

  @Override
  public Optional<SpecificationMaturity> getSpecificationMaturity() {
    return Optional.of(SpecificationMaturity.DRAFT_LDPROXY);
  }
}

/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.filter.api;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.filter.domain.FilterConfiguration;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.foundation.domain.TypedQueryParameter;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title filter-lang
 * @endpoints Features, Vector Tile
 * @langEn Language of the query expression in the `filter` parameter. Supported are `cql2-text` and
 *     `cql2-json`, specified in the OGC candidate standard *Common Query Language (CQL2)*.
 *     `cql2-text` is an SQL-like text encoding for filter expressions that also supports spatial,
 *     temporal and array predicates. `cql2-json` is a JSON encoding of the same grammar, suitable
 *     for use as part of a JSON object that represents a query. The use of `cql2-text` is
 *     recommended for filter expressions in the `filter` parameter.
 * @langDe Sprache des Filterausdrucks im Parameter `filter`. Unterstützt werden `cql2-text` und
 *     `cql2-json`, spezifiziert im Entwurf des OGC-Standards *Common Query Language (CQL2)*.
 *     `cql2-text` ist eine SQL-ähnliche Textkodierung für Filterausdrücke, die auch räumliche,
 *     zeitliche und Array-Prädikate unterstützt. `cql2-json` ist eine JSON-Kodierung der gleichen
 *     Grammatik, geeignet für die Verwendung als Teil eines JSON-Objekts, das eine Query darstellt.
 *     Die Verwendung von `cql2-text` wird für Filterausdrücke im Parameter `filter` empfohlen.
 */
@Singleton
@AutoBind
public class QueryParameterFilterLang extends ApiExtensionCache
    implements OgcApiQueryParameter, TypedQueryParameter<Cql.Format> {

  static final String FILTER_LANG_CQL2_TEXT = "cql2-text";
  static final String FILTER_LANG_CQL2_JSON = "cql2-json";

  private final FeaturesCoreProviders providers;
  private final SchemaValidator schemaValidator;

  @Inject
  public QueryParameterFilterLang(
      FeaturesCoreProviders providers, SchemaValidator schemaValidator) {
    this.providers = providers;
    this.schemaValidator = schemaValidator;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return super.isEnabledForApi(apiData)
        && providers
            .getFeatureProvider(apiData)
            .filter(FeatureProvider2::supportsQueries)
            .map(FeatureProvider2::queries)
            .map(FeatureQueries::supportsCql2)
            .orElse(false);
  }

  @Override
  public String getName() {
    return "filter-lang";
  }

  @Override
  public String getDescription() {
    return "Language of the query expression in the 'filter' parameter. Supported are 'cql2-text' (default) and 'cql2-json', "
        + "specified in the OGC candidate standard 'Common Query Language (CQL2)'. 'cql2-text' is an SQL-like text encoding for "
        + "filter expressions that also supports spatial, temporal and array predicates. 'cql2-json' is a JSON encoding of "
        + "that grammar, suitable for use as part of a JSON object that represents a query. The use of 'cql2-text' is recommended "
        + "for filter expressions in the 'filter' parameter.";
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(),
        () ->
            isEnabledForApi(apiData)
                && method == HttpMethods.GET
                && (definitionPath.equals("/collections/{collectionId}/items")
                    || definitionPath.equals(
                        "/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
                    || definitionPath.equals(
                        "/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")));
  }

  @Override
  public Cql.Format parse(
      String value,
      Map<String, Object> typedValues,
      OgcApi api,
      Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    return Objects.equals(value, FILTER_LANG_CQL2_JSON) ? Cql.Format.JSON : Cql.Format.TEXT;
  }

  private final Schema<String> schema =
      new StringSchema()
          ._enum(ImmutableList.of(FILTER_LANG_CQL2_TEXT, FILTER_LANG_CQL2_JSON))
          ._default(FILTER_LANG_CQL2_TEXT);

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
    return FilterConfiguration.class;
  }
}

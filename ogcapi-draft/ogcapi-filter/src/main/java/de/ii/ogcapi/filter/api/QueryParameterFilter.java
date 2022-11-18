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
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.collections.queryables.domain.QueryablesConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.ItemTypeSpecificConformanceClass;
import de.ii.ogcapi.filter.domain.FilterConfiguration;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.tiles.domain.TileGenerationUserParameter;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileGenerationUserParameters;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationSchema;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.Cql.Format;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
// TODO
// @endpoints {@link de.ii.ogcapi.features.core.app.EndpointFeatures},{@link
// de.ii.ogcapi.tiles.infra.EndpointTileMultiCollection},{@link
// de.ii.ogcapi.tiles.infra.EndpointTileSingleCollection}

/**
 * @langEn The filter expression in the filter language declared in `filter-lang`. Coordinates are
 *     in the coordinate reference system declared in `filter-crs`.
 * @langDe Der Filterausdruck in der in `filter-lang` angegebenen Filtersprache. Die Koordinaten
 *     sind in dem in `filter-crs` angegebenen Koordinatenreferenzsystem.
 * @name filter
 * @endpoints Features, Vector Tile
 */
@Singleton
@AutoBind
public class QueryParameterFilter extends ApiExtensionCache
    implements OgcApiQueryParameter, ItemTypeSpecificConformanceClass, TileGenerationUserParameter {

  private final FeaturesCoreProviders providers;
  private final SchemaValidator schemaValidator;
  private final Cql cql;

  @Inject
  public QueryParameterFilter(
      FeaturesCoreProviders providers, SchemaValidator schemaValidator, Cql cql) {
    this.providers = providers;
    this.schemaValidator = schemaValidator;
    this.cql = cql;
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
    return "filter";
  }

  @Override
  public String getDescription() {
    return "Filter features in the collection using the query expression in the parameter value. Filter expressions "
        + "are written in the Common Query Language (CQL2), which is a candidate OGC standard. This API implements "
        + "the draft version from February 2022, which is a release candidate. "
        + "The language for this query parameter is CQL2 Text (`filter-lang=cql2-text`).\n\n"
        + "CQL2 Text expressions are similar to SQL expressions and also support spatial, temporal and array predicates. "
        + "All property references must be queryables of the collection and must be declared in the Queryables sub-resource "
        + "of the collection.\n\n"
        + "The following are examples of CQL Text expressions:\n\n"
        + "* Logical operators (`AND`, `OR`, `NOT`) are supported\n"
        + "* Simple comparison predicates (`=`, `<>`, `<`, `>`, `<=`, `>=`):\n"
        + "  * `address.LocalityName = 'Bonn'`\n"
        + "  * `measuredHeight > 10`\n"
        + "  * `storeysAboveGround <= 4`\n"
        + "  * `creationDate > '2017-12-31'`\n"
        + "  * `creationDate < '2018-01-01'`\n"
        + "  * `creationDate >= '2018-01-01' AND creationDate <= '2018-12-31'`\n"
        + "* Advanced comparison operators (`LIKE`, `BETWEEN`, `IN`, `IS NULL`):\n"
        + "  * `name LIKE '%Kirche%'`\n"
        + "  * `measuredHeight BETWEEN 10 AND 20`\n"
        + "  * `address.LocalityName IN ('Bonn', 'Köln', 'Düren')`\n"
        + "  * `address.LocalityName NOT IN ('Bonn', 'Köln', 'Düren')`\n"
        + "  * `name IS NULL`\n"
        + "  * `name IS NOT NULL`\n"
        + "* Spatial operators (the standard Simple Feature operators, e.g., `S_INTERSECTS`, `S_WITHIN`):\n"
        + "  * `S_INTERSECTS(bbox, POLYGON((8 52, 9 52, 9 53, 8 53, 8 52)))`\n"
        + "* Temporal operators (e.g., `T_AFTER`, `T_BEFORE`, `T_INTERSECTS`)\n"
        + "  * `T_AFTER(creationDate, DATE('2018-01-01'))`\n"
        + "  * `T_BEFORE(creationDate, DATE('2018-01-01'))`\n"
        + "  * `T_INTERSECTS(creationDate, INTERVAL('2018-01-01','2018-12-31'))`\n"
        + "  * `T_INTERSECTS(INTERVAL(begin,end), INTERVAL('2018-01-01','2018-12-31'))`\n\n"
        + "Warning: The final version of the Common Query Language standard may include changes to the "
        + "CQL2 Text language supported by this API.";
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    if (apiValidation == MODE.NONE) return ValidationResult.of();

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().mode(apiValidation);

    for (String collectionWithoutQueryables :
        api.getData().getCollections().entrySet().stream()
            .filter(
                entry ->
                    entry
                            .getValue()
                            .getExtension(getBuildingBlockConfigurationType())
                            .map(ExtensionConfiguration::isEnabled)
                            .orElse(false)
                        && !entry
                            .getValue()
                            .getExtension(QueryablesConfiguration.class)
                            .map(ExtensionConfiguration::isEnabled)
                            .orElse(false))
            .map(Map.Entry::getKey)
            .collect(Collectors.toUnmodifiableList())) {
      builder.addStrictErrors(
          MessageFormat.format(
              "The FILTER module is enabled for collection ''{0}'', but the QUERYABLES module is not enabled.",
              collectionWithoutQueryables));
    }

    return builder.build();
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
                        "/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
                    || definitionPath.equals(
                        "/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")));
  }

  private final Schema<String> schema = new StringSchema();

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
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();

    providers
        .getFeatureProvider(apiData)
        .ifPresent(
            provider -> {
              if (provider.supportsQueries() && provider.queries().supportsCql2()) {
                builder.add(
                    "http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/filter",
                    "http://www.opengis.net/spec/cql2/0.0/conf/basic-cql2",
                    "http://www.opengis.net/spec/cql2/0.0/conf/advanced-comparison-operators",
                    "http://www.opengis.net/spec/cql2/0.0/conf/case-insensitive-comparison",
                    "http://www.opengis.net/spec/cql2/0.0/conf/basic-spatial-operators",
                    "http://www.opengis.net/spec/cql2/0.0/conf/spatial-operators",
                    "http://www.opengis.net/spec/cql2/0.0/conf/temporal-operators",
                    "http://www.opengis.net/spec/cql2/0.0/conf/array-operators",
                    "http://www.opengis.net/spec/cql2/0.0/conf/property-property",
                    "http://www.opengis.net/spec/cql2/0.0/conf/cql2-text",
                    "http://www.opengis.net/spec/cql2/0.0/conf/cql2-json");
                if (provider.queries().supportsAccenti())
                  builder.add(
                      "http://www.opengis.net/spec/cql2/0.0/conf/accent-insensitive-comparison");
              }
            });

    if (isItemTypeUsed(apiData, FeaturesCoreConfiguration.ItemType.feature))
      builder.add("http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/features-filter");

    if (isItemTypeUsed(apiData, FeaturesCoreConfiguration.ItemType.record))
      builder.add(
          "http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/features-filter",
          "http://www.opengis.net/spec/ogcapi-records-1/0.0/req/cql-filter");

    return builder.build();
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return FilterConfiguration.class;
  }

  @Override
  public Set<String> getFilterParameters(
      Set<String> filterParameters, OgcApiDataV2 apiData, String collectionId) {
    if (!isEnabledForApi(apiData)) return filterParameters;

    return new ImmutableSet.Builder<String>().addAll(filterParameters).add(getName()).build();
  }

  @Override
  public void applyTo(
      ImmutableTileGenerationUserParameters.Builder userParametersBuilder,
      QueryParameterSet parameters,
      Optional<TileGenerationSchema> generationSchema) {
    if (parameters.getValues().containsKey(getName()) && generationSchema.isPresent()) {
      Cql.Format filterLang =
          parameters.getDefinitions().stream()
              .filter(def -> def instanceof QueryParameterFilterLang)
              .findFirst()
              .flatMap(
                  filterLangDef -> parameters.getValue((QueryParameterFilterLang) filterLangDef))
              .orElse(Format.TEXT);
      EpsgCrs filterCrs =
          parameters.getDefinitions().stream()
              .filter(def -> def instanceof QueryParameterFilterCrs)
              .findFirst()
              .flatMap(filterCrsDef -> parameters.getValue((QueryParameterFilterCrs) filterCrsDef))
              .orElse(OgcCrs.CRS84);

      Cql2Expression filter =
          parseFilter(
              parameters.getValues().get(getName()),
              filterLang,
              filterCrs,
              generationSchema.get().getProperties());

      userParametersBuilder.addFilters(filter);
    }
  }

  private Cql2Expression parseFilter(
      String filter, Format filterLang, EpsgCrs filterCrs, Map<String, String> propertyTypes) {
    Cql2Expression cql2Expression;
    try {
      cql2Expression = cql.read(filter, filterLang, filterCrs);
    } catch (Throwable e) {
      throw new IllegalArgumentException(
          String.format("The parameter '%s' is invalid", getName()), e);
    }

    List<String> invalidProperties =
        cql.findInvalidProperties(cql2Expression, propertyTypes.keySet());

    if (!invalidProperties.isEmpty()) {
      throw new IllegalArgumentException(
          String.format(
              "The parameter '%s' is invalid. Unknown or forbidden properties used: %s.",
              getName(), String.join(", ", invalidProperties)));
    }

    // will throw an error
    cql.checkTypes(cql2Expression, propertyTypes);

    return cql2Expression;
  }
}

/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.text.search.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Splitter;
import de.ii.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.text.search.domain.TextSearchConfiguration;
import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import de.ii.xtraplatform.cql.domain.Like;
import de.ii.xtraplatform.cql.domain.Or;
import de.ii.xtraplatform.cql.domain.ScalarLiteral;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @langEn TODO
 * @langDe TODO
 * @name q
 * @endpoints Features
 */
@Singleton
@AutoBind
public class QueryParameterQ extends ApiExtensionCache implements OgcApiQueryParameter {

  private final Schema<?> baseSchema;
  private final SchemaValidator schemaValidator;

  String PARAMETER_Q = "q";

  @Inject
  public QueryParameterQ(SchemaValidator schemaValidator) {
    this.schemaValidator = schemaValidator;
    this.baseSchema = new ArraySchema().items(new StringSchema());
  }

  @Override
  public String getName() {
    return "q";
  }

  @Override
  public String getDescription() {

    return "General text search in multiple text properties of the data. Separate search terms by comma."
        + "If at least one of the search terms is included in an item, it is included in the result set. "
        + "Known limitation: The search should be case-insensitive, but currently is case-sensitive.";
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

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return isExtensionEnabled(
        apiData.getCollections().get(collectionId),
        FeaturesCoreConfiguration.class,
        config ->
            !config.getQueryables().orElse(FeaturesCollectionQueryables.of()).getQ().isEmpty());
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    return baseSchema;
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
    return baseSchema;
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TextSearchConfiguration.class;
  }

  @Override
  public Map<String, String> transformParameters(
      FeatureTypeConfigurationOgcApi featureType,
      Map<String, String> parameters,
      OgcApiDataV2 apiData) {

    Map<String, String> filters = new LinkedHashMap<>();
    for (String filterKey : parameters.keySet()) {
      if (filterKey.equals(PARAMETER_Q)) {
        String filterValue = parameters.get(filterKey);
        filters.put(filterKey, filterValue);
        return filters;
      } else {
        throw new IllegalArgumentException("filterKey does not equal 'PARAMETER_Q'");
      }
    }

    return Map.of();
  }

  private Optional<Cql2Expression> getCQLFromParameterQ(
      Map<String, String> filters, List<String> qFields) {

    List<Cql2Expression> predicates =
        filters.entrySet().stream()
            .map(
                filter -> {
                  if (filter.getKey().equals(PARAMETER_Q)) {
                    return qToCql(qFields, filter.getValue()).orElse(null);
                  } else {
                    throw new IllegalArgumentException("filterKey does not equal 'PARAMETER_Q'");
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    return predicates.isEmpty()
        ? Optional.empty()
        : Optional.of(predicates.size() == 1 ? predicates.get(0) : And.of(predicates));
  }

  // Todo Used twice?
  private Optional<Cql2Expression> qToCql(List<String> qFields, String qValue) {
    // predicate that ORs LIKE operators of all values;
    List<String> qValues = Splitter.on(",").trimResults().splitToList(qValue);

    return qFields.size() > 1 || qValues.size() > 1
        ? Optional.of(
            Or.of(
                qFields.stream()
                    .map(
                        qField ->
                            qValues.stream()
                                .map(word -> Like.of(qField, ScalarLiteral.of("%" + word + "%")))
                                .collect(Collectors.toUnmodifiableList()))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toUnmodifiableList())))
        : Optional.of(Like.of(qFields.get(0), ScalarLiteral.of("%" + qValues.get(0) + "%")));
  }
}

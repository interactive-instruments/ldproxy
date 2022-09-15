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
import com.google.common.collect.ImmutableSet;
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
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.Cql.Format;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import de.ii.xtraplatform.cql.domain.Like;
import de.ii.xtraplatform.cql.domain.Or;
import de.ii.xtraplatform.cql.domain.ScalarLiteral;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

  private final Cql cql;

  String PARAMETER_Q = "q";

  @Inject
  public QueryParameterQ(SchemaValidator schemaValidator, Cql cql) {
    this.schemaValidator = schemaValidator;
    this.baseSchema = new ArraySchema().items(new StringSchema());
    this.cql = cql;
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
  public Set<String> getFilterParameters(
      Set<String> filterParameters, OgcApiDataV2 apiData, String collectionId) {
    return new ImmutableSet.Builder<String>().addAll(filterParameters).add("filter-q").build();
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {

    return ((isExtensionEnabled(
                apiData.getCollections().get(collectionId),
                FeaturesCoreConfiguration.class,
                config ->
                    !config
                        .getQueryables()
                        .orElse(FeaturesCollectionQueryables.of())
                        .getQ()
                        .isEmpty())
            || isExtensionEnabled(
                apiData.getCollections().get(collectionId),
                TextSearchConfiguration.class,
                config -> !config.getProperties().isEmpty()))
        && apiData
            .getExtension(getBuildingBlockConfigurationType())
            .map(ExtensionConfiguration::isEnabled)
            .orElse(false));
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

    if (parameters.containsKey(PARAMETER_Q)) {
      String filterValue = parameters.get(PARAMETER_Q);
      Optional<FeaturesCoreConfiguration> featuresCoreConfiguration =
          apiData.getExtension(FeaturesCoreConfiguration.class, featureType.getId());
      Optional<TextSearchConfiguration> textSearchConfiguration =
          apiData.getExtension(TextSearchConfiguration.class, featureType.getId());
      Set<String> qProperties = new HashSet<>();
      // TODO
      qProperties.addAll(textSearchConfiguration.get().getProperties());
      qProperties.addAll(featuresCoreConfiguration.get().getQProperties());
      List<String> finalQProperties = new ArrayList<>();
      finalQProperties.addAll(qProperties);
      Optional<Cql2Expression> cql = qToCql(finalQProperties, filterValue);
      if (cql.isPresent()) {
        Cql2Expression cql2 = cql.get();
        String s = this.cql.write(cql2, Format.TEXT);
        // TODO filter-q
        parameters.put("filter", s);
      }
    }

    return parameters;
  }

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

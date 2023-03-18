/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.queryables.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.collections.queryables.domain.ImmutableQueryablesConfiguration;
import de.ii.ogcapi.collections.queryables.domain.QueryablesConfiguration;
import de.ii.ogcapi.collections.queryables.domain.QueryablesConfiguration.PathSeparator;
import de.ii.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Feature Collections - Queryables
 * @langEn Metadata about the properties of the features in a feature collection that can be used in
 *     filter expressions.
 * @langDe Metadaten über die Eigenschaften von Objekten aus einer Feature Collection, die in
 *     Filter-Ausdrücken verwendet werden können.
 * @scopeEn The queryables are represented as a schema where each queryable is a property. The
 *     schema for each queryable is automatically derived from the definition of the property in the
 *     feature provider. Supported encodings are JSON Schema 2019-09 and HTML.
 * @scopeDe Die Queryables werden als Schema kodiert, wobei jede Queryable eine Objekteigenschaft
 *     ist. Das Schema für jede abfragbare Eigenschaft wird automatisch aus der Definition der
 *     Eigenschaft im Feature-Provider abgeleitet. Unterstützte Kodierungen sind JSON Schema 2019-09
 *     und HTML.
 * @conformanceEn *Feature Collections - Queryables* implements all requirements and recommendations
 *     of section 6.2 ("Queryables") of the [draft OGC API - Features - Part 3:
 *     Filtering](https://docs.ogc.org/DRAFTS/19-079r1.html#filter-queryables).
 * @conformanceDe Das Modul implementiert die Vorgaben und Empfehlungen aus Abschnitt 6.2
 *     ("Queryables") des [Entwurfs von OGC API - Features - Part 3:
 *     Filtering](https://docs.ogc.org/DRAFTS/19-079r1.html#filter-queryables).
 * @ref:cfg {@link de.ii.ogcapi.collections.queryables.domain.QueryablesConfiguration}
 * @ref:cfgProperties {@link
 *     de.ii.ogcapi.collections.queryables.domain.ImmutableQueryablesConfiguration}
 * @ref:endpoints {@link de.ii.ogcapi.collections.queryables.app.EndpointQueryables}
 * @ref:queryParameters {@link de.ii.ogcapi.collections.queryables.app.QueryParameterFQueryables}
 * @ref:pathParameters {@link
 *     de.ii.ogcapi.collections.queryables.app.PathParameterCollectionIdQueryables}
 */
@Singleton
@AutoBind
public class QueryablesBuildingBlock implements ApiBuildingBlock {

  static final List<String> VALID_TYPES =
      ImmutableList.of(
          "STRING", "DATE", "DATETIME", "INTEGER", "FLOAT", "BOOLEAN", "GEOMETRY", "VALUE_ARRAY");

  private final SchemaInfo schemaInfo;
  private final FeaturesCoreProviders providers;

  @Inject
  public QueryablesBuildingBlock(SchemaInfo schemaInfo, FeaturesCoreProviders providers) {
    this.schemaInfo = schemaInfo;
    this.providers = providers;
  }

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableQueryablesConfiguration.Builder()
        .enabled(false)
        .pathSeparator(PathSeparator.DOT)
        .enableEndpoint(true)
        .asQueryParameters(true)
        .build();
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    // get the configurations to process
    Map<String, QueryablesConfiguration> configs =
        api.getData().getCollections().entrySet().stream()
            .map(
                entry -> {
                  final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
                  final QueryablesConfiguration config =
                      collectionData.getExtension(QueryablesConfiguration.class).orElse(null);
                  if (Objects.isNull(config) || !config.isEnabled()) return null;
                  return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), config);
                })
            .filter(Objects::nonNull)
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    if (configs.isEmpty()) {
      // nothing to do
      return ValidationResult.of();
    }

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().mode(apiValidation);

    // check that the feature provider supports queries
    FeatureProvider2 provider = providers.getFeatureProviderOrThrow(api.getData());
    if (!provider.supportsQueries())
      builder.addErrors(
          MessageFormat.format(
              "Queryables is enabled, but the feature provider of the API '{0}' does not support queries.",
              provider.getData().getId()));

    // no additional operational checks for now, only validation; we can stop, if no validation is
    // requested
    if (apiValidation == ValidationResult.MODE.NONE) return builder.build();

    for (Map.Entry<String, QueryablesConfiguration> entry : configs.entrySet()) {
      // check that there is at least one queryable for each collection where queryables is enabled
      List<String> deprecatedQueryables =
          api.getData()
              .getExtension(FeaturesCoreConfiguration.class, entry.getKey())
              .flatMap(FeaturesCoreConfiguration::getQueryables)
              .map(FeaturesCollectionQueryables::getAll)
              .orElse(ImmutableList.of());
      if (entry.getValue().getIncluded().isEmpty() && deprecatedQueryables.isEmpty())
        builder.addStrictErrors(
            MessageFormat.format(
                "Queryables is enabled for collection ''{0}'', but no queryable property has been configured.",
                entry.getKey()));

      List<String> properties =
          schemaInfo.getPropertyNames(api.getData(), entry.getKey(), true, false);
      Optional<FeatureSchema> schema =
          providers.getFeatureSchema(
              api.getData(), api.getData().getCollections().get(entry.getKey()));
      if (schema.isEmpty())
        builder.addErrors(
            MessageFormat.format(
                "Queryables is enabled for collection ''{0}'', but no provider has been configured.",
                entry.getKey()));
      else {
        for (String queryable :
            Stream.concat(
                    deprecatedQueryables.stream(),
                    Stream.concat(
                        entry.getValue().getIncluded().stream(),
                        entry.getValue().getExcluded().stream()))
                .filter(v -> !"*".equals(v))
                .collect(Collectors.toUnmodifiableList())) {
          // does the collection include the queryable property?
          if (!properties.contains(queryable))
            builder.addErrors(
                MessageFormat.format(
                    "The Queryables configuration for collection ''{0}'' includes property ''{1}'', but the property does not exist.",
                    entry.getKey(), queryable));

          // check types
          schema.get().getAllNestedProperties().stream()
              .filter(property -> queryable.equals(property.getFullPathAsString()))
              .findFirst()
              .ifPresent(
                  property -> {
                    if (!VALID_TYPES.contains(property.getType().toString()))
                      builder.addErrors(
                          MessageFormat.format(
                              "The Queryables configuration for collection ''{0}'' includes a queryable property ''{1}'', but the property is not of a supported type. Found: ''{2}''.",
                              entry.getKey(), queryable, property.getType().toString()));
                  });
        }
      }
    }

    return builder.build();
  }
}

/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.schema.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import dagger.Lazy;
import de.ii.ogcapi.common.domain.PathParameterType;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class PathParameterTypeSchema extends PathParameterType {

  static final List<String> TYPES = ImmutableList.of("feature", "collection");
  static final String SCHEMA_TYPE_PATTERN = "[\\w\\-]+";

  private final Lazy<Set<QueriesHandlerSchema>> queriesHandlers;
  private final List<String> types;

  @Inject
  public PathParameterTypeSchema(
      ExtensionRegistry extensionRegistry,
      SchemaValidator schemaValidator,
      Lazy<Set<QueriesHandlerSchema>> queriesHandlers) {
    super(extensionRegistry, schemaValidator);
    this.queriesHandlers = queriesHandlers;
    this.types = new ArrayList<>();
  }

  @Override
  public String getId() {
    return "typeSchema";
  }

  @Override
  protected boolean isApplicablePath(OgcApiDataV2 apiData, String definitionPath) {
    return definitionPath.equals("/collections/{collectionId}/schemas/{type}");
  }

  @Override
  public List<String> getValues(OgcApiDataV2 apiData) {
    if (types.isEmpty()) {
      types.addAll(
          queriesHandlers.get().stream()
              .flatMap(handler -> handler.getSupportedTypes().stream())
              .collect(Collectors.toList()));
    }
    return types;
  }

  @Override
  public String getPattern() {
    return SCHEMA_TYPE_PATTERN;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return SchemaConfiguration.class;
  }
}

/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.search.domain.SearchConfiguration;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ApiHeader;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class HeaderPreferStoredQueriesManager extends ApiExtensionCache implements ApiHeader {

  private final Schema<?> schema =
      new StringSchema()._enum(ImmutableList.of("handling=strict", "handling=lenient"));
  private final SchemaValidator schemaValidator;

  @Inject
  HeaderPreferStoredQueriesManager(SchemaValidator schemaValidator) {
    this.schemaValidator = schemaValidator;
  }

  @Override
  public String getId() {
    return "Prefer";
  }

  @Override
  public String getDescription() {
    return "'handling=strict' creates or updates a stored query after successful validation and returns 400, "
        + "if validation fails. 'handling=lenient' (the default) creates or updates the query without validation.";
  }

  @Override
  public boolean isRequestHeader() {
    return true;
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(),
        () ->
            isEnabledForApi(apiData)
                && ((method == HttpMethods.PUT && definitionPath.equals("/search/{queryId}"))));
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    return schema;
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return isExtensionEnabled(
            apiData, SearchConfiguration.class, SearchConfiguration::isManagerEnabled)
        && isExtensionEnabled(
            apiData, SearchConfiguration.class, SearchConfiguration::isValidationEnabled);
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
    return Objects.nonNull(collectionData)
        && isExtensionEnabled(
            collectionData, SearchConfiguration.class, SearchConfiguration::isManagerEnabled)
        && isExtensionEnabled(
            collectionData, SearchConfiguration.class, SearchConfiguration::isValidationEnabled);
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return SearchConfiguration.class;
  }
}

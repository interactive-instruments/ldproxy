/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.xtraplatform.base.domain.AppContext;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Schema;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @langEn Debug option in development environments: Bare HTML output for feature pages.
 * @langDe Debug-Option in Entwicklungsumgebungen: "Nackte" HTML-Ausgabe für Features.
 * @name bare
 * @endpoints Feature
 */
@Singleton
@AutoBind
public class QueryParameterBareHtml extends ApiExtensionCache implements OgcApiQueryParameter {

  private final Schema<?> schema = new BooleanSchema()._default(false);
  private final boolean allowDebug;
  private final SchemaValidator schemaValidator;

  @Inject
  public QueryParameterBareHtml(AppContext appContext, SchemaValidator schemaValidator) {
    super();
    this.allowDebug = appContext.isDevEnv();
    this.schemaValidator = schemaValidator;
  }

  @Override
  public String getName() {
    return "bare";
  }

  @Override
  public String getDescription() {
    return "Debug option in development environments: Bare HTML output for feature pages.";
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(),
        () ->
            isEnabledForApi(apiData)
                && method == HttpMethods.GET
                && ("/collections/{collectionId}/items".equals(definitionPath)
                    || "/collections/{collectionId}/items/{featureId}".equals(definitionPath)));
  }

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
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return OgcApiQueryParameter.super.isEnabledForApi(apiData) && allowDebug;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return OgcApiQueryParameter.super.isEnabledForApi(apiData, collectionId) && allowDebug;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return HtmlConfiguration.class;
  }
}

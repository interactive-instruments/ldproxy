/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ApiSecurity;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FoundationConfiguration;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class QueryParameterToken extends ApiExtensionCache implements OgcApiQueryParameter {

  private Schema<?> schema;
  private final SchemaValidator schemaValidator;

  @Inject
  public QueryParameterToken(SchemaValidator schemaValidator) {
    super();
    this.schemaValidator = schemaValidator;
  }

  @Override
  public String getName() {
    return OAuthCredentialAuthFilter.OAUTH_ACCESS_TOKEN_PARAM;
  }

  @Override
  public String getDescription() {
    return "Set the authorization token for the request. If no value is provided, "
        + "the standard HTTP rules apply, i.e., the authorization header will be used to determine the token.";
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(),
        () -> isEnabledForApi(apiData) && method == HttpMethods.GET);
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    if (schema == null) {
      schema = new StringSchema();
    }
    return schema;
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData.getAccessControl().filter(ApiSecurity::isEnabled).isPresent();
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return FoundationConfiguration.class;
  }
}

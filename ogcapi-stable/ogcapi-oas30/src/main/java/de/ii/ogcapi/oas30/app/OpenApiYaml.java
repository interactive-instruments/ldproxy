/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.oas30.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.common.domain.ApiDefinitionFormatExtension;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.oas30.domain.Oas30Configuration;
import io.swagger.v3.oas.models.media.ObjectSchema;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @title YAML
 */
@Singleton
@AutoBind
public class OpenApiYaml implements ApiDefinitionFormatExtension {

  private static Logger LOGGER = LoggerFactory.getLogger(OpenApiYaml.class);
  private static ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "vnd.oai.openapi", ImmutableMap.of("version", "3.0")))
          .parameter("yaml")
          .label("YAML")
          .build();

  private final ExtendableOpenApiDefinition openApiDefinition;

  @Inject
  public OpenApiYaml(ExtendableOpenApiDefinition openApiDefinition) {
    this.openApiDefinition = openApiDefinition;
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  // always active, if OpenAPI 3.0 is active, since a service-desc link relation is mandatory
  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return Oas30Configuration.class;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    if (path.startsWith("/api/")) return null;

    return new ImmutableApiMediaTypeContent.Builder()
        .schema(new ObjectSchema())
        .schemaRef("#/components/schemas/objectSchema")
        .ogcApiMediaType(MEDIA_TYPE)
        .build();
  }

  @Override
  public Response getApiDefinitionResponse(
      OgcApiDataV2 apiData, ApiRequestContext apiRequestContext) {
    return openApiDefinition.getOpenApi(
        "yaml", apiRequestContext.getUriCustomizer().copy(), apiData);
  }

  @Override
  public Optional<String> getRel() {
    return Optional.of("service-desc");
  }
}

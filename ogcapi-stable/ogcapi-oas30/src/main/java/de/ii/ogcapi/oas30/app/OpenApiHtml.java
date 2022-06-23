/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.oas30.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.common.domain.ApiDefinitionFormatExtension;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.oas30.domain.Oas30Configuration;
import de.ii.xtraplatform.openapi.domain.OpenApiViewerResource;
import io.swagger.v3.oas.models.media.StringSchema;
import java.net.URISyntaxException;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @format HTML
 */
@Singleton
@AutoBind
public class OpenApiHtml implements ApiDefinitionFormatExtension {

  private static Logger LOGGER = LoggerFactory.getLogger(OpenApiHtml.class);
  private static ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(MediaType.TEXT_HTML_TYPE)
          .label("HTML")
          .parameter("html")
          .build();

  private final ExtendableOpenApiDefinition openApiDefinition;
  private final OpenApiViewerResource openApiViewerResource;

  @Inject
  public OpenApiHtml(
      ExtendableOpenApiDefinition openApiDefinition, OpenApiViewerResource openApiViewerResource) {
    this.openApiDefinition = openApiDefinition;
    this.openApiViewerResource = openApiViewerResource;
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  // always active, if OpenAPI 3.0 is active, since a service-doc link relation is mandatory
  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return Oas30Configuration.class;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    if (path.startsWith("/api/")) return null;

    return new ImmutableApiMediaTypeContent.Builder()
        .schema(new StringSchema().example("<html>...</html>"))
        .schemaRef("#/components/schemas/htmlSchema")
        .ogcApiMediaType(MEDIA_TYPE)
        .build();
  }

  @Override
  public Response getApiDefinitionResponse(
      OgcApiDataV2 apiData, ApiRequestContext apiRequestContext) {
    if (!apiRequestContext.getUriCustomizer().getPath().endsWith("/")) {
      try {
        return Response.status(Response.Status.MOVED_PERMANENTLY)
            .location(apiRequestContext.getUriCustomizer().copy().ensureTrailingSlash().build())
            .build();
      } catch (URISyntaxException ex) {
        throw new RuntimeException("Invalid URI: " + ex.getMessage(), ex);
      }
    }

    if (openApiViewerResource == null) {
      throw new NullPointerException(
          "The object to retrieve auxiliary files for the HTML API documentation is null, but should not be null.");
    }

    return openApiViewerResource.getFile("index.html");
  }

  @Override
  public Optional<String> getRel() {
    return Optional.of("service-doc");
  }
}

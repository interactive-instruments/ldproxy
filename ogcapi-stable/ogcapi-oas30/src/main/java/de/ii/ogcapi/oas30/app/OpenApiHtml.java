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
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.oas30.domain.Oas30Configuration;
import de.ii.xtraplatform.openapi.domain.OpenApiViewerResource;
import java.net.URISyntaxException;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

/**
 * @title HTML
 */
@Singleton
@AutoBind
public class OpenApiHtml implements ApiDefinitionFormatExtension {

  private final ExtendableOpenApiDefinition openApiDefinition;
  private final OpenApiViewerResource openApiViewerResource;

  @Inject
  public OpenApiHtml(
      ExtendableOpenApiDefinition openApiDefinition, OpenApiViewerResource openApiViewerResource) {
    this.openApiDefinition = openApiDefinition;
    this.openApiViewerResource = openApiViewerResource;
  }

  // always active, if OpenAPI 3.0 is active, since a service-doc link relation is mandatory
  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return Oas30Configuration.class;
  }

  @Override
  public ApiMediaType getMediaType() {
    return ApiMediaType.HTML_MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent() {
    return FormatExtension.HTML_CONTENT;
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

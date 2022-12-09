/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.oas30.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.ogcapi.foundation.domain.ClassSchemaCache;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.ExternalDocumentation;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.oas30.domain.OpenApiExtension;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AuthConfiguration;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.io.IOException;
import java.util.Comparator;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;

@Singleton
@AutoBind
public class ExtendableOpenApiDefinitionImpl implements ExtendableOpenApiDefinition {

  private final AuthConfiguration authConfig;
  private final ExtensionRegistry extensionRegistry;

  @Inject
  public ExtendableOpenApiDefinitionImpl(
      AppContext appContext,
      ExtensionRegistry extensionRegistry,
      ClassSchemaCache classSchemaCache) {
    this.authConfig = appContext.getConfiguration().getAuth();
    this.extensionRegistry = extensionRegistry;
  }

  @Override
  public Response getOpenApi(
      String type, URICustomizer requestUriCustomizer, OgcApiDataV2 apiData) {

    try {
      OpenAPI openAPI = initOpenAPI();

      addSecuritySchemes(apiData, openAPI);

      addServer(requestUriCustomizer, openAPI);

      addInfo(apiData, openAPI);

      addExternalDocs(apiData, openAPI);

      extensionRegistry.getExtensionsForType(OpenApiExtension.class).stream()
          .sorted(Comparator.comparing(OpenApiExtension::getSortPriority))
          .forEachOrdered(openApiExtension -> openApiExtension.process(openAPI, apiData));

      return getResponse(type, openAPI);
    } catch (IOException e) {
      throw new IllegalStateException("OpenAPI document could not be created", e);
    }
  }

  private OpenAPI initOpenAPI() throws IOException {
    return Json.mapper()
        .readerFor(OpenAPI.class)
        .readValue(
            Resources.asByteSource(
                    Resources.getResource(ExtendableOpenApiDefinitionImpl.class, "/openapi.json"))
                .openBufferedStream());
  }

  private void addSecuritySchemes(OgcApiDataV2 apiData, OpenAPI openAPI) {
    if (apiData.getSecured() && authConfig.isActive()) {
      openAPI
          .getComponents()
          .addSecuritySchemes(
              "JWT",
              new SecurityScheme()
                  .type(SecurityScheme.Type.HTTP)
                  .scheme("bearer")
                  .bearerFormat("JWT"));
    }
  }

  private Response getResponse(String type, OpenAPI openAPI) throws JsonProcessingException {
    boolean pretty = false;
    if (StringUtils.isNotBlank(type) && "yaml".equalsIgnoreCase(type.trim())) {
      return Response.status(Response.Status.OK)
          .entity(pretty ? Yaml.pretty(openAPI) : Yaml.mapper().writeValueAsString(openAPI))
          .type("application/vnd.oai.openapi;version=3.0")
          .build();
    } else {
      return Response.status(Response.Status.OK)
          .entity(pretty ? Json.pretty(openAPI) : Json.mapper().writeValueAsString(openAPI))
          .type("application/vnd.oai.openapi+json;version=3.0")
          .build();
    }
  }

  private void addExternalDocs(OgcApiDataV2 apiData, OpenAPI openAPI) {
    if (apiData.getExternalDocs().isPresent()) {
      ExternalDocumentation externalDocs = apiData.getExternalDocs().get();
      io.swagger.v3.oas.models.ExternalDocumentation docs =
          new io.swagger.v3.oas.models.ExternalDocumentation().url(externalDocs.getUrl());
      if (externalDocs.getDescription().isPresent()) {
        docs.description(externalDocs.getDescription().get());
      }
      openAPI.externalDocs(docs);
    }
  }

  private void addInfo(OgcApiDataV2 apiData, OpenAPI openAPI) {
    openAPI.getInfo().title(apiData.getLabel()).description(apiData.getDescription().orElse(""));

    if (apiData.getMetadata().isPresent()) {
      ApiMetadata md = apiData.getMetadata().get();
      openAPI.getInfo().version(md.getVersion().orElse("1.0.0"));
      if (md.getContactName().isPresent()
          || md.getContactUrl().isPresent()
          || md.getContactEmail().isPresent()) {
        Contact contact = new Contact();
        md.getContactName().ifPresent(contact::name);
        md.getContactUrl().ifPresent(contact::url);
        md.getContactEmail().ifPresent(contact::email);
        openAPI.getInfo().contact(contact);
      }
      if (md.getLicenseName().isPresent()) {
        // license name is required
        License license = new License().name(md.getLicenseName().get());
        md.getLicenseUrl().ifPresent(license::url);
        openAPI.getInfo().license(license);
      }
    } else {
      // version is required
      // see also https://github.com/interactive-instruments/ldproxy/issues/807
      openAPI.getInfo().version("1.0.0");
    }
  }

  private void addServer(URICustomizer requestUriCustomizer, OpenAPI openAPI) {
    openAPI.servers(
        ImmutableList.of(
            new Server()
                .url(
                    requestUriCustomizer
                        .copy()
                        .clearParameters()
                        .ensureNoTrailingSlash()
                        .removeLastPathSegment("api")
                        .toString())));
  }
}

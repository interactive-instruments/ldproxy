/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.oas30.app;

import static de.ii.ogcapi.foundation.domain.ApiSecurity.GROUP_PUBLIC;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.ogcapi.foundation.domain.ApiSecurity;
import de.ii.ogcapi.foundation.domain.ApiSecurity.ScopeGranularity;
import de.ii.ogcapi.foundation.domain.EndpointExtension;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.ExternalDocumentation;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.PermissionGroup;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.oas30.domain.OpenApiExtension;
import de.ii.xtraplatform.auth.domain.Oidc;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AuthConfiguration;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.servers.Server;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class ExtendableOpenApiDefinitionImpl implements ExtendableOpenApiDefinition {

  private static Logger LOGGER = LoggerFactory.getLogger(ExtendableOpenApiDefinitionImpl.class);

  private final AuthConfiguration authConfig;
  private final ExtensionRegistry extensionRegistry;
  private final Oidc oidc;

  @Inject
  public ExtendableOpenApiDefinitionImpl(
      AppContext appContext, ExtensionRegistry extensionRegistry, Oidc oidc) {
    this.authConfig = appContext.getConfiguration().getAuth();
    this.extensionRegistry = extensionRegistry;
    this.oidc = oidc;
  }

  @Override
  public Response getOpenApi(
      String type, URICustomizer requestUriCustomizer, OgcApiDataV2 apiData) {

    boolean pretty = true;

    try {
      OpenAPI openAPI =
          Json.mapper()
              .readerFor(OpenAPI.class)
              .readValue(
                  Resources.asByteSource(
                          Resources.getResource(
                              ExtendableOpenApiDefinitionImpl.class, "/openapi.json"))
                      .openBufferedStream());

      if (apiData.getAccessControl().filter(ApiSecurity::isEnabled).isPresent()) {
        if (oidc.isEnabled()) {
          Scopes scopes = new Scopes();

          if (!apiData.getAccessControl().get().getScopes().isEmpty()) {
            List<PermissionGroup> groups =
                extensionRegistry.getExtensionsForType(EndpointExtension.class).stream()
                    .filter(endpoint -> endpoint.isEnabledForApi(apiData))
                    .flatMap(
                        endpointExtension ->
                            endpointExtension
                                .getDefinition(apiData)
                                .getResources()
                                .values()
                                .stream())
                    .flatMap(ogcApiResource -> ogcApiResource.getOperations().values().stream())
                    .map(apiOperation -> apiOperation.getPermissionGroup())
                    .filter(
                        group ->
                            apiData
                                .getAccessControl()
                                .filter(apiSecurity -> apiSecurity.isRestricted(group.setOf()))
                                .isPresent())
                    .collect(Collectors.toList());

            Map<String, String> scopeGroups = new LinkedHashMap<>();

            for (ScopeGranularity scopeGranularity : apiData.getAccessControl().get().getScopes()) {
              switch (scopeGranularity) {
                case BASE:
                  groups.forEach(
                      group ->
                          scopeGroups.computeIfAbsent(
                              group.base().toString(),
                              name ->
                                  String.format(
                                      "includes %s",
                                      groups.stream()
                                          .filter(
                                              group1 -> Objects.equals(group.base(), group1.base()))
                                          .map(group1 -> group1.name())
                                          .distinct()
                                          .collect(Collectors.joining(", ")))));
                  break;
                case PARENT:
                  groups.forEach(
                      group ->
                          scopeGroups.computeIfAbsent(
                              group.group(),
                              name ->
                                  String.format(
                                      "includes %s",
                                      groups.stream()
                                          .filter(
                                              group1 ->
                                                  Objects.equals(group.group(), group1.group()))
                                          .map(group1 -> group1.name())
                                          .distinct()
                                          .collect(Collectors.joining(", ")))));
                  break;
                case MAIN:
                  groups.forEach(group -> scopeGroups.put(group.name(), group.description()));
                  break;
                case CUSTOM:
                  groups.stream()
                      .flatMap(
                          group ->
                              apiData.getAccessControl().get().getGroupsWith(group.setOf()).stream()
                                  .filter(group1 -> !Objects.equals(group1, GROUP_PUBLIC)))
                      .distinct()
                      .forEach(
                          group ->
                              scopeGroups.computeIfAbsent(
                                  group,
                                  name ->
                                      String.format(
                                          "includes %s",
                                          groups.stream()
                                              .filter(
                                                  group1 ->
                                                      apiData
                                                          .getAccessControl()
                                                          .get()
                                                          .getGroupsWith(group1.setOf())
                                                          .contains(group))
                                              .map(group1 -> group1.name())
                                              .distinct()
                                              .collect(Collectors.joining(", ")))));
                  break;
              }
            }

            scopeGroups.forEach(scopes::addString);
          }

          openAPI
              .getComponents()
              .addSecuritySchemes(
                  "Default",
                  new SecurityScheme()
                      .type(Type.OAUTH2)
                      .flows(
                          new OAuthFlows()
                              .authorizationCode(
                                  new OAuthFlow()
                                      .authorizationUrl(oidc.getLoginUri().toString())
                                      .tokenUrl(oidc.getTokenUri().toString())
                                      .scopes(scopes))));
        } else {
          openAPI
              .getComponents()
              .addSecuritySchemes(
                  "Default",
                  new SecurityScheme()
                      .type(SecurityScheme.Type.HTTP)
                      .scheme("bearer")
                      .bearerFormat("JWT"));
        }
      }

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

      if (apiData != null) {

        openAPI
            .getInfo()
            .title(apiData.getLabel())
            .description(apiData.getDescription().orElse(""));

        if (apiData.getMetadata().isPresent()) {
          ApiMetadata md = apiData.getMetadata().get();
          openAPI.getInfo().version(md.getVersion().orElse("1.0.0"));
          if (md.getContactName().isPresent()
              || md.getContactUrl().isPresent()
              || md.getContactEmail().isPresent()) {
            Contact contact = new Contact();
            md.getContactName().ifPresent(v -> contact.name(v));
            md.getContactUrl().ifPresent(v -> contact.url(v));
            md.getContactEmail().ifPresent(v -> contact.email(v));
            openAPI.getInfo().contact(contact);
          }
          if (md.getLicenseName().isPresent()) {
            // license name is required
            License license = new License().name(md.getLicenseName().get());
            md.getLicenseUrl().ifPresent(v -> license.url(v));
            openAPI.getInfo().license(license);
          }
        } else {
          // version is required
          openAPI.getInfo().version("1.0.0");
        }

        if (apiData.getExternalDocs().isPresent()) {
          ExternalDocumentation externalDocs = apiData.getExternalDocs().get();
          io.swagger.v3.oas.models.ExternalDocumentation docs =
              new io.swagger.v3.oas.models.ExternalDocumentation().url(externalDocs.getUrl());
          if (externalDocs.getDescription().isPresent())
            docs.description(externalDocs.getDescription().get());
          openAPI.externalDocs(docs);
        }
      }

      // TODO update with examples and details (f enums, lang enums, etc.)

      extensionRegistry.getExtensionsForType(OpenApiExtension.class).stream()
          .sorted(Comparator.comparing(OpenApiExtension::getSortPriority))
          .forEachOrdered(openApiExtension -> openApiExtension.process(openAPI, apiData));

      if (StringUtils.isNotBlank(type) && type.trim().equalsIgnoreCase("yaml")) {
        return Response.ok()
            .entity(pretty ? Yaml.pretty(openAPI) : Yaml.mapper().writeValueAsString(openAPI))
            .type(OpenApiYaml.MEDIA_TYPE.type())
            .build();
      } else {
        return Response.ok()
            .entity(pretty ? Json.pretty(openAPI) : Json.mapper().writeValueAsString(openAPI))
            .type(OpenApiJson.MEDIA_TYPE.type())
            .build();
      }
    } catch (IOException e) {
      throw new RuntimeException("OpenAPI document could not be created", e);
    }
  }
}

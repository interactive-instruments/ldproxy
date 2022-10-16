/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiHeader;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.auth.domain.User;
import io.dropwizard.auth.Auth;
import io.dropwizard.views.View;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class EndpointLogin extends EndpointSubCollection {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointLogin.class);
  private static final List<String> TAGS = ImmutableList.of("Mutate data");

  @Inject
  public EndpointLogin(ExtensionRegistry extensionRegistry) {
    super(extensionRegistry);
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return CrudConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null)
      formats =
          extensionRegistry.getExtensionsForType(FormatExtension.class).stream()
              .filter(ext -> ext instanceof CrudFormatHtml)
              .collect(Collectors.toList());
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    Optional<CrudConfiguration> config = apiData.getExtension(CrudConfiguration.class);
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("collections")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_CRUD);
    String path = "/collections/{collectionId}/login";
    HttpMethods method = HttpMethods.GET;
    List<OgcApiPathParameter> pathParameters =
        getPathParameters(extensionRegistry, apiData, "/collections/{collectionId}");
    List<OgcApiQueryParameter> queryParameters =
        getQueryParameters(extensionRegistry, apiData, path, method);
    List<ApiHeader> headers = List.of();
    String operationSummary = "login";
    Optional<String> operationDescription = Optional.empty();
    ImmutableOgcApiResourceData.Builder resourceBuilder =
        new ImmutableOgcApiResourceData.Builder().path(path).pathParameters(pathParameters);
    ApiOperation.getResource(
            apiData,
            path,
            false,
            queryParameters,
            headers,
            getContent(apiData, path),
            operationSummary,
            operationDescription,
            Optional.empty(),
            TAGS)
        .ifPresent(operation -> resourceBuilder.putOperations(method.name(), operation));
    definitionBuilder.putResources(path, resourceBuilder.build());

    path = "/collections/{collectionId}/callback";
    queryParameters = getQueryParameters(extensionRegistry, apiData, path, method);
    ImmutableOgcApiResourceData.Builder resourceBuilder2 =
        new ImmutableOgcApiResourceData.Builder().path(path).pathParameters(pathParameters);
    ApiOperation.getResource(
            apiData,
            path,
            false,
            queryParameters,
            headers,
            getContent(apiData, path),
            operationSummary,
            operationDescription,
            Optional.empty(),
            TAGS)
        .ifPresent(operation -> resourceBuilder2.putOperations(method.name(), operation));
    definitionBuilder.putResources(path, resourceBuilder2.build());

    return definitionBuilder.build();
  }

  private static final String IS_URI = "https://wso2.ldproxy.net";
  private static final String OIDC_URI = IS_URI + "/oauth2/oidcdiscovery";
  private static final String AUTH_URI = IS_URI + "/oauth2/token";

  @Path("/{collectionId}/login")
  @GET
  @Produces("text/html")
  public View getLogin(
      @Auth Optional<User> optionalUser,
      @PathParam("collectionId") String collectionId,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context HttpServletRequest request) {

    String callbackUri =
        requestContext
            .getUriCustomizer()
            .replaceInPath("/login", "/callback")
            .clearParameters()
            .toString();

    return new LoginView(OIDC_URI, AUTH_URI, callbackUri, "", false);
  }

  @Path("/{collectionId}/callback")
  @GET
  @Produces("text/html")
  public View getCallback(
      @Auth Optional<User> optionalUser,
      @PathParam("collectionId") String collectionId,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context HttpServletRequest request) {

    String callbackUri = requestContext.getUriCustomizer().clearParameters().toString();
    String redirectUri =
        requestContext
            .getUriCustomizer()
            .replaceInPath("/callback", "/crud")
            .clearParameters()
            .toString();

    return new LoginView(OIDC_URI, AUTH_URI, callbackUri, redirectUri, true);
  }
}

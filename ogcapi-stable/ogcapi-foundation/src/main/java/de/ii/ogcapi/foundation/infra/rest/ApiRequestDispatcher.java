/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.infra.rest;

import static de.ii.ogcapi.foundation.domain.ApiEndpointDefinition.SORT_PRIORITY_DUMMY;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ApiSecurity;
import de.ii.ogcapi.foundation.domain.EndpointExtension;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext.Builder;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.OgcApiResource;
import de.ii.ogcapi.foundation.domain.ParameterExtension;
import de.ii.ogcapi.foundation.domain.RequestInjectableContext;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.auth.domain.User.PolicyDecision;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.services.domain.ServiceEndpoint;
import de.ii.xtraplatform.services.domain.ServicesContext;
import io.dropwizard.auth.Auth;
import io.dropwizard.jetty.HttpConnectorFactory;
import java.net.URI;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;

@Singleton
@AutoBind
// @PermitAll
public class ApiRequestDispatcher implements ServiceEndpoint {

  private static final Set<String> NOCONTENT_METHODS =
      ImmutableSet.of("POST", "PUT", "DELETE", "PATCH");
  private static final ApiMediaType DEFAULT_MEDIA_TYPE = ApiMediaType.JSON_MEDIA_TYPE;

  private final ExtensionRegistry extensionRegistry;
  private final RequestInjectableContext ogcApiInjectableContext;
  private final URI servicesUri;
  private final ContentNegotiationMediaType contentNegotiationMediaType;
  private final ContentNegotiationLanguage contentNegotiationLanguage;
  private final int maxResponseLinkHeaderSize;

  @Inject
  ApiRequestDispatcher(
      AppContext appContext,
      ExtensionRegistry extensionRegistry,
      RequestInjectableContext ogcApiInjectableContext,
      ServicesContext servicesContext,
      ContentNegotiationMediaType contentNegotiationMediaType,
      ContentNegotiationLanguage contentNegotiationLanguage) {
    this.extensionRegistry = extensionRegistry;
    this.ogcApiInjectableContext = ogcApiInjectableContext;
    this.servicesUri = servicesContext.getUri();
    this.contentNegotiationMediaType = contentNegotiationMediaType;
    this.contentNegotiationLanguage = contentNegotiationLanguage;
    this.maxResponseLinkHeaderSize = getMaxResponseHeaderSize(appContext) / 4;
  }

  @Override
  public String getServiceType() {
    return OgcApiDataV2.SERVICE_TYPE;
  }

  @Path("")
  public EndpointExtension dispatchLandingPageWithoutSlash(
      @PathParam("entrypoint") String entrypoint,
      @Context OgcApi service,
      @Context ContainerRequestContext requestContext,
      @Context Request request,
      @Auth Optional<User> optionalUser) {
    return dispatch("", service, requestContext, request, optionalUser);
  }

  @Path("/{entrypoint: [^/]*}")
  public EndpointExtension dispatch(
      @PathParam("entrypoint") String entrypoint,
      @Context OgcApi service,
      @Context ContainerRequestContext requestContext,
      @Context Request request,
      @Auth Optional<User> optionalUser) {

    String subPath = ((UriRoutingContext) requestContext.getUriInfo()).getFinalMatchingGroup();
    String method = requestContext.getMethod();

    EndpointExtension ogcApiEndpoint = findEndpoint(service.getData(), entrypoint, subPath, method);

    // Check request
    checkParameterNames(
        requestContext, service.getData(), ogcApiEndpoint, entrypoint, subPath, method);
    validateRequest(requestContext, service.getData(), ogcApiEndpoint, entrypoint, subPath, method);

    // Content negotiation
    ImmutableSet<ApiMediaType> supportedMediaTypes =
        ogcApiEndpoint.getMediaTypes(service.getData(), subPath, method);
    ApiMediaType selectedMediaType = selectMediaType(requestContext, supportedMediaTypes, method);
    Locale selectedLanguage =
        contentNegotiationLanguage.negotiateLanguage(requestContext).orElse(Locale.ENGLISH);

    checkAuthorization(
        service.getData(), entrypoint, subPath, method, selectedMediaType, optionalUser);

    ApiRequestContext apiRequestContext =
        new Builder()
            .requestUri(requestContext.getUriInfo().getRequestUri())
            .request(request)
            .externalUri(getExternalUri())
            .mediaType(selectedMediaType)
            .alternateMediaTypes(getAlternateMediaTypes(selectedMediaType, supportedMediaTypes))
            .language(selectedLanguage)
            .api(service)
            .maxResponseLinkHeaderSize(maxResponseLinkHeaderSize)
            .build();

    ogcApiInjectableContext.inject(requestContext, apiRequestContext);

    return ogcApiEndpoint;
  }

  @SuppressWarnings("PMD.CyclomaticComplexity")
  private void checkAuthorization(
      OgcApiDataV2 data,
      String entrypoint,
      String path,
      String method,
      ApiMediaType mediaType,
      Optional<User> optionalUser) {
    if (Objects.equals(entrypoint, "api")) {
      return;
    }
    if (mediaType.matches(MediaType.TEXT_HTML_TYPE)
        && (path.endsWith("/crud") || path.endsWith("/login") || path.endsWith("/callback"))) {
      return;
    }

    String requiredScope =
        List.of("POST", "PUT", "PATCH", "DELETE").contains(method)
            ? ApiSecurity.SCOPE_WRITE
            : ApiSecurity.SCOPE_READ;

    boolean isScopeRestricted =
        data.getAccessControl().filter(s -> s.isSecured(requiredScope)).isPresent();
    boolean userHasScope =
        optionalUser.filter(u -> u.getScopes().contains(requiredScope)).isPresent();
    boolean isPolicyDenial =
        optionalUser.filter(u -> u.getPolicyDecision() == PolicyDecision.DENY).isPresent();

    if (isScopeRestricted && (!userHasScope || isPolicyDenial)) {
      throw new NotAuthorizedException("Bearer realm=\"ldproxy\"");
    }
  }

  private void checkParameterNames(
      ContainerRequestContext requestContext,
      OgcApiDataV2 apiData,
      EndpointExtension ogcApiEndpoint,
      @SuppressWarnings("unused") String entrypoint,
      String subPath,
      String method) {
    if ("OPTIONS".equals(method)) {
      return;
    }
    if (ogcApiEndpoint.shouldIgnoreParameters(apiData, subPath, method)) {
      return;
    }
    Set<String> parameters = requestContext.getUriInfo().getQueryParameters().keySet();
    List<OgcApiQueryParameter> knownParameters =
        ogcApiEndpoint.getParameters(apiData, subPath, method);
    Set<String> unknownParameters =
        parameters.stream()
            .filter(
                parameter ->
                    knownParameters.stream()
                        .noneMatch(param -> param.getName().equalsIgnoreCase(parameter)))
            .collect(Collectors.toSet());
    if (!unknownParameters.isEmpty()) {
      throw new BadRequestException(
          "The following query parameters are rejected: "
              + String.join(", ", unknownParameters)
              + ". Valid parameters for this request are: "
              + knownParameters.stream()
                  .map(ParameterExtension::getName)
                  .collect(Collectors.joining(", ")));
    }
  }

  private void validateRequest(
      ContainerRequestContext requestContext,
      OgcApiDataV2 apiData,
      EndpointExtension ogcApiEndpoint,
      String entrypoint,
      String subPath,
      String method) {
    ApiEndpointDefinition apiDef = ogcApiEndpoint.getDefinition(apiData);
    if (!apiDef.getResources().isEmpty()) {
      // check that the subPath is valid
      OgcApiResource resource = apiDef.getResource("/" + entrypoint + subPath).orElse(null);
      if (resource == null) {
        throw new NotFoundException("The requested path is not a resource in this API.");
      }

      // no need to check the path parameters here, only the parent path parameters (service,
      // endpoint) are available;
      // path parameters in the sub-path have to be checked later
      ApiOperation operation = apiDef.getOperation(resource, method).orElse(null);
      if (operation == null) {
        throw notAllowedOrNotFound(getMethods(apiData, entrypoint, subPath));
      }

      Optional<String> collectionId = resource.getCollectionId(apiData);

      // validate query parameters
      requestContext
          .getUriInfo()
          .getQueryParameters()
          .forEach(
              (name, values) ->
                  operation.getQueryParameters().stream()
                      .filter(param -> param.getName().equalsIgnoreCase(name))
                      .forEach(
                          param -> {
                            Optional<String> result = param.validate(apiData, collectionId, values);
                            if (result.isPresent()) {
                              throw new BadRequestException(result.get());
                            }
                          }));
    }
  }

  private RuntimeException notAllowedOrNotFound(Set<String> methods) {
    if (methods.isEmpty()) {
      return new NotFoundException("The requested path is not a resource in this API.");
    } else {
      String first = methods.stream().findFirst().get();
      String[] more =
          methods.stream().filter(method -> !method.equals(first)).toArray(String[]::new);
      return new NotAllowedException(first, more);
    }
  }

  private ApiMediaType selectMediaType(
      ContainerRequestContext requestContext,
      Set<ApiMediaType> supportedMediaTypes,
      String method) {
    if (supportedMediaTypes.isEmpty() && NOCONTENT_METHODS.contains(method)) {
      return DEFAULT_MEDIA_TYPE;
    }

    return contentNegotiationMediaType
        .negotiateMediaType(requestContext, supportedMediaTypes)
        .orElseThrow(
            () ->
                new NotAcceptableException(
                    MessageFormat.format(
                        "The Accept header ''{0}'' does not match any of the supported media types for this resource: {1}.",
                        requestContext.getHeaderString("Accept"),
                        supportedMediaTypes.stream()
                            .map(mediaType -> mediaType.type().toString())
                            .collect(Collectors.toList()))));
  }

  private Set<ApiMediaType> getAlternateMediaTypes(
      ApiMediaType selectedMediaType, Set<ApiMediaType> mediaTypes) {
    return mediaTypes.stream()
        .filter(mediaType -> !Objects.equals(mediaType, selectedMediaType))
        .collect(ImmutableSet.toImmutableSet());
  }

  private EndpointExtension findEndpoint(
      OgcApiDataV2 dataset,
      @PathParam("entrypoint") String entrypoint,
      String subPath,
      String method) {
    if ("OPTIONS".equals(method)) {
      // special treatment for OPTIONS
      // check that the resource exists and in that case use the general endpoint for all OPTIONS
      // requests
      boolean resourceExists =
          getEndpoints().stream()
              .filter(endpoint -> endpoint.isEnabledForApi(dataset))
              .anyMatch(
                  endpoint -> {
                    ApiEndpointDefinition apiDef = endpoint.getDefinition(dataset);
                    if (apiDef.getSortPriority() != SORT_PRIORITY_DUMMY) {
                      return apiDef.matches("/" + entrypoint + subPath, null);
                    }
                    return false;
                  });
      if (!resourceExists) {
        throw new NotFoundException("The requested path is not a resource in this API.");
      }

      return getEndpoints().stream()
          .filter(endpoint -> endpoint.getClass() == OptionsEndpoint.class)
          .findAny()
          .orElseThrow(() -> notAllowedOrNotFound(getMethods(dataset, entrypoint, subPath)));
    }

    return getEndpoints().stream()
        .filter(endpoint -> endpoint.isEnabledForApi(dataset))
        .filter(
            endpoint -> {
              ApiEndpointDefinition apiDef = endpoint.getDefinition(dataset);
              if (apiDef != null && apiDef.getSortPriority() != SORT_PRIORITY_DUMMY) {
                return apiDef.matches("/" + entrypoint + subPath, method);
              }
              return false;
            })
        .findFirst()
        .orElseThrow(() -> notAllowedOrNotFound(getMethods(dataset, entrypoint, subPath)));
  }

  private Set<String> getMethods(
      OgcApiDataV2 dataset, @PathParam("entrypoint") String entrypoint, String subPath) {
    return getEndpoints().stream()
        .map(
            endpoint -> {
              ApiEndpointDefinition apiDef = endpoint.getDefinition(dataset);
              if (!apiDef.getResources().isEmpty()) {
                Optional<OgcApiResource> resource = apiDef.getResource("/" + entrypoint + subPath);
                return resource
                    .map(ogcApiResource -> ogcApiResource.getOperations().keySet())
                    .orElse(null);
              }
              return null;
            })
        .filter(Objects::nonNull)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  private List<EndpointExtension> getEndpoints() {
    return extensionRegistry.getExtensionsForType(EndpointExtension.class);
  }

  private Optional<URI> getExternalUri() {
    return Optional.of(servicesUri);
  }

  private static int getMaxResponseHeaderSize(AppContext appContext) {
    HttpConnectorFactory httpConnectorFactory =
        (HttpConnectorFactory)
            appContext.getConfiguration().getServerFactory().getApplicationConnectors().get(0);

    return (int) httpConnectorFactory.getMaxResponseHeaderSize().toBytes();
  }
}

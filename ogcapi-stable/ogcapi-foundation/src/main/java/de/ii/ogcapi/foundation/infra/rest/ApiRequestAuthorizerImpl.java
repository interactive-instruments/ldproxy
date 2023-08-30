/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.infra.rest;

import static de.ii.ogcapi.foundation.domain.ApiSecurity.GROUP_PUBLIC;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.Sets;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ApiSecurity;
import de.ii.ogcapi.foundation.domain.ApiSecurity.ScopeGranularity;
import de.ii.ogcapi.foundation.domain.ApiSecurityInfo;
import de.ii.ogcapi.foundation.domain.EndpointExtension;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.PermissionGroup;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.xtraplatform.auth.domain.SplitCookie;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.auth.domain.User.PolicyDecision;
import de.ii.xtraplatform.web.domain.LoginHandler;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class ApiRequestAuthorizerImpl implements ApiRequestAuthorizer, ApiSecurityInfo {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiRequestAuthorizerImpl.class);

  private final ExtensionRegistry extensionRegistry;

  @Inject
  ApiRequestAuthorizerImpl(ExtensionRegistry extensionRegistry) {
    this.extensionRegistry = extensionRegistry;
  }

  @Override
  public void checkAuthorization(
      ApiRequestContext requestContext,
      @Nullable ApiOperation apiOperation,
      Optional<User> optionalUser) {
    if (Objects.isNull(apiOperation)) {
      return;
    }

    OgcApiDataV2 data = requestContext.getApi().getData();

    if (data.getAccessControl().filter(ApiSecurity::isEnabled).isEmpty()) {
      return;
    }

    PermissionGroup permissionGroup = apiOperation.getPermissionGroup();
    String operationId = apiOperation.getOperationIdWithoutPrefix();
    Optional<String> collectionId = requestContext.getCollectionId();
    Set<String> requiredPermissions =
        getRequiredPermissions(permissionGroup, operationId, data.getId(), collectionId);
    Set<String> requiredScopes = getRequiredScopes(permissionGroup, data.getAccessControl().get());
    Set<String> activeScopes = getActiveScopes(data).keySet();
    ApiMediaType mediaType = requestContext.getMediaType();
    URI loginUri = getLoginUri(requestContext, activeScopes);

    if (isNotAuthorized(
        requestContext,
        data.getAccessControl().get(),
        optionalUser,
        requiredPermissions,
        requiredScopes,
        mediaType,
        loginUri,
        apiOperation)) {
      throw new NotAuthorizedException("Bearer realm=\"ldproxy\"");
    }
  }

  private static Set<String> getRequiredPermissions(
      PermissionGroup scope, String operationId, String apiId, Optional<String> collectionId) {
    if (collectionId.isPresent()) {
      return scope.setOf(operationId, apiId, collectionId.get());
    }

    return scope.setOf(operationId, apiId);
  }

  private static URI getLoginUri(ApiRequestContext requestContext, Set<String> activeScopes) {
    URIBuilder uriBuilder =
        new URICustomizer(requestContext.getExternalUri())
            .appendPath(LoginHandler.PATH_LOGIN)
            .addParameter(
                LoginHandler.PARAM_LOGIN_REDIRECT_URI,
                requestContext
                    .getUriCustomizer()
                    .removeParameter(OAuthCredentialAuthFilter.OAUTH_ACCESS_TOKEN_PARAM)
                    .toString());

    if (!activeScopes.isEmpty()) {
      uriBuilder.addParameter(LoginHandler.PARAM_LOGIN_SCOPES, String.join(" ", activeScopes));
    }

    return URI.create(uriBuilder.toString());
  }

  private static Set<String> getRequiredScopes(
      PermissionGroup permissionGroup, ApiSecurity apiSecurity) {
    if (apiSecurity.getScopes().isEmpty()) {
      return Set.of();
    }

    return permissionGroup.setOf(apiSecurity.getScopes());
  }

  private boolean isNotAuthorized(
      ApiRequestContext requestContext,
      ApiSecurity apiSecurity,
      Optional<User> optionalUser,
      Set<String> requiredPermissions,
      Set<String> requiredScopes,
      ApiMediaType mediaType,
      URI loginUri,
      ApiOperation apiOperation) {
    // TODO: move to bottom?
    if (isPolicyDenial(requestContext, apiSecurity, optionalUser, apiOperation)) {
      return true;
    }
    if (isAccessRestricted(apiSecurity, requiredPermissions)) {
      if (isNoUser(optionalUser, mediaType, loginUri)
          || isAudienceMismatch(apiSecurity, optionalUser)
          || isScopeMismatch(optionalUser, requiredScopes)
          || !hasUserPermission(apiSecurity, optionalUser, requiredPermissions)) {
        return true;
      }
    }

    return false;
  }

  private static boolean isAccessRestricted(
      ApiSecurity apiSecurity, Set<String> requiredPermissions) {
    return apiSecurity.isRestricted(requiredPermissions);
  }

  private boolean isPolicyDenial(
      ApiRequestContext requestContext,
      ApiSecurity apiSecurity,
      Optional<User> optionalUser,
      ApiOperation apiOperation) {
    boolean denial =
        optionalUser.filter(u -> u.getPolicyDecision() == PolicyDecision.DENY).isPresent();

    if (denial && LOGGER.isDebugEnabled()) {
      LOGGER.debug("Not authorized: policy denial");
    }

    return denial;
  }

  private static boolean isNoUser(
      Optional<User> optionalUser, ApiMediaType mediaType, URI loginUri) {
    if (optionalUser.isEmpty()) {
      if (mediaType.matches(MediaType.TEXT_HTML_TYPE)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Not logged in, redirecting");
        }
        List<String> authCookies =
            SplitCookie.deleteToken(loginUri.getHost(), Objects.equals(loginUri, "https"));

        ResponseBuilder response = Response.seeOther(loginUri);
        authCookies.forEach(cookie -> response.header("Set-Cookie", cookie));

        throw new WebApplicationException(response.build());
      }
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Not authorized: no valid token");
      }
      return true;
    }

    return false;
  }

  private static boolean isAudienceMismatch(ApiSecurity apiSecurity, Optional<User> optionalUser) {
    boolean isMismatch =
        !apiSecurity.getAudience().isEmpty()
            && !intersects(
                apiSecurity.getAudience(), optionalUser.map(User::getAudience).orElse(Set.of()));

    if (isMismatch && LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "Not authorized: token does not have any of these audiences {}",
          apiSecurity.getAudience());
    }

    return isMismatch;
  }

  private static boolean isScopeMismatch(Optional<User> optionalUser, Set<String> requiredScopes) {
    boolean isMismatch =
        !requiredScopes.isEmpty()
            && !intersects(requiredScopes, optionalUser.map(User::getScopes).orElse(Set.of()));

    if (isMismatch && LOGGER.isDebugEnabled()) {
      LOGGER.debug("Not authorized: token does not have any of these scopes {}", requiredScopes);
    }

    return isMismatch;
  }

  private static boolean hasUserPermission(
      ApiSecurity apiSecurity, Optional<User> optionalUser, Set<String> requiredPermissions) {
    Set<String> validPermissions =
        Sets.union(requiredPermissions, apiSecurity.getGroupsWith(requiredPermissions));

    boolean hasUserPermission =
        optionalUser
            .filter(u -> intersects(validPermissions, new HashSet<>(u.getPermissions())))
            .isPresent();

    if (!hasUserPermission && LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "Not authorized: user does not have any of these permissions/roles {}", validPermissions);
    }

    return hasUserPermission;
  }

  private static <T> boolean intersects(Set<T> first, Set<T> second) {
    return !Sets.intersection(first, second).isEmpty();
  }

  @Override
  public List<PermissionGroup> getActiveGroups(OgcApiDataV2 apiData) {
    return extensionRegistry.getExtensionsForType(EndpointExtension.class).stream()
        .filter(endpoint -> endpoint.isEnabledForApi(apiData))
        .flatMap(
            endpointExtension ->
                endpointExtension.getDefinition(apiData).getResources().values().stream())
        .flatMap(ogcApiResource -> ogcApiResource.getOperations().values().stream())
        .map(ApiOperation::getPermissionGroup)
        .filter(
            group ->
                apiData
                    .getAccessControl()
                    .filter(apiSecurity -> apiSecurity.isRestricted(group.setOf()))
                    .isPresent())
        .collect(Collectors.toList());
  }

  @Override
  public Map<String, String> getActiveScopes(OgcApiDataV2 apiData) {
    List<PermissionGroup> groups = getActiveGroups(apiData);

    Map<String, String> scopes = new LinkedHashMap<>();

    for (ScopeGranularity scopeGranularity : apiData.getAccessControl().get().getScopes()) {
      switch (scopeGranularity) {
        case BASE:
          groups.forEach(
              group ->
                  scopes.computeIfAbsent(
                      group.base().toString(),
                      name ->
                          String.format(
                              "includes %s",
                              groups.stream()
                                  .filter(group1 -> Objects.equals(group.base(), group1.base()))
                                  .map(group1 -> group1.name())
                                  .distinct()
                                  .collect(Collectors.joining(", ")))));
          break;
        case PARENT:
          groups.forEach(
              group ->
                  scopes.computeIfAbsent(
                      group.group(),
                      name ->
                          String.format(
                              "includes %s",
                              groups.stream()
                                  .filter(group1 -> Objects.equals(group.group(), group1.group()))
                                  .map(group1 -> group1.name())
                                  .distinct()
                                  .collect(Collectors.joining(", ")))));
          break;
        case MAIN:
          groups.forEach(group -> scopes.put(group.name(), group.description()));
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
                      scopes.computeIfAbsent(
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

    return scopes;
  }
}

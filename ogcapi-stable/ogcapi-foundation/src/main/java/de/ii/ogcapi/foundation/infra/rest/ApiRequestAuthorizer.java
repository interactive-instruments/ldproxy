/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.infra.rest;

import com.google.common.collect.Sets;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiSecurity;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.PermissionGroup;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.auth.domain.User.PolicyDecision;
import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ApiRequestAuthorizer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiRequestAuthorizer.class);

  static void checkAuthorization(
      OgcApiDataV2 data,
      String entrypoint,
      String subPath,
      ApiMediaType mediaType,
      URI loginUri,
      @Nullable ApiOperation apiOperation,
      Optional<User> optionalUser) {
    if (Objects.isNull(apiOperation)) {
      return;
    }

    PermissionGroup scope = apiOperation.getPermissionGroup();
    String operationId = apiOperation.getOperationIdWithoutPrefix();
    Optional<String> collectionId = getCollectionId(entrypoint, subPath);
    Set<String> requiredPermissions =
        getRequiredPermissions(scope, operationId, data.getId(), collectionId);

    if (isNotAuthorized(data, optionalUser, requiredPermissions, mediaType, loginUri)) {
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

  private static boolean isNotAuthorized(
      OgcApiDataV2 data,
      Optional<User> optionalUser,
      Set<String> requiredPermissions,
      ApiMediaType mediaType,
      URI loginUri) {
    if (isPolicyDenial(optionalUser)) {
      return true;
    }
    if (isAccessRestricted(data, requiredPermissions)) {
      if (optionalUser.isEmpty() && mediaType.matches(MediaType.TEXT_HTML_TYPE)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Not logged in, redirecting");
        }
        throw new WebApplicationException(Response.seeOther(loginUri).build());
      }
      if (isAudienceMismatch(data, optionalUser)
          || !hasUserPermission(data, optionalUser, requiredPermissions)) {
        return true;
      }
    }

    return false;
  }

  private static boolean isAccessRestricted(OgcApiDataV2 data, Set<String> requiredPermissions) {
    return data.getAccessControl().filter(s -> s.isRestricted(requiredPermissions)).isPresent();
  }

  private static boolean isPolicyDenial(Optional<User> optionalUser) {
    boolean denial =
        optionalUser.filter(u -> u.getPolicyDecision() == PolicyDecision.DENY).isPresent();

    if (denial && LOGGER.isDebugEnabled()) {
      LOGGER.debug("Not authorized: policy denial");
    }

    return denial;
  }

  private static boolean isAudienceMismatch(OgcApiDataV2 data, Optional<User> optionalUser) {
    boolean isMismatch =
        data.getAccessControl()
            .filter(
                s ->
                    !s.getAudience().isEmpty()
                        && !intersects(
                            s.getAudience(), optionalUser.map(User::getAudience).orElse(Set.of())))
            .isPresent();

    if (isMismatch && LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "Not authorized: token does not have any of these audiences {}",
          data.getAccessControl().map(ApiSecurity::getAudience).orElse(Set.of()));
    }

    return isMismatch;
  }

  private static boolean hasUserPermission(
      OgcApiDataV2 data, Optional<User> optionalUser, Set<String> requiredPermissions) {
    Set<String> validPermissions =
        Sets.union(
            requiredPermissions,
            data.getAccessControl()
                .map(s -> s.getGroupsWith(requiredPermissions))
                .orElse(Set.of()));

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

  private static Optional<String> getCollectionId(String entrypoint, String path) {
    if (!Objects.equals("collections", entrypoint) || path.isBlank()) {
      return Optional.empty();
    }
    int secondSlash = path.indexOf('/', 1);
    return Optional.of(path.substring(1, secondSlash > 1 ? secondSlash : path.length()));
  }

  private static <T> boolean intersects(Set<T> first, Set<T> second) {
    return !Sets.intersection(first, second).isEmpty();
  }
}

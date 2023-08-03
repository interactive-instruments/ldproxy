/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.infra.rest;

import com.google.common.collect.Sets;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiSecurity.Scope;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.auth.domain.User.PolicyDecision;
import de.ii.xtraplatform.base.domain.util.Tuple;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.ws.rs.NotAuthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ApiRequestAuthorizer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiRequestAuthorizer.class);

  static void checkAuthorization(
      OgcApiDataV2 data,
      String entrypoint,
      String subPath,
      @Nullable ApiOperation apiOperation,
      Optional<User> optionalUser) {
    if (Objects.isNull(apiOperation)) {
      return;
    }

    Tuple<Scope, String> scope = apiOperation.getScope();
    String operationId = apiOperation.getOperationIdWithoutPrefix();
    Optional<String> collectionId = getCollectionId(entrypoint, subPath);
    Set<String> requiredPermissions =
        getRequiredPermissions(scope, operationId, data.getId(), collectionId);

    if (isNotAuthorized(data, optionalUser, requiredPermissions)) {
      throw new NotAuthorizedException("Bearer realm=\"ldproxy\"");
    }
  }

  private static Set<String> getRequiredPermissions(
      Tuple<Scope, String> scope, String operationId, String apiId, Optional<String> collectionId) {
    Scope type = scope.first();
    String group = scope.second();

    if (collectionId.isPresent()) {
      return type.setOf(group, operationId, apiId, collectionId.get());
    }

    return type.setOf(group, operationId, apiId);
  }

  private static boolean isNotAuthorized(
      OgcApiDataV2 data, Optional<User> optionalUser, Set<String> requiredPermissions) {
    return isPolicyDenial(optionalUser)
        || (isAccessRestricted(data, requiredPermissions)
            && !hasUserPermission(data, optionalUser, requiredPermissions));
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

  private static boolean hasUserPermission(
      OgcApiDataV2 data, Optional<User> optionalUser, Set<String> requiredPermissions) {
    Set<String> validPermissions =
        Sets.union(
            requiredPermissions,
            data.getAccessControl().map(s -> s.getRolesWith(requiredPermissions)).orElse(Set.of()));

    boolean hasUserPermission =
        optionalUser
            .filter(
                u -> !Sets.intersection(validPermissions, new HashSet<>(u.getScopes())).isEmpty())
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
}

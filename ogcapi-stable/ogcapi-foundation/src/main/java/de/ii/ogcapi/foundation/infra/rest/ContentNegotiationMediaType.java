/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.infra.rest;

import de.ii.ogcapi.foundation.domain.ApiMediaType;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

public interface ContentNegotiationMediaType {
  Optional<ApiMediaType> negotiateMediaType(
      ContainerRequestContext requestContext, Set<ApiMediaType> supportedMediaTypes);

  Optional<ApiMediaType> negotiateMediaType(
      Request request,
      HttpHeaders httpHeaders,
      UriInfo uriInfo,
      Set<ApiMediaType> supportedMediaTypes);
}

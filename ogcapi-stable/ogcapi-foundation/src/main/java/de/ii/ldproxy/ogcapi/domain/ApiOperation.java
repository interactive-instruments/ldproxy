/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Value.Immutable
public interface ApiOperation {
    String getSummary();
    Optional<String> getDescription();
    Optional<ExternalDocumentation> getExternalDocs();
    Set<String> getTags();
    Optional<String> getOperationId();
    List<OgcApiQueryParameter> getQueryParameters();
    Optional<ApiRequestBody> getRequestBody();
    List<ApiHeader> getHeaders();
    Optional<ApiResponse> getSuccess();
    @Value.Default
    default boolean getHideInOpenAPI() { return false; }
}

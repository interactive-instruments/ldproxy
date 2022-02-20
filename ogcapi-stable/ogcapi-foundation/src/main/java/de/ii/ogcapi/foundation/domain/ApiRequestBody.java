/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import org.immutables.value.Value;

import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
public interface ApiRequestBody {
    default String getStatusCode() { return "200"; } ;
    Optional<String> getId(); // TODO set for reusable responses
    String getDescription();
    Map<MediaType, ApiMediaTypeContent> getContent();
    default boolean getRequired() { return false; }
}

/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.oas30.app;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;

import javax.ws.rs.core.Response;

public interface ExtendableOpenApiDefinition {
    Response getOpenApi(String type, URICustomizer requestUriCustomizer, OgcApiDataV2 apiData);
}

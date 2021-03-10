/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain;

import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;

import javax.ws.rs.core.Response;
import java.util.Optional;

public interface ApiDefinitionFormatExtension extends FormatExtension {

    default String getPathPattern() {
        return "^/api/?$";
    }

    Response getApiDefinitionResponse(OgcApiDataV2 apiData,
                                      ApiRequestContext apiRequestContext);

    default Response getApiDefinitionFile(OgcApiDataV2 apiData,
                                          ApiRequestContext apiRequestContext,
                                          String file) {
        throw new RuntimeException("Access to an auxiliary API definition file was requested for a format that does not support auxiliary files.");
    }

    default Optional<String> getRel() {
        return Optional.empty();
    }
}

/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import javax.ws.rs.core.Response;

public interface CommonFormatExtension extends FormatExtension {

    default String getPathPattern() {
        return "^/?(?:conformance(?:/)?)?$";
    }

    Response getLandingPageResponse(LandingPage apiLandingPage,
                                    OgcApiApi api, OgcApiRequestContext requestContext);

    Response getConformanceResponse(ConformanceDeclaration conformanceDeclaration,
                                    OgcApiApi api, OgcApiRequestContext requestContext);
}

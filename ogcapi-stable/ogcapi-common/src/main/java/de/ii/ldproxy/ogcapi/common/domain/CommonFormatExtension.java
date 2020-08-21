/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain;

import de.ii.ldproxy.ogcapi.domain.*;

public interface CommonFormatExtension extends GenericFormatExtension {

    /*
     * If a new subtype is created, add it to FORMAT_MAP in GenericFormatExtension.
     */

    @Override
    default String getPathPattern() {
        return "^/?(?:conformance(?:/)?)?$";
    }

    Object getLandingPageEntity(LandingPage apiLandingPage,
                                OgcApi api, ApiRequestContext requestContext);

    Object getConformanceEntity(ConformanceDeclaration conformanceDeclaration,
                                OgcApi api, ApiRequestContext requestContext);
}

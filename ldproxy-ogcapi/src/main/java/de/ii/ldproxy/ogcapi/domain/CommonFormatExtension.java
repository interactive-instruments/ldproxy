/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import javax.ws.rs.core.Response;
import java.util.List;

public interface CommonFormatExtension extends FormatExtension {

    default String getPathPattern() {
        return "^/?(?:conformance)?$";
    }

    Response getLandingPageResponse(Dataset dataset, OgcApiDataset api, OgcApiRequestContext requestContext);

    Response getConformanceResponse(List<ConformanceClass> ocgApiConformanceClasses,
                                    OgcApiDataset api, OgcApiRequestContext requestContext);
}

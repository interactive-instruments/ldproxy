/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;

import javax.ws.rs.core.Response;

public interface StylesFormatExtension extends FormatExtension {

    @Override
    default String getPathPattern() {
        return "^/styles(?:/\\w+)?/?$";
    }

    Response getStylesResponse(Styles styles,
                               OgcApiApi api,
                               OgcApiRequestContext requestContext);

    Response getStyleMetadataResponse(StyleMetadata metadata,
                                      OgcApiApi api,
                                      OgcApiRequestContext requestContext);

}

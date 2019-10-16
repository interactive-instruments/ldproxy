/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import javax.ws.rs.core.Response;

public interface CollectionsFormatExtension extends FormatExtension {

    @Override
    default String getPathPattern() {
        return "^/collections(?:/[\\w\\-]+)?/?$";
    }

    Response getCollectionsResponse(Collections collections,
                                    OgcApiDataset api,
                                    OgcApiRequestContext requestContext);

    Response getCollectionResponse(OgcApiCollection ogcApiCollection,
                                   OgcApiDataset api,
                                   OgcApiRequestContext requestContext);

}

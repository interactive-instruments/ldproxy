/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import de.ii.ldproxy.ogcapi.domain.*;

import javax.ws.rs.core.Response;

public interface Wfs3CollectionFormatExtension extends CommonFormatExtension {

    @Override
    default String getPathPattern() {
        return "^/?(?:conformance|collections(?:/\\w+)?)?$";
    }

    Response getCollectionsResponse(Dataset dataset, OgcApiDataset api,
                                    OgcApiRequestContext requestContext);

    Response getCollectionResponse(OgcApiCollection ogcApiCollection, String collectionName,
                                   OgcApiDataset api, OgcApiRequestContext requestContext);

}

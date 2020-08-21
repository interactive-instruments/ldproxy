/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.domain;

import de.ii.ldproxy.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;

public interface CollectionsFormatExtension extends GenericFormatExtension {

    @Override
    default String getPathPattern() {
        return "^/collections(?:/[\\w\\-]+)?/?$";
    }

    Object getCollectionsEntity(Collections collections,
                                    OgcApi api,
                                    ApiRequestContext requestContext);

    Object getCollectionEntity(OgcApiCollection ogcApiCollection,
                                   OgcApi api,
                                   ApiRequestContext requestContext);

}

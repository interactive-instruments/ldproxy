/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;

import static de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

@AutoMultiBind
public interface CollectionsFormatExtension extends GenericFormatExtension {

    @Override
    default String getPathPattern() {
        return "^/collections(?:/"+COLLECTION_ID_PATTERN+")?/?$";
    }

    Object getCollectionsEntity(Collections collections,
                                    OgcApi api,
                                    ApiRequestContext requestContext);

    Object getCollectionEntity(OgcApiCollection ogcApiCollection,
                                   OgcApi api,
                                   ApiRequestContext requestContext);

}

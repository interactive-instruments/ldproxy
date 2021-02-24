/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.queryables.app;

import de.ii.ldproxy.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaObject;

import java.util.List;

import static de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

public interface QueryablesFormatExtension extends GenericFormatExtension {

    default String getPathPattern() {
        return "^/?collections/"+COLLECTION_ID_PATTERN+"/queryables/?$";
    }

    Object getEntity(JsonSchemaObject schemaQueryables, List<Link> links, String collectionId, OgcApi api, ApiRequestContext requestContext);

}

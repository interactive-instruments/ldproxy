/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.schema;

import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaObject;

import static de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

public interface SchemaFormatExtension extends FormatExtension {

    default String getPathPattern() {
        return "^\\/?collections\\/"+COLLECTION_ID_PATTERN+"\\/schema/?$";
    }

    Object getEntity(JsonSchemaObject schema, String collectionId, OgcApi api, ApiRequestContext requestContext);
}

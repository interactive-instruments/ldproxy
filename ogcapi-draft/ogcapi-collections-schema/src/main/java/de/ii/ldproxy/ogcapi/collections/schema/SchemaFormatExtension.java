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

import java.util.Map;

public interface SchemaFormatExtension extends FormatExtension {

    default String getPathPattern() {
        return "^\\/?collections\\/[^\\/]+\\/schema/?$";
    }

    Object getEntity(Map<String,Object> schema, String collectionId, OgcApi api, ApiRequestContext requestContext);
}

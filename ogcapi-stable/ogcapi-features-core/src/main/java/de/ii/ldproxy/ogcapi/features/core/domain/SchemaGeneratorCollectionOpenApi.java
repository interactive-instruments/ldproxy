/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import io.swagger.v3.oas.models.media.Schema;

public interface SchemaGeneratorCollectionOpenApi {
    String getSchemaReference();

    Schema<?> getSchema();

    String getSchemaReference(String collectionId);

    Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId);

    String getSchemaReferenceForName(String name);

    Schema<?> getSchemaForName(String name);
}

/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.schema.domain;

import static de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

import de.ii.ogcapi.features.core.domain.JsonSchemaAbstractDocument;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApi;

public interface SchemaFormatExtension extends FormatExtension {

  default String getPathPattern() {
    return "^/?collections/"
        + COLLECTION_ID_PATTERN
        + "/schemas/"
        + PathParameterTypeSchema.SCHEMA_TYPE_PATTERN
        + "/?$";
  }

  Object getEntity(
      JsonSchemaAbstractDocument schema,
      String collectionId,
      OgcApi api,
      ApiRequestContext requestContext);
}

/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;

public interface ClassSchemaCache {
  default Schema<?> getSchema(Class<?> clazz) {
    return getSchema(clazz, null);
  }

  Schema<?> getSchema(Class<?> clazz, Class<?> referencingClazz);

  void registerSchema(Class<?> clazz, Schema<?> schema, List<Class<?>> referencedSchemas);

  Map<String, Schema<?>> getReferencedSchemas(Class<?> clazz);
}

/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to provide information for use in the API definition. Fields: {@code schemaId}.
 */
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Documented
@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE})
public @interface ApiInfo {

  /**
   * By default, the id of the schema component in the API definition is the simple class name.
   * A different id can be set using the {@code schemaId} field.
   */
  String schemaId();
}

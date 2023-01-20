/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Date;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableCaching.Builder.class)
public interface Caching {

  /**
   * @langEn TODO_DOCS
   * @langDe Für die Ressourcen in dem Modul wird der `Last-Modified` Header auf den konfigurierten
   *     Wert gesetzt. Der Wert überschreibt einen ggf. aus der Ressource bestimmten
   *     Änderungszeitpunkt.
   * @default null
   */
  @Nullable
  Date getLastModified();

  /**
   * @langEn TODO_DOCS
   * @langDe Für die Ressourcen in dem Modul wird der `Expires` Header auf den konfigurierten Wert
   *     gesetzt.
   * @default null
   */
  @Nullable
  Date getExpires();

  /**
   * @langEn TODO_DOCS
   * @langDe Für die Ressourcen in dem Modul wird der `Cache-Control` Header auf den konfigurierten
   *     Wert gesetzt. Ausnahme sind die *Features* und *Feature* Ressourcen, bei denen
   *     `cacheControlItems` zu verwenden ist.
   * @default null
   */
  @Nullable
  String getCacheControl();

  /**
   * @langEn TODO_DOCS
   * @langDe Für die *Features* und *Feature* Ressourcen wird der `Cache-Control` Header auf den
   *     konfigurierten Wert gesetzt.
   * @default null
   */
  @Nullable
  String getCacheControlItems();
}

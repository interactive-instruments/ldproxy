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
   * @langEn For the resources in the building block, the `Last-Modified` header is set to the
   *     configured value. The value overrides any modification time determined from the resource.
   * @langDe Für die Ressourcen in dem Modul wird der `Last-Modified` Header auf den konfigurierten
   *     Wert gesetzt. Der Wert überschreibt einen ggf. aus der Ressource bestimmten
   *     Änderungszeitpunkt.
   * @default null
   * @since v3.1
   */
  @Nullable
  Date getLastModified();

  /**
   * @langEn For the resources in the building block, the `Expires` header is set to the configured
   *     value.
   * @langDe Für die Ressourcen in dem Modul wird der `Expires` Header auf den konfigurierten Wert
   *     gesetzt.
   * @default null
   * @since v3.1
   */
  @Nullable
  Date getExpires();

  /**
   * @langEn For the resources in the building block, the `Cache-Control` header is set to the
   *     configured value. Exception are the *Features* and *Feature* resources, where
   *     `cacheControlItems` is to be used.
   * @langDe Für die Ressourcen in dem Modul wird der `Cache-Control` Header auf den konfigurierten
   *     Wert gesetzt. Ausnahme sind die *Features* und *Feature* Ressourcen, bei denen
   *     `cacheControlItems` zu verwenden ist.
   * @default null
   * @since v3.1
   */
  @Nullable
  String getCacheControl();

  /**
   * @langEn For the *Features* und *Feature* resources in the building block, the `Cache-Control`
   *     header is set to the configured value.
   * @langDe Für die *Features* und *Feature* Ressourcen wird der `Cache-Control` Header auf den
   *     konfigurierten Wert gesetzt.
   * @default null
   * @since v3.1
   */
  @Nullable
  String getCacheControlItems();
}

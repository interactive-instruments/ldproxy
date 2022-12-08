/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import org.immutables.value.Value;

@Value.Immutable
public interface LayerOptionsHttp extends LayerOptionsCommon {

  /**
   * @langEn URL template for accessing tiles. Parameters to use are `{{tileMatrix}}`,
   *     `{{tileRow}}`, `{{tileCol}}` and `{{fileExtension}}`.
   * @langDe URL-Template für den Zugriff auf Kacheln. Zu verwenden sind die Parameter
   *     `{{tileMatrix}}`, `{{tileRow}}`, `{{tileCol}}` und `{{fileExtension}}`.
   * @default `null`
   */
  String getUrlTemplate();
}

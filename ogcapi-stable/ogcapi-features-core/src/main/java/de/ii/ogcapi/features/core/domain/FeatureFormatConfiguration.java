/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.Map;

public interface FeatureFormatConfiguration extends PropertyTransformations {

  /**
   * @langEn Allows to change the default value of the [profile
   *     parameter](features.md#query-parameters) for a specific format. The key is the kind of
   *     profile, e.g. `rel` or `val`. The value is the default profile value, e.g. `rel-as-uri` or
   *     `val-as-title`. If only some or no profile kinds are set, the application defaults apply.
   * @langDe Erlaubt es, den Standardwert des [Profile-Parameters](features.md#query-parameter) für
   *     ein bestimmtes Format zu ändern. Der Schlüssel ist die Art des Profils, z. B. `rel` oder
   *     `val`. Der Wert ist der Standardprofilwert, z. B. `rel-as-uri` oder `val-as-title`. Wenn
   *     nur einige oder keine Profilarten festgelegt sind, gelten die Anwendungsstandards.
   * @since v4.2
   * @default {}
   */
  Map<String, String> getDefaultProfiles();
}

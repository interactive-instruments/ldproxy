/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import javax.annotation.Nullable;

public interface CachingConfiguration {

    /**
     * @en Sets fixed values for [HTTP-Caching-Header](general-rules.md#caching) for the resources.
     * @de Setzt feste Werte für [HTTP-Caching-Header](general-rules.md#caching) für die Ressourcen.
     * @default `{}`
     */
    @Nullable
    Caching getCaching();

}

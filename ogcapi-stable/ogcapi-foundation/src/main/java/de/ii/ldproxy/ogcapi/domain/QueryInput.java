/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import org.immutables.value.Value;

import java.util.Date;
import java.util.Optional;

public interface QueryInput {

    // general output options
    @Value.Default
    default boolean getIncludeLinkHeader() { return false; }

    Optional<Date> getLastModified();

    Optional<Date> getExpires();

    Optional<String> getCacheControl();
}

/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import org.immutables.value.Value;

@Value.Immutable
public abstract class OgcApiQueryInputGeneric implements OgcApiQueryInput {

    @Override
    @Value.Default
    public boolean getIncludeHomeLink() {
        return false;
    }

    @Override
    @Value.Default
    public boolean getIncludeLinkHeader() {
        return false;
    }
}

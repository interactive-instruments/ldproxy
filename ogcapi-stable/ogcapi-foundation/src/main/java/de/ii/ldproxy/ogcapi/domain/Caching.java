/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Date;

@Value.Immutable
@JsonDeserialize(builder = ImmutableCaching.Builder.class)
public interface Caching {

    @Nullable
    Date getLastModified();

    @Nullable
    Date getExpires();

    @Nullable
    String getCacheControl();

    @Nullable
    String getCacheControlItems();

}

/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableListener.Builder.class)
public interface Listener {
    // TODO generalize beyond PSQL, create proper subtypes
    // TODO currently need to add connection info here again, because the information is not available from other modules
    String getHost();
    String getDatabase();
    String getUser();
    String getPassword();
    String getType();
    String getChannel();
}

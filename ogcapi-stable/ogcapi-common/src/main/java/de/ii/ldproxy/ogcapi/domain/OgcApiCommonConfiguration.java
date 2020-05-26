/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableOgcApiCommonConfiguration.Builder.class)
public abstract class OgcApiCommonConfiguration implements ExtensionConfiguration {

    @Value.Default
    @Override
    public boolean getEnabled() {
        return true;
    }

    @Value.Default
    public boolean getIncludeHomeLink() { return false; }

    @Value.Default
    public boolean getUseLangParameter() { return false; }

    @Value.Default
    public boolean getIncludeLinkHeader() { return true; }

}

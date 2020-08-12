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

import javax.annotation.Nullable;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableOgcApiCommonConfiguration.Builder.class)
public interface OgcApiCommonConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Nullable
    Boolean getIncludeHomeLink();

    @Nullable
    Boolean getUseLangParameter();

    @Nullable
    Boolean getIncludeLinkHeader();

    @Override
    default Builder getBuilder() {
        return new ImmutableOgcApiCommonConfiguration.Builder();
    }

}

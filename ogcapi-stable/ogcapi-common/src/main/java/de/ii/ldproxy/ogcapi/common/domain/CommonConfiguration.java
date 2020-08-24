/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableCommonConfiguration.Builder.class)
public interface CommonConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Nullable
    Boolean getIncludeHomeLink();

    @Nullable
    Boolean getUseLangParameter();

    @Nullable
    Boolean getIncludeLinkHeader();

    List<String> getEncodings();

    @Override
    default Builder getBuilder() {
        return new ImmutableCommonConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableCommonConfiguration.Builder builder = ((ImmutableCommonConfiguration.Builder) source.getBuilder())
                .from(source)
                .from(this);

        //TODO: this is a work-around for default from behaviour (list is not reset, which leads to duplicates in the list of encodings)
        if (!getEncodings().isEmpty())
            builder.encodings(getEncodings());

        return builder.build();
    }
}
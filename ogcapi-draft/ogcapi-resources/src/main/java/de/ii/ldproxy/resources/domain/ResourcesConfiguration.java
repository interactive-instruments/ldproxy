/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableResourcesConfiguration.Builder.class)
public interface ResourcesConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Nullable
    Boolean getManagerEnabled();

    @Override
    default Builder getBuilder() {
        return new ImmutableResourcesConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableResourcesConfiguration.Builder builder = ((ImmutableResourcesConfiguration.Builder) source.getBuilder())
                .from(source)
                .from(this);

        return builder.build();
    }
}
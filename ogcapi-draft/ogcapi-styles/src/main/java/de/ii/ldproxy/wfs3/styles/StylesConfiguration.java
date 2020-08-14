/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableStylesConfiguration.Builder.class)
public interface StylesConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Nullable
    Boolean getManagerEnabled();

    @Nullable
    Boolean getMapsEnabled();

    @Nullable
    Boolean getValidationEnabled();

    @Nullable
    Boolean getResourcesEnabled();

    @Nullable
    Boolean getResourceManagerEnabled();

    @Nullable
    Boolean getHtmlEnabled();

    @Nullable
    Boolean getMbStyleEnabled();

    @Nullable
    Boolean getSld10Enabled();

    @Nullable
    Boolean getSld11Enabled();

    @Nullable
    Boolean getUseFormattedJsonOutput();

    @Override
    default Builder getBuilder() {
        return new ImmutableStylesConfiguration.Builder();
    }

}
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

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableStylesConfiguration.Builder.class)
public abstract class StylesConfiguration implements ExtensionConfiguration {

    @Value.Default
    @Override
    public boolean getEnabled() {
        return false;
    }

    @Value.Default
    public boolean getManagerEnabled() {
        return false;
    }

    @Value.Default
    public boolean getMapsEnabled() {
        return false;
    }

    @Value.Default
    public boolean getValidationEnabled() {
        return false;
    }

    @Value.Default
    public boolean getResourcesEnabled() {
        return false;
    }

    @Value.Default
    public boolean getResourceManagerEnabled() {
        return false;
    }

    @Value.Default
    public boolean getHtmlEnabled() {
        return false;
    }

    @Value.Default
    public boolean getMbStyleEnabled() {
        return false;
    }

    @Value.Default
    public boolean getSld10Enabled() {
        return false;
    }

    @Value.Default
    public boolean getSld11Enabled() {
        return false;
    }

    @Value.Default
    public boolean getUseFormattedJsonOutput() {
        return false;
    }

}

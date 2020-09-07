/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableStylesConfiguration.Builder.class)
public interface StylesConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    List<String> getStyleEncodings();

    @Nullable
    Boolean getStyleInfosOnCollection();

    @Nullable
    Boolean getManagerEnabled();

    @Nullable
    Boolean getValidationEnabled();

    @Nullable
    Boolean getResourcesEnabled();

    @Nullable
    Boolean getResourceManagerEnabled();

    @Nullable
    String getDefaultStyle();

    @Deprecated
    @Nullable
    Boolean getMapsEnabled();

    @Deprecated
    @Value.Derived
    @Nullable
    default Boolean getHtmlEnabled() { return getMapsEnabled(); }

    @Deprecated
    @Nullable
    Boolean getMbStyleEnabled();

    @Deprecated
    @Nullable
    Boolean getSld10Enabled();

    @Deprecated
    @Nullable
    Boolean getSld11Enabled();

    @Override
    default Builder getBuilder() {
        return new ImmutableStylesConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableStylesConfiguration.Builder builder = ((ImmutableStylesConfiguration.Builder) source.getBuilder())
                .from(source)
                .from(this);

        //TODO: this is a work-around for default from behaviour (list is not reset, which leads to duplicates in the list of encodings)
        if (!getStyleEncodings().isEmpty())
            builder.styleEncodings(getStyleEncodings());

        return builder.build();
    }
}
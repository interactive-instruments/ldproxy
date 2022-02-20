/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import de.ii.ldproxy.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableStylesConfiguration.Builder.class)
public interface StylesConfiguration extends ExtensionConfiguration, CachingConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    List<String> getStyleEncodings();

    @Nullable
    Boolean getManagerEnabled();

    @Nullable
    Boolean getValidationEnabled();

    @Nullable
    Boolean getUseIdFromStylesheet();

    @Deprecated
    @Nullable
    Boolean getResourcesEnabled();

    @Deprecated
    @Nullable
    Boolean getResourceManagerEnabled();

    @Deprecated(since = "3.1.0")
    @Nullable
    String getDefaultStyle();

    @Nullable
    Boolean getDeriveCollectionStyles();

    @Nullable
    Boolean getWebmapWithPopup();

    @Nullable
    Boolean getWebmapWithLayerControl();

    @Nullable
    Boolean getLayerControlAllLayers();

    @Override
    default Builder getBuilder() {
        return new ImmutableStylesConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableStylesConfiguration.Builder builder = ((ImmutableStylesConfiguration.Builder) source.getBuilder())
                .from(source)
                .from(this);

        List<String> styleEncodings = Lists.newArrayList(((StylesConfiguration) source).getStyleEncodings());
        getStyleEncodings().forEach(styleEncoding -> {
            if (!styleEncodings.contains(styleEncoding)) {
                styleEncodings.add(styleEncoding);
            }
        });
        builder.styleEncodings(styleEncodings);

        return builder.build();
    }
}
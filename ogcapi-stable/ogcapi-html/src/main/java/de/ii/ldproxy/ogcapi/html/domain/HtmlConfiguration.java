/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.html.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@Value.Style(builder = "new", attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableHtmlConfiguration.Builder.class)
public interface HtmlConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Nullable
    Boolean getNoIndexEnabled();

    @JsonAlias(value = "microdataEnabled")
    @Nullable
    Boolean getSchemaOrgEnabled();

    @Nullable
    Boolean getCollectionDescriptionsInOverview();

    @Nullable
    String getLegalName();

    @Nullable
    String getLegalUrl();

    @Nullable
    String getPrivacyName();

    @Nullable
    String getPrivacyUrl();

    @Nullable
    String getMapBackgroundUrl();

    @Nullable
    String getMapAttribution();

    @Deprecated(since = "3.1.0")
    @Nullable
    String getLeafletUrl();

    @Deprecated(since = "3.1.0")
    @Nullable
    String getLeafletAttribution();

    @Deprecated(since = "3.1.0")
    @Nullable
    String getOpenLayersUrl();

    @Deprecated(since = "3.1.0")
    @Nullable
    String getOpenLayersAttribution();

    @Nullable
    String getFooterText();

    @Override
    default Builder getBuilder() {
        return new ImmutableHtmlConfiguration.Builder();
    }
}

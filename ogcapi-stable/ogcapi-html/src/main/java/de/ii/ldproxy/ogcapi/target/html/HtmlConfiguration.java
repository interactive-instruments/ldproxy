/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.target.html;

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
    String getLeafletUrl();

    @Nullable
    String getLeafletAttribution();

    @Nullable
    String getOpenLayersUrl();

    @Nullable
    String getOpenLayersAttribution();

    @Nullable
    String getFooterText();

    @Nullable
    String getApiCatalogLabel();

    @Nullable
    String getApiCatalogDescription();

    @Override
    default Builder getBuilder() {
        return new ImmutableHtmlConfiguration.Builder();
    }
}

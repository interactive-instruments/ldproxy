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
@JsonDeserialize(builder = ImmutableFoundationConfiguration.Builder.class)
public interface FoundationConfiguration extends ExtensionConfiguration {

    String API_RESOURCES_DIR = "api-resources";
    String CACHE_DIR = "cache";
    String TMP_DIR = "tmp";

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Nullable
    Boolean getUseLangParameter();

    @Nullable
    Boolean getIncludeLinkHeader();

    @Nullable
    String getApiCatalogLabel();

    @Nullable
    String getApiCatalogDescription();

    @Override
    default Builder getBuilder() {
        return new ImmutableFoundationConfiguration.Builder();
    }
}
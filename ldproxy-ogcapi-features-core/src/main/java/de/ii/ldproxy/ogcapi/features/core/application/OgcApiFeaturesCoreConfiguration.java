/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.application;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableOgcApiFeaturesCoreConfiguration.Builder.class)
public abstract class OgcApiFeaturesCoreConfiguration implements ExtensionConfiguration {

    static final int MINIMUM_PAGE_SIZE = 1;
    static final int DEFAULT_PAGE_SIZE = 10;
    static final int MAX_PAGE_SIZE = 10000;

    @Value.Default
    @Override
    public boolean getEnabled() {
        return true;
    }

    @Value.Default
    public int getMinimumPageSize() {
        return MINIMUM_PAGE_SIZE;
    }

    @Value.Default
    public int getDefaultPageSize() {
        return DEFAULT_PAGE_SIZE;
    }

    @Value.Default
    public int getMaxPageSize() {
        return MAX_PAGE_SIZE;
    }

    @Value.Default
    public boolean getShowsFeatureSelfLink() { return false; }

}

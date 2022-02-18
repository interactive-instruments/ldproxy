/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.extensions.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Objects;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableFeaturesExtensionsConfiguration.Builder.class)
public interface FeaturesExtensionsConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Nullable
    Boolean getPostOnItems();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean shouldSupportPostOnItems() {
        return Objects.equals(getPostOnItems(), true);
    }


    @Nullable
    Boolean getIntersectsParameter();

    @Override
    default Builder getBuilder() {
        return new ImmutableFeaturesExtensionsConfiguration.Builder();
    }
}

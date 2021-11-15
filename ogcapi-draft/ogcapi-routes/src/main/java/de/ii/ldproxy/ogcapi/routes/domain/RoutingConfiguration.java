/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.CachingConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Map;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableRoutingConfiguration.Builder.class)
public interface RoutingConfiguration extends ExtensionConfiguration, CachingConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Nullable
    String getFeatureType();

    /* TODO not yet implemented
    @Nullable
    Boolean getAsync();

    @Nullable
    Boolean getCallback();

    */

    @Nullable
    Boolean getIntermediateWaypoints();

    Map<String, RoutingFlag> getPreferences();

    String getDefaultPreference();

    Map<String, RoutingFlag> getAdditionalFlags();

    @Nullable
    FeaturesCoreConfiguration.DefaultCrs getDefaultCrs();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default EpsgCrs getDefaultEpsgCrs() {
        return ImmutableEpsgCrs.copyOf(getDefaultCrs() == FeaturesCoreConfiguration.DefaultCrs.CRS84h ? OgcCrs.CRS84h : OgcCrs.CRS84);
    }

    Map<String, Integer> getCoordinatePrecision();

    @Nullable
    Boolean getHtml();

    @Override
    default Builder getBuilder() {
        return new ImmutableRoutingConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableRoutingConfiguration.Builder builder = ((ImmutableRoutingConfiguration.Builder) source.getBuilder())
                .from(source)
                .from(this);

        RoutingConfiguration src = (RoutingConfiguration) source;

        // always override the default configuration options
        builder.preferences(getPreferences());
        builder.additionalFlags(getAdditionalFlags());

        return builder.build();
    }
}
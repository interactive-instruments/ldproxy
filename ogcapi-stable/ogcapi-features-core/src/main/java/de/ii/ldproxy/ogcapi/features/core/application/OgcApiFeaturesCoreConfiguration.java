/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.application;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformations;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTypeMapping2;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeaturesCollectionQueryables;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableOgcApiFeaturesCoreConfiguration.Builder.class)
public abstract class OgcApiFeaturesCoreConfiguration implements ExtensionConfiguration, FeatureTransformations {

    enum DefaultCrs {CRS84, CRS84h}

    static final int MINIMUM_PAGE_SIZE = 1;
    static final int DEFAULT_PAGE_SIZE = 10;
    static final int MAX_PAGE_SIZE = 10000;

    @Value.Default
    @Override
    public boolean getEnabled() {
        return true;
    }

    public abstract Optional<String> getFeatureProvider();

    public abstract Optional<String> getFeatureType();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Value.Default
    public List<String> getFeatureTypes() {
        return getFeatureType().isPresent() ? ImmutableList.of(getFeatureType().get()) : ImmutableList.of();
    }

    @Value.Default
    public DefaultCrs getDefaultCrs() {
        return DefaultCrs.CRS84;
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
    public boolean getShowsFeatureSelfLink() {
        return false;
    }

    public abstract Optional<OgcApiFeaturesCollectionQueryables> getQueryables();

    @Override
    public abstract Map<String, FeatureTypeMapping2> getTransformations();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    public EpsgCrs getDefaultEpsgCrs() {
        return getDefaultCrs() == DefaultCrs.CRS84h ? OgcCrs.CRS84h : OgcCrs.CRS84;
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    public Map<String, String> getAllFilterParameters() {
        if (getQueryables().isPresent()) {
            OgcApiFeaturesCollectionQueryables queryables = getQueryables().get();
            ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();

            if (!queryables.getSpatial()
                           .isEmpty()) {
                builder.put("bbox", queryables.getSpatial()
                                              .get(0));
            }
            if (!queryables.getTemporal()
                           .isEmpty()) {
                // TODO support more than a single queryable, e.g. for interval start/end DateTime values
                builder.put("datetime", queryables.getTemporal()
                                                  .get(0));
            }
            queryables.getOther()
                      .forEach(other -> builder.put(other, other));

            return builder.build();
        }

        return ImmutableMap.of("bbox", "NOT_AVAILABLE");
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    public Map<String, String> getOtherFilterParameters() {
        if (getQueryables().isPresent()) {
            OgcApiFeaturesCollectionQueryables queryables = getQueryables().get();
            ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();

            queryables.getOther()
                      .forEach(other -> builder.put(other, other));

            return builder.build();
        }

        return ImmutableMap.of("bbox", "NOT_AVAILABLE");
    }

    @Override
    public <T extends ExtensionConfiguration> T mergeDefaults(T extensionConfigurationDefault) {
        DefaultCrs collectionDefaultCrs = this.getDefaultCrs();
        DefaultCrs apiDefaultCrs = ((OgcApiFeaturesCoreConfiguration)extensionConfigurationDefault).getDefaultCrs();
        DefaultCrs mergedDefaultCrs = (collectionDefaultCrs == DefaultCrs.CRS84h || apiDefaultCrs == DefaultCrs.CRS84h) ? DefaultCrs.CRS84h : DefaultCrs.CRS84;

        return (T) new ImmutableOgcApiFeaturesCoreConfiguration.Builder().from(extensionConfigurationDefault)
                                                                         .from(this)
                                                                         .defaultCrs(mergedDefaultCrs)
                                                                         .build();
    }
}

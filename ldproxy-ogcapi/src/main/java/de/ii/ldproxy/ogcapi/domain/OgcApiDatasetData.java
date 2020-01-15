/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.event.store.EntityDataBuilder;
import de.ii.xtraplatform.service.api.ServiceData;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;



@Value.Immutable
@JsonDeserialize(builder = ImmutableOgcApiDatasetData.Builder.class)
public abstract class OgcApiDatasetData implements ExtendableConfiguration, ServiceData {

    public static final String DEFAULT_CRS_URI = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
    public static final EpsgCrs DEFAULT_CRS = new EpsgCrs(4326, true);
    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiDatasetData.class);

    static abstract class Builder implements EntityDataBuilder<OgcApiDatasetData> {
    }

    //@JsonMerge
    //@Nullable
    //public abstract ValueBuilderMap<Test, ImmutableTest.Builder> getTestMap();


    @Override
    public long getCurrentEntityDataVersion() {
        return 1;
    }

    @Value.Default
    @Override
    public String getLabel() {
        return getId();
    }

    //behaves exactly like Map<String, FeatureTypeConfigurationOgcApi>, but supports mergeable builder deserialization
    //(immutables attributeBuilder does not work with maps yet)
    @JsonMerge
    //@Override
    public abstract ValueBuilderMap<FeatureTypeConfigurationOgcApi, ImmutableFeatureTypeConfigurationOgcApi.Builder> getFeatureTypes();

    public abstract List<EpsgCrs> getAdditionalCrs();

    //TODO: Optional does not work with nested builders
    @Nullable
    public abstract Metadata getMetadata();

    @Override
    @Value.Derived
    public boolean isLoading() {
        //TODO: delegate to extensions?
        return false;
        //return Objects.nonNull(getFeatureProvider().getMappingStatus()) && getFeatureProvider().getMappingStatus()
        //                           .getLoading();
    }

    @Override
    @Value.Derived
    public boolean hasError() {
        //TODO: delegate to extensions?
        return false;
        /*return Objects.nonNull(getFeatureProvider().getMappingStatus())
                && getFeatureProvider().getMappingStatus().getEnabled()
                && !getFeatureProvider().getMappingStatus().getSupported()
                && Objects.nonNull(getFeatureProvider().getMappingStatus().getErrorMessage());*/
    }

    public boolean isCollectionEnabled(final String collectionId) {
        return getFeatureTypes().containsKey(collectionId); //TODO && getFeatureTypes().get(featureType).isEnabled();
        //return getFeatureProvider().isFeatureTypeEnabled(featureType);
    }

    /**
     * Determine spatial extent of all collections in the dataset.
     * @return array of coordinates of the bounding box in the following format:
     * [minimum longitude, minimum latitude, maximum longitude, maximum latitude]
     */
    public double[] getSpatialExtent() {
        double[] spatialExtent = getFeatureTypes().values()
                .stream()
                .map(featureTypeConfigurationWfs3 -> featureTypeConfigurationWfs3.getExtent()
                        .getSpatial()
                        .getCoords())
                .reduce((doubles, doubles2) -> new double[]{
                        Math.min(doubles[0], doubles2[0]),
                        Math.min(doubles[1], doubles2[1]),
                        Math.max(doubles[2], doubles2[2]),
                        Math.max(doubles[3], doubles2[3])})
                .orElse(null);
        return spatialExtent;
    }
}

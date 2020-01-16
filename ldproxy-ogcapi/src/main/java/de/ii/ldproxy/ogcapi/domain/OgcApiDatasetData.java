/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.event.store.EntityDataBuilder;
import de.ii.xtraplatform.service.api.ServiceData;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;


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
     * @return the bounding box in the default CRS
     */
    @Nullable
    @JsonIgnore
    @Value.Derived
    public BoundingBox getSpatialExtent() {
        double[] val = getFeatureTypes().values()
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

        return Objects.nonNull(val) ? new BoundingBox(val[0], val[1], val[2], val[3], DEFAULT_CRS) : null;
    }

    /**
     * Determine spatial extent of all collections in the dataset in another CRS.
     * @param crsTransformation the factory for CRS transformers
     * @param targetCrs the target CRS
     * @return the bounding box
     */
    public BoundingBox getSpatialExtent(CrsTransformation crsTransformation, EpsgCrs targetCrs) throws CrsTransformationException {
        BoundingBox spatialExtent = getSpatialExtent();
        CrsTransformer crsTransformer = crsTransformation.getTransformer(DEFAULT_CRS, targetCrs);

        return Objects.nonNull(spatialExtent) ? crsTransformer.transformBoundingBox(spatialExtent) : null;
    }

    /**
     * Determine spatial extent of a collection in the dataset.
     * @param collectionId the name of the feature type
     * @return the bounding box in the default CRS
     */
    public BoundingBox getSpatialExtent(String collectionId) {
        BoundingBox spatialExtent = getFeatureTypes().values()
                                                     .stream()
                                                     .filter(featureTypeConfiguration -> featureTypeConfiguration.getId().equals(collectionId))
                                                     .map(featureTypeConfiguration -> featureTypeConfiguration.getExtent()
                                                                                                              .getSpatial())
                                                     .findFirst()
                                                     .orElse(null);
        return spatialExtent;
    }

    /**
     * Determine spatial extent of a collection in the dataset in another CRS.
     * @param collectionId the name of the feature type
     * @param crsTransformation the factory for CRS transformers
     * @param targetCrs the target CRS
     * @return the bounding box in the target CRS
     */
    public BoundingBox getSpatialExtent(String collectionId, CrsTransformation crsTransformation, EpsgCrs targetCrs) throws CrsTransformationException {
        BoundingBox spatialExtent = getSpatialExtent(collectionId);
        CrsTransformer crsTransformer = crsTransformation.getTransformer(DEFAULT_CRS, targetCrs);

        return Objects.nonNull(spatialExtent) ? crsTransformer.transformBoundingBox(spatialExtent) : null;
    }
}

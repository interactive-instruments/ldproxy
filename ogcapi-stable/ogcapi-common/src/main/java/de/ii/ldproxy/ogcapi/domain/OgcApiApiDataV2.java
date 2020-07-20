/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.*;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.event.store.EntityDataBuilder;
import de.ii.xtraplatform.service.api.ServiceData;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Value.Immutable
@JsonDeserialize(builder = ImmutableOgcApiApiDataV2.Builder.class)
public abstract class OgcApiApiDataV2 implements ServiceData, ExtendableConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiApiDataV2.class);

    public static final String SERVICE_TYPE = "OGC_API";

    static abstract class Builder implements EntityDataBuilder<OgcApiApiDataV2> {
    }

    @Value.Derived
    @Override
    public long getEntitySchemaVersion() {
        return 2;
    }

    @Override
    public Optional<String> getEntitySubType() {
        return Optional.of(SERVICE_TYPE);
    }

    @Value.Default
    @Override
    public String getServiceType() {
        return SERVICE_TYPE;
    }

    public abstract Optional<Metadata> getMetadata();

    public abstract Optional<OgcApiExternalDocumentation> getExternalDocs();

    @JsonProperty(value = "api")
    @Override
    public abstract List<ExtensionConfiguration> getExtensions();

    //behaves exactly like Map<String, FeatureTypeConfigurationOgcApi>, but supports mergeable builder deserialization
    //(immutables attributeBuilder does not work with maps yet)
    //@JsonMerge
    public abstract ValueBuilderMap<FeatureTypeConfigurationOgcApi, ImmutableFeatureTypeConfigurationOgcApi.Builder> getCollections();

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

    @Value.Derived
    public boolean isCollectionEnabled(final String collectionId) {
        return getCollections().containsKey(collectionId) && getCollections().get(collectionId).getEnabled();
    }

    /**
     * Determine spatial extent of all collections in the dataset.
     * @return the bounding box in the default CRS
     */
    @Nullable
    @JsonIgnore
    @Value.Derived
    public BoundingBox getSpatialExtent() {
        double[] val = getCollections().values()
                                       .stream()
                                       .map(FeatureTypeConfigurationOgcApi::getExtent)
                                       .filter(Optional::isPresent)
                                       .map(Optional::get)
                                       .map(FeatureTypeConfigurationOgcApi.CollectionExtent::getSpatial)
                                       .filter(Optional::isPresent)
                                       .map(Optional::get)
                                       .map(BoundingBox::getCoords)
                                       .reduce((doubles, doubles2) -> new double[]{
                                                Math.min(doubles[0], doubles2[0]),
                                                Math.min(doubles[1], doubles2[1]),
                                                Math.max(doubles[2], doubles2[2]),
                                                Math.max(doubles[3], doubles2[3])})
                                       .orElse(null);

        return Objects.nonNull(val) ? new BoundingBox(val[0], val[1], val[2], val[3], OgcCrs.CRS84) : null;
    }

    /**
     * Determine spatial extent of all collections in the dataset in another CRS.
     * @param crsTransformerFactory the factory for CRS transformers
     * @param targetCrs the target CRS
     * @return the bounding box
     */
    @Value.Derived
    public BoundingBox getSpatialExtent(CrsTransformerFactory crsTransformerFactory, EpsgCrs targetCrs) throws CrsTransformationException {
        BoundingBox spatialExtent = getSpatialExtent();

        return transformSpatialExtent(spatialExtent, crsTransformerFactory, targetCrs);
    }

    /**
     * Determine spatial extent of a collection in the dataset.
     * @param collectionId the name of the feature type
     * @return the bounding box in the default CRS
     */
    @Value.Derived
    public BoundingBox getSpatialExtent(String collectionId) {
        return getCollections().values()
                               .stream()
                               .filter(featureTypeConfiguration -> featureTypeConfiguration.getId().equals(collectionId))
                               .map(FeatureTypeConfigurationOgcApi::getExtent)
                               .filter(Optional::isPresent)
                               .map(Optional::get)
                               .map(FeatureTypeConfigurationOgcApi.CollectionExtent::getSpatial)
                               .filter(Optional::isPresent)
                               .map(Optional::get)
                               .findFirst()
                               .orElse(null);
    }

    /**
     * Determine spatial extent of a collection in the dataset in another CRS.
     * @param collectionId the name of the feature type
     * @param crsTransformerFactory the factory for CRS transformers
     * @param targetCrs the target CRS
     * @return the bounding box in the target CRS
     */
    @Value.Derived
    public BoundingBox getSpatialExtent(String collectionId, CrsTransformerFactory crsTransformerFactory, EpsgCrs targetCrs) throws CrsTransformationException {
        BoundingBox spatialExtent = getSpatialExtent(collectionId);

        return transformSpatialExtent(spatialExtent, crsTransformerFactory, targetCrs);
    }

    @Value.Derived
    private BoundingBox transformSpatialExtent(BoundingBox spatialExtent, CrsTransformerFactory crsTransformerFactory, EpsgCrs targetCrs) throws CrsTransformationException {
        Optional<CrsTransformer> crsTransformer = crsTransformerFactory.getTransformer(OgcCrs.CRS84, targetCrs);

        if (Objects.nonNull(spatialExtent) && crsTransformer.isPresent()) {
            return crsTransformer.get().transformBoundingBox(spatialExtent);
        }

        return spatialExtent;
    }
}

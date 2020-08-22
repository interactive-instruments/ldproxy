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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.*;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import de.ii.xtraplatform.services.domain.ServiceData;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


@Value.Immutable
@JsonDeserialize(builder = ImmutableOgcApiDataV2.Builder.class)
public abstract class OgcApiDataV2 implements ServiceData, ExtendableConfiguration {

    public static final String SERVICE_TYPE = "OGC_API";

    static abstract class Builder implements EntityDataBuilder<OgcApiDataV2> {

        // jackson should append to instead of replacing extensions
        @JsonIgnore
        public abstract Builder extensions(Iterable<? extends ExtensionConfiguration> elements);

        @JsonProperty("api")
        public abstract Builder addAllExtensions(Iterable<? extends ExtensionConfiguration> elements);

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

    public abstract Optional<ExternalDocumentation> getExternalDocs();

    @JsonProperty(value = "api")
    @JsonMerge
    @Override
    public abstract List<ExtensionConfiguration> getExtensions();

    //behaves exactly like Map<String, FeatureTypeConfigurationOgcApi>, but supports mergeable builder deserialization
    //(immutables attributeBuilder does not work with maps yet)
    public abstract BuildableMap<FeatureTypeConfigurationOgcApi, ImmutableFeatureTypeConfigurationOgcApi.Builder> getCollections();

    @Override
    @Value.Derived
    public boolean isLoading() {
        //TODO: delegate to extensions?
        return false;
    }

    @Override
    @Value.Derived
    public boolean hasError() {
        //TODO: delegate to extensions?
        return false;
    }

    @Value.Check
    public OgcApiDataV2 mergeBuildingBlocks() {
        boolean collectionsHaveMissingParentExtensions = getCollections().values()
                                                                         .stream()
                                                                         .anyMatch(collection -> collection.getParentExtensions()
                                                                                                           .size() < getExtensions().size());

        if (collectionsHaveMissingParentExtensions) {
            Map<String, FeatureTypeConfigurationOgcApi> mergedCollections = new LinkedHashMap<>();

            getCollections().values()
                            .forEach(featureTypeConfigurationOgcApi -> mergedCollections.put(featureTypeConfigurationOgcApi.getId(), featureTypeConfigurationOgcApi.getBuilder()
                                                                                                                                                               .parentExtensions(getExtensions())
                                                                                                                                                               .build()));

            return new ImmutableOgcApiDataV2.Builder().from(this)
                                                         .collections(mergedCollections)
                                                         .build();
        }

        return this;
    }

    public boolean isCollectionEnabled(final String collectionId) {
        return getCollections().containsKey(collectionId) && getCollections().get(collectionId)
                                                                             .getEnabled();
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
    public BoundingBox getSpatialExtent(CrsTransformerFactory crsTransformerFactory, EpsgCrs targetCrs) throws CrsTransformationException {
        BoundingBox spatialExtent = getSpatialExtent();

        return transformSpatialExtent(spatialExtent, crsTransformerFactory, targetCrs);
    }

    /**
     * Determine spatial extent of a collection in the dataset.
     * @param collectionId the name of the feature type
     * @return the bounding box in the default CRS
     */
    public BoundingBox getSpatialExtent(String collectionId) {
        return getCollections().values()
                               .stream()
                               .filter(featureTypeConfiguration -> featureTypeConfiguration.getId()
                                                                                           .equals(collectionId))
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
    public BoundingBox getSpatialExtent(String collectionId, CrsTransformerFactory crsTransformerFactory, EpsgCrs targetCrs) throws CrsTransformationException {
        BoundingBox spatialExtent = getSpatialExtent(collectionId);

        return transformSpatialExtent(spatialExtent, crsTransformerFactory, targetCrs);
    }

    private BoundingBox transformSpatialExtent(BoundingBox spatialExtent, CrsTransformerFactory crsTransformerFactory, EpsgCrs targetCrs) throws CrsTransformationException {
        Optional<CrsTransformer> crsTransformer = crsTransformerFactory.getTransformer(OgcCrs.CRS84, targetCrs);

        if (Objects.nonNull(spatialExtent) && crsTransformer.isPresent()) {
            return crsTransformer.get()
                                 .transformBoundingBox(spatialExtent);
        }

        return spatialExtent;
    }
}

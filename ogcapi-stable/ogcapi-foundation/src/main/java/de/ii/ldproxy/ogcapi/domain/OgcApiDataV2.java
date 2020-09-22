/**
 * Copyright 2020 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import jersey.repackaged.com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

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

        public abstract ImmutableOgcApiDataV2.Builder id(String id);

        @Override
        public EntityDataBuilder<OgcApiDataV2> fillRequiredFieldsWithPlaceholders() {
            String placeholder = "__DEFAULT__";
            return this.id(placeholder)
                .serviceType(placeholder);
        }

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

    public abstract Optional<CollectionExtent> getDefaultExtent();

    // TODO: move to ServiceData?
    public abstract List<String> getTags();

    // TODO: move to ServiceData?
    @Value.Derived
    public List<String> getSubPath() {
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
        builder.add(getId());
        if (getApiVersion().isPresent())
            builder.add("v"+getApiVersion().get());
        return builder.build();
    }

    // TODO: move to ServiceData?
    @Value.Derived
    public int getSubPathLength() {
        return 1 + (getApiVersion().isPresent() ? 1 : 0);
    }

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
     *
     * @return the bounding box in the default CRS
     */
    @JsonIgnore
    @Value.Derived
    public Optional<BoundingBox> getSpatialExtent() {
        return getCollections().keySet()
                               .stream()
                               .map(this::getSpatialExtent)
                               .filter(Optional::isPresent)
                               .map(Optional::get)
                               .map(BoundingBox::getCoords)
                               .reduce((doubles, doubles2) -> new double[]{
                                       Math.min(doubles[0], doubles2[0]),
                                       Math.min(doubles[1], doubles2[1]),
                                       Math.max(doubles[2], doubles2[2]),
                                       Math.max(doubles[3], doubles2[3])})
                               .map(doubles -> new BoundingBox(doubles[0], doubles[1], doubles[2], doubles[3], OgcCrs.CRS84));
    }

    /**
     * Determine spatial extent of all collections in the dataset in another CRS.
     *
     * @param crsTransformerFactory the factory for CRS transformers
     * @param targetCrs             the target CRS
     * @return the bounding box
     */
    public Optional<BoundingBox> getSpatialExtent(CrsTransformerFactory crsTransformerFactory, EpsgCrs targetCrs) throws CrsTransformationException {
        Optional<BoundingBox> spatialExtent = getSpatialExtent();

        if (spatialExtent.isPresent()) {
            return Optional.ofNullable(transformSpatialExtent(spatialExtent.get(), crsTransformerFactory, targetCrs));
        }

        return Optional.empty();
    }

    /**
     * Determine temporal extent of all collections in the dataset.
     *
     * @return the temporal extent in the Gregorian calendar
     */
    @JsonIgnore
    @Value.Derived
    public Optional<TemporalExtent> getTemporalExtent() {
        return getCollections().keySet()
                               .stream()
                               .map(this::getTemporalExtent)
                               .filter(Optional::isPresent)
                               .map(Optional::get)
                               .map(temporalExtent -> new Long[]{temporalExtent.getStart(), temporalExtent.getEnd()})
                               .reduce((longs, longs2) -> new Long[]{
                                       longs[0] == null || longs2[0] == null ? null : Math.min(longs[0], longs2[0]),
                                       longs[1] == null || longs2[1] == null ? null : Math.max(longs[1], longs2[1])})
                               .map(longs -> new ImmutableTemporalExtent.Builder().start(longs[0])
                                                                                  .end(longs[1])
                                                                                  .build());
    }

    /**
     * Determine spatial extent of a collection in the dataset.
     *
     * @param collectionId the name of the feature type
     * @return the bounding box in the default CRS
     */
    public Optional<BoundingBox> getSpatialExtent(String collectionId) {
        return getCollections().values()
                               .stream()
                               .filter(featureTypeConfiguration -> featureTypeConfiguration.getId()
                                                                                           .equals(collectionId))
                               .filter(FeatureTypeConfigurationOgcApi::getEnabled)
                               .findFirst()
                               .flatMap(FeatureTypeConfigurationOgcApi::getExtent)
                               .flatMap(CollectionExtent::getSpatial)
                               .or(() -> getDefaultExtent().flatMap(CollectionExtent::getSpatial));
    }

    /**
     * Determine spatial extent of a collection in the dataset in another CRS.
     *
     * @param collectionId          the name of the feature type
     * @param crsTransformerFactory the factory for CRS transformers
     * @param targetCrs             the target CRS
     * @return the bounding box in the target CRS
     */
    public Optional<BoundingBox> getSpatialExtent(String collectionId, CrsTransformerFactory crsTransformerFactory, EpsgCrs targetCrs) throws CrsTransformationException {
        Optional<BoundingBox> spatialExtent = getSpatialExtent(collectionId);

        if (spatialExtent.isPresent()) {
            return Optional.ofNullable(transformSpatialExtent(spatialExtent.get(), crsTransformerFactory, targetCrs));
        }

        return Optional.empty();
    }

    private BoundingBox transformSpatialExtent(BoundingBox spatialExtent, CrsTransformerFactory crsTransformerFactory, EpsgCrs targetCrs) throws CrsTransformationException {
        Optional<CrsTransformer> crsTransformer = crsTransformerFactory.getTransformer(OgcCrs.CRS84, targetCrs);

        if (Objects.nonNull(spatialExtent) && crsTransformer.isPresent()) {
            return crsTransformer.get()
                                 .transformBoundingBox(spatialExtent);
        }

        return spatialExtent;
    }

    /**
     * Determine temporal extent of a collection in the dataset.
     *
     * @param collectionId the name of the feature type
     * @return the temporal extent in the Gregorian calendar
     */
    public Optional<TemporalExtent> getTemporalExtent(String collectionId) {
        return getCollections().values()
                               .stream()
                               .filter(featureTypeConfiguration -> featureTypeConfiguration.getId()
                                                                                           .equals(collectionId))
                               .filter(FeatureTypeConfigurationOgcApi::getEnabled)
                               .findFirst()
                               .flatMap(FeatureTypeConfigurationOgcApi::getExtent)
                               .flatMap(CollectionExtent::getTemporal)
                               .or(() -> getDefaultExtent().flatMap(CollectionExtent::getTemporal));
    }
}

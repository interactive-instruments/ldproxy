/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaults;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import org.immutables.value.Value;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            return this.id(EntityDataDefaults.PLACEHOLDER)
                       .serviceType(EntityDataDefaults.PLACEHOLDER);
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

    public abstract Optional<ApiMetadata> getMetadata();

    public abstract Optional<ExternalDocumentation> getExternalDocs();

    public abstract Optional<CollectionExtent> getDefaultExtent();

    public abstract Optional<Caching> getDefaultCaching();

    @Value.Default
    public MODE getApiValidation() {
        return MODE.NONE;
    }

    // TODO: move to ServiceData?
    public abstract List<String> getTags();

    // TODO: move to ServiceData?
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    public List<String> getSubPath() {
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
        builder.add(getId());
        if (getApiVersion().isPresent()) {
            builder.add("v" + getApiVersion().get());
        }
        return builder.build();
    }

    @JsonProperty("api")
    @JsonMerge
    @Override
    public abstract List<ExtensionConfiguration> getExtensions();

    //behaves exactly like Map<String, FeatureTypeConfigurationOgcApi>, but supports mergeable builder deserialization
    //(immutables attributeBuilder does not work with maps yet)
    public abstract BuildableMap<FeatureTypeConfigurationOgcApi, ImmutableFeatureTypeConfigurationOgcApi.Builder> getCollections();

    public Optional<FeatureTypeConfigurationOgcApi> getCollectionData(String collectionId) {
        return Optional.ofNullable(getCollections().get(collectionId));
    }

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
        List<ExtensionConfiguration> distinctExtensions = getMergedExtensions();

        // remove duplicates
        if (getExtensions().size() > distinctExtensions.size()) {
            return new ImmutableOgcApiDataV2.Builder().from(this)
                                                      .extensions(distinctExtensions)
                                                      .build();
        }

        boolean collectionsHaveMissingParentExtensions = getCollections().values()
                                                                         .stream()
                                                                         .anyMatch(collection -> collection.getParentExtensions()
                                                                                                           .size() < getMergedExtensions().size());

        if (collectionsHaveMissingParentExtensions) {
            Map<String, FeatureTypeConfigurationOgcApi> mergedCollections = new LinkedHashMap<>(getCollections());

                mergedCollections.values()
                    .forEach(featureTypeConfigurationOgcApi -> mergedCollections
                        .put(featureTypeConfigurationOgcApi.getId(),
                            featureTypeConfigurationOgcApi.getBuilder()
                                                                                                                                                                   .parentExtensions(getMergedExtensions())
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

    @JsonIgnore
    @Value.Derived
    public boolean isDataset() {
        // return false if there no collection or no collection that is enabled
        return getCollections()
            .values()
            .stream()
            .anyMatch(FeatureTypeConfigurationOgcApi::getEnabled);
    }

    /**
     * Determine extent of a collection in the dataset.
     *
     * @param collectionId the name of the feature type
     * @return the extent
     */
    public Optional<CollectionExtent> getExtent(String collectionId) {
        return getCollections().values()
            .stream()
            .filter(featureTypeConfiguration -> featureTypeConfiguration.getId()
                .equals(collectionId))
            .filter(FeatureTypeConfigurationOgcApi::getEnabled)
            .findFirst()
            .flatMap(FeatureTypeConfigurationOgcApi::getExtent)
            .flatMap(collectionExtent -> mergeExtents(getDefaultExtent(), Optional.of(collectionExtent)))
            .or(this::getDefaultExtent);
    }

    private Optional<CollectionExtent> mergeExtents(Optional<CollectionExtent> defaultExtent, Optional<CollectionExtent> collectionExtent) {
        if (defaultExtent.isEmpty()) {
            return collectionExtent;
        } else if (collectionExtent.isEmpty()) {
            return defaultExtent;
        }

        return Optional.of(new ImmutableCollectionExtent.Builder()
            .from(defaultExtent.get())
            .from(collectionExtent.get())
            .build());
    }

    public <T extends ExtensionConfiguration> Optional<T> getExtension(Class<T> clazz, String collectionId) {
        if (isCollectionEnabled(collectionId)) {
            return getCollections().get(collectionId).getExtension(clazz);
        }
        return getExtension(clazz);
    }
}

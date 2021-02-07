/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.CollectionExtent;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableCollectionExtent;
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableMetadata;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.ImmutableTemporalExtent;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataHydratorExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.TemporalExtent;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableFeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableFeaturesCoreConfiguration;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.Metadata;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class FeaturesCoreDataHydrator implements OgcApiDataHydratorExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesCoreDataHydrator.class);

    private final FeaturesCoreProviders providers;
    private final CrsTransformerFactory crsTransformerFactory;

    public FeaturesCoreDataHydrator(@Requires FeaturesCoreProviders providers,
                                    @Requires CrsTransformerFactory crsTransformerFactory) {
        this.providers = providers;
        this.crsTransformerFactory = crsTransformerFactory;
    }

    @Override
    public int getSortPriority() {
        // this must be processed before any other hydrator that works with collections
        return 100;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }

    @Override
    public OgcApiDataV2 getHydratedData(OgcApiDataV2 apiData) {

        OgcApiDataV2 data = apiData;
        FeatureProvider2 featureProvider = providers.getFeatureProvider(data);

        // if the feature provider is not available, disable all collections
        boolean disabled = false;
        if (Objects.isNull(featureProvider)) {
            LOGGER.error("The feature provider was not found, all collections are removed from the configuration.");
            data = new ImmutableOgcApiDataV2.Builder()
                    .from(data)
                    .collections(ImmutableMap.of())
                    .build();
            disabled = true;

        } else if (data.isAuto() && data.getCollections()
                                        .isEmpty()) {
            data = new ImmutableOgcApiDataV2.Builder()
                    .from(data)
                    .collections(generateCollections(featureProvider))
                    .build();

        } else {
            // process configuration and remove invalid configuration elements
            data = new ImmutableOgcApiDataV2.Builder()
                    .from(data)
                    .collections(
                            data.getCollections()
                                .entrySet()
                                .stream()
                                .filter(collection -> {
                                    // remove all collections without a feature provider
                                    String collectionId = collection.getKey();
                                    Optional<FeaturesCoreConfiguration> config = collection.getValue().getExtension(FeaturesCoreConfiguration.class);
                                    String typeId = config.map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                                          .orElse(collectionId);
                                    FeatureSchema schema = featureProvider.getData().getTypes().get(typeId);
                                    if (Objects.isNull(schema)) {
                                        LOGGER.error("Collection '{}' has been removed, because the feature type '{}' was not found.", collectionId, typeId);
                                        return false;
                                    }
                                    return true;
                                })
                                .map(entry -> {
                                    // * remove unknown queryables and transformations from the Features Core configuration
                                    // * normalize the property references in queryables and transformations by removing all parts in square brackets

                                    final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
                                    final FeaturesCoreConfiguration config = collectionData.getExtension(FeaturesCoreConfiguration.class).orElse(null);
                                    if (Objects.isNull(config))
                                        return entry;

                                    final String buildingBlock = config.getBuildingBlock();
                                    final String collectionId = entry.getKey();
                                    final FeatureSchema schema = featureProvider.getData()
                                                                                .getTypes()
                                                                                .get(config.getFeatureType().orElse(collectionId));

                                    return new AbstractMap.SimpleImmutableEntry<String, FeatureTypeConfigurationOgcApi>(collectionId, new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                            .from(entry.getValue())

                                            // use the type label from the provider, if the service configuration has just the default label
                                            .label(collectionData.getLabel().equals(collectionId) && schema.getLabel().isPresent()
                                                           ? schema.getLabel().get()
                                                           : collectionData.getLabel())
                                            // use the type description from the provider, if the service configuration does not have one
                                            .description(Optional.ofNullable(collectionData.getDescription()
                                                                                           .orElse(schema.getDescription()
                                                                                                         .orElse(null))))
                                            // validate and update the Features Core configuration
                                            .extensions(new ImmutableList.Builder<ExtensionConfiguration>()
                                                    // do not touch any other extension
                                                    .addAll(entry.getValue()
                                                                      .getExtensions()
                                                                      .stream()
                                                                      .filter(ext -> !ext.getBuildingBlock().equals(buildingBlock))
                                                                      .collect(Collectors.toUnmodifiableList()))
                                                    // process the Features Core configuration
                                                    .add(new ImmutableFeaturesCoreConfiguration.Builder()
                                                                 .from(config)
                                                                 .queryables(config.validateQueryables(collectionId, schema))
                                                                 .transformations(config.validateTransformations(buildingBlock,collectionId,schema))
                                                                 .build())
                                                    .build()
                                            )
                                            .build());
                                })
                                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
                    .build();
        }

        if (!disabled && !data.isAutoPersist() && hasMissingBboxes(data.getCollections())) {
            data = new ImmutableOgcApiDataV2.Builder()
                    .from(data)
                    .collections(computeMissingBboxes(data))
                    .build();
        }

        if (!disabled && !data.isAutoPersist() && hasMissingIntervals(data.getCollections())) {
            data = new ImmutableOgcApiDataV2.Builder()
                    .from(data)
                    .collections(computeMissingIntervals(data))
                    .build();
        }

        if (!disabled && data.isAuto() && data.isAutoPersist()) {
            data = new ImmutableOgcApiDataV2.Builder()
                .from(data)
                .auto(Optional.empty())
                .autoPersist(Optional.empty())
                .build();
        }

        if (!disabled && !data.getMetadata().isPresent() && featureProvider.supportsMetadata()) {
            Optional<Metadata> providerMetadata = featureProvider.metadata()
                                                                 .getMetadata();
            if (providerMetadata.isPresent()) {
                Optional<String> license = providerMetadata.flatMap(Metadata::getAccessConstraints).isPresent()
                        ? providerMetadata.flatMap(Metadata::getAccessConstraints)
                        : providerMetadata.flatMap(Metadata::getFees);

                ImmutableMetadata metadata = new ImmutableMetadata.Builder()
                        .addAllKeywords(providerMetadata.get().getKeywords())
                        .contactName(providerMetadata.get().getContactName())
                        .contactUrl(providerMetadata.get().getContactUrl())
                        .contactEmail(providerMetadata.get().getContactEmail())
                        .licenseName(license)
                        .build();

                String label = Objects.equals(data.getId(), data.getLabel()) && providerMetadata.flatMap(Metadata::getLabel).isPresent()
                        ? providerMetadata.flatMap(Metadata::getLabel).get()
                        : data.getLabel();

                Optional<String> description = data.getDescription().isPresent() ? data.getDescription() : providerMetadata.flatMap(Metadata::getDescription);

                data = new ImmutableOgcApiDataV2.Builder()
                        .from(data)
                        .label(label)
                        .description(description)
                        .metadata(metadata)
                        .build();
            }
        }

        return data;
    }

    private Map<String, FeatureTypeConfigurationOgcApi> generateCollections(FeatureProvider2 featureProvider) {
        return featureProvider.getData()
                              .getTypes()
                              .values()
                              .stream()
                              .map(type -> {

                                  ImmutableList<String> spatialProperty = type.getProperties()
                                                                              .stream()
                                                                              .filter(FeatureSchema::isSpatial)
                                                                              .findFirst()
                                                                              .map(FeatureSchema::getName)
                                                                              .map(ImmutableList::of)
                                                                              .orElse(ImmutableList.of());

                                  ImmutableFeatureTypeConfigurationOgcApi collection =
                                          new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                                  .id(type.getName())
                                                  .label(type.getName())
                                                  .extent(new ImmutableCollectionExtent.Builder()
                                                          .spatialComputed(true)
                                                          .temporalComputed(true)
                                                          .build())
                                                  .addExtensions(new ImmutableFeaturesCoreConfiguration.Builder()
                                                          .enabled(true)
                                                          .queryables(new ImmutableFeaturesCollectionQueryables.Builder()
                                                                  .spatial(spatialProperty)
                                                                  .build())
                                                          .build())
                                                  .build();

                                  return new AbstractMap.SimpleImmutableEntry<>(type.getName(), collection);
                              })
                              .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean hasMissingBboxes(Map<String, FeatureTypeConfigurationOgcApi> featureTypes) {
        return featureTypes.values()
                           .stream()
                           .anyMatch(this::hasMissingBbox);
    }

    private boolean hasMissingBbox(FeatureTypeConfigurationOgcApi featureType) {
        return featureType.getExtent()
                          .flatMap(CollectionExtent::getSpatialComputed)
                          .orElse(false);
    }

    private ImmutableMap<String, FeatureTypeConfigurationOgcApi> computeMissingBboxes(
            OgcApiDataV2 apiData) throws IllegalStateException {


        return apiData.getCollections()
                      .entrySet()
                      .stream()
                      .map(entry -> {

                          FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, entry.getValue());

                          if (hasMissingBbox(entry.getValue()) && featureProvider.supportsExtents()) {
                              Optional<BoundingBox> spatialExtent = featureProvider.extents()
                                                                                   .getSpatialExtent(entry.getValue()
                                                                                                          .getId());

                              if (spatialExtent.isPresent()) {

                                  BoundingBox boundingBox = spatialExtent.get();
                                  if (!boundingBox.getEpsgCrs()
                                                  .equals(OgcCrs.CRS84) &&
                                      !boundingBox.getEpsgCrs()
                                                  .equals(OgcCrs.CRS84h)) {
                                      try {
                                          Optional<CrsTransformer> transformer = crsTransformerFactory.getTransformer(boundingBox.getEpsgCrs(), OgcCrs.CRS84);
                                          if (transformer.isPresent()) {
                                              boundingBox = transformer.get()
                                                                       .transformBoundingBox(boundingBox);
                                          }
                                      } catch (CrsTransformationException e) {
                                          throw new RuntimeException(String.format("Error transforming the bounding box with CRS '%s'", boundingBox.getEpsgCrs()
                                                                                                                                                       .toUriString()));
                                      }
                                  }

                                  ImmutableFeatureTypeConfigurationOgcApi featureTypeConfiguration;
                                  if (entry.getValue()
                                           .getExtent()
                                           .isPresent()) {
                                      featureTypeConfiguration = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                              .from(entry.getValue())
                                              .extent(new ImmutableCollectionExtent.Builder()
                                                      .from(entry.getValue()
                                                                 .getExtent()
                                                                 .get())
                                                      .spatial(boundingBox)
                                                      .build())
                                              .build();
                                  } else {
                                      featureTypeConfiguration = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                              .from(entry.getValue())
                                              .extent(new ImmutableCollectionExtent.Builder()
                                                      .spatial(boundingBox)
                                                      .build())
                                              .build();

                                  }

                                  return new AbstractMap.SimpleEntry<>(entry.getKey(), featureTypeConfiguration);
                              }
                          }
                          return entry;
                      })
                      .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean hasMissingIntervals(Map<String, FeatureTypeConfigurationOgcApi> featureTypes) {
        return featureTypes
                .values()
                .stream()
                .anyMatch(this::hasMissingInterval);
    }

    private boolean hasMissingInterval(FeatureTypeConfigurationOgcApi featureType) {
        return featureType.getExtent()
                          .flatMap(CollectionExtent::getTemporalComputed)
                          .orElse(false);
    }

    private ImmutableMap<String, FeatureTypeConfigurationOgcApi> computeMissingIntervals(OgcApiDataV2 apiData) {
        return apiData.getCollections()
                .entrySet()
                .stream()
                .map(entry -> {
                    FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, entry.getValue());

                    if (hasMissingInterval(entry.getValue()) && featureProvider.supportsExtents()) {

                        List<String> temporalQueryables = entry.getValue()
                                .getExtension(FeaturesCoreConfiguration.class)
                                .flatMap(FeaturesCoreConfiguration::getQueryables)
                                .map(FeaturesCollectionQueryables::getTemporal)
                                .orElse(ImmutableList.of());

                        if (!temporalQueryables.isEmpty()) {
                            Optional<Interval> interval;
                            if (temporalQueryables.size() >= 2) {
                                interval = featureProvider.extents()
                                        .getTemporalExtent(entry.getValue().getId(), temporalQueryables.get(0), temporalQueryables.get(1));
                            } else {
                                interval = featureProvider.extents()
                                        .getTemporalExtent(entry.getValue().getId(), temporalQueryables.get(0));
                            }

                            if (!interval.isPresent()) {
                                return entry;
                            }

                            TemporalExtent temporalExtent = new ImmutableTemporalExtent.Builder()
                                    .start(interval.get().getStart().toEpochMilli())
                                    .end(interval.get().isUnboundedEnd() ? null : interval.get().getEnd().toEpochMilli())
                                    .build();
                            ImmutableFeatureTypeConfigurationOgcApi featureTypeConfiguration;
                            if (entry.getValue().getExtent().isPresent()) {
                                featureTypeConfiguration = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                        .from(entry.getValue())
                                        .extent(new ImmutableCollectionExtent.Builder()
                                                .from(entry.getValue()
                                                        .getExtent()
                                                        .get())
                                                .temporal(temporalExtent)
                                                .build())
                                        .build();
                            } else {
                                featureTypeConfiguration = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                        .from(entry.getValue())
                                        .extent(new ImmutableCollectionExtent.Builder()
                                                .temporal(temporalExtent)
                                                .build())
                                        .build();
                            }
                            return new AbstractMap.SimpleEntry<>(entry.getKey(), featureTypeConfiguration);
                        }
                    }
                    return entry;
                })
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

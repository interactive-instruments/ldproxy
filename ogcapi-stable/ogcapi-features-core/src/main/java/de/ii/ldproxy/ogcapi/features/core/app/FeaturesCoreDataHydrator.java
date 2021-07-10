/**
 * Copyright 2021 interactive instruments GmbH
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
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreValidation;
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
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.util.ArrayList;
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
    private final FeaturesCoreValidation featuresCoreValidator;

    public FeaturesCoreDataHydrator(@Requires FeaturesCoreProviders providers,
                                    @Requires CrsTransformerFactory crsTransformerFactory,
                                    @Requires FeaturesCoreValidation featuresCoreValidator) {
        this.providers = providers;
        this.crsTransformerFactory = crsTransformerFactory;
        this.featuresCoreValidator = featuresCoreValidator;
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
        FeatureProvider2 featureProvider = providers.getFeatureProviderOrThrow(apiData);

        OgcApiDataV2 data = apiData;
        if (data.isAuto() && data.getCollections()
            .isEmpty()) {
          data = new ImmutableOgcApiDataV2.Builder()
              .from(data)
              .collections(generateCollections(featureProvider))
              .build();

        }

        Map<String, FeatureSchema> featureSchemas = providers.getFeatureSchemas(data);
        MODE apiValidation = data.getApiValidation();

        // The behaviour depends on the requested validation approach
        // NONE: no validation during hydration
        // LAX: try to remove invalid options, but start the service with the valid options, if possible
        // STRICT: no validation during hydration, validation will be done in onStartup() and startup will fail in case of an error

        // get Features Core configurations to process, normalize property names unless in STRICT mode
        Map<String, FeaturesCoreConfiguration> coreConfigs = data.getCollections()
                                                                 .entrySet()
                                                                 .stream()
                                                                 .map(entry -> {
                                                                     // normalize the property references in queryables and transformations
                                                                     // by removing all parts in square brackets unless in STRICT mode

                                                                     final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
                                                                     // cannot use collectionData.getExtension(FeaturesCoreConfiguration.class), it merges in the defaults, we don't want that in auto mode
                                                                     FeaturesCoreConfiguration config = collectionData.getExtensions()
                                                                         .stream()
                                                                         .filter(extensionConfiguration -> extensionConfiguration instanceof FeaturesCoreConfiguration)
                                                                         .map(extensionConfiguration -> (FeaturesCoreConfiguration)extensionConfiguration)
                                                                         .findFirst()
                                                                         .orElse(null);

                                                                     if (Objects.isNull(config))
                                                                         return null;

                                                                     final String collectionId = entry.getKey();
                                                                     final String buildingBlock = config.getBuildingBlock();

                                                                     if (apiValidation!= MODE.STRICT &&
                                                                         config.hasDeprecatedTransformationKeys())
                                                                         config = new ImmutableFeaturesCoreConfiguration.Builder()
                                                                                 .from(config)
                                                                                 .transformations(config.normalizeTransformationKeys(buildingBlock,collectionId))
                                                                                 .build();
                                                                      //TODO: move to immutable check method???
                                                                     if (apiValidation!= MODE.STRICT &&
                                                                         config.hasDeprecatedQueryables())
                                                                         config = new ImmutableFeaturesCoreConfiguration.Builder()
                                                                                 .from(config)
                                                                                 .queryables(config.normalizeQueryables(collectionId))
                                                                                 .build();

                                                                     return new AbstractMap.SimpleImmutableEntry<>(collectionId, config);
                                                                 })
                                                                 .filter(Objects::nonNull)
                                                                 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // update data with changes
        // also disable invalid collections where either the feature type is not found or has no property with role id
        // also update label and description, if we have information in the provider
        data = new ImmutableOgcApiDataV2.Builder()
                .from(data)
                .collections(
                        data.getCollections()
                            .entrySet()
                            .stream()
                            .map(entry -> {
                                final String collectionId = entry.getKey();
                                final Optional<FeaturesCoreConfiguration> coreConfiguration = Optional.ofNullable(coreConfigs.get(collectionId));
                                final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
                                final FeatureSchema schema = featureSchemas.get(collectionId);

                                final boolean featureTypeNotFound = Objects.isNull(schema);
                                final boolean idNotFound = !featureTypeNotFound && schema.getAllNestedProperties().stream().noneMatch(FeatureSchema::isId);
                                final boolean disabled = featureTypeNotFound || idNotFound;

                                if (featureTypeNotFound) {
                                  LOGGER.error("Collection '{}' has been disabled because its feature type was not found in the provider schema.", collectionId);
                                }
                                if (idNotFound) {
                                  LOGGER.error("Collection '{}' has been disabled because its feature type has no property with role ID in the provider schema.", collectionId);
                                }

                                return new AbstractMap.SimpleImmutableEntry<String, FeatureTypeConfigurationOgcApi>(collectionId, new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                        .from(entry.getValue())
                                        // disable if feature type or id not found
                                        .enabled(collectionData.getEnabled() && !disabled)
                                        // use the type label from the provider, if the service configuration has just the default label
                                        .label(collectionData.getLabel().equals(collectionId) && Objects.nonNull(schema) && schema.getLabel().isPresent()
                                                       ? schema.getLabel().get()
                                                       : collectionData.getLabel())
                                        // use the type description from the provider, if the service configuration does not have one
                                        .description(Optional.ofNullable(collectionData.getDescription()
                                                                                       .orElse(Objects.nonNull(schema) ? schema.getDescription()
                                                                                                                               .orElse(null)
                                                                                                                       : null )))
                                        .extensions(coreConfiguration.isPresent()
                                            ? new ImmutableList.Builder<ExtensionConfiguration>()
                                                // do not touch any other extension
                                                .addAll(entry.getValue()
                                                                  .getExtensions()
                                                                  .stream()
                                                                  .filter(ext -> !ext.getBuildingBlock().equals(coreConfiguration.get().getBuildingBlock()))
                                                                  .collect(Collectors.toUnmodifiableList()))
                                                // add the modified Features Core configuration
                                                .add(coreConfiguration.get())
                                                .build()
                                            : entry.getValue()
                                                    .getExtensions()
                                        )
                                        .build());
                            })
                            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
                .build();

        if (!data.isAutoPersist() && hasMissingBboxes(data)) {
            data = new ImmutableOgcApiDataV2.Builder()
                    .from(data)
                    .collections(computeMissingBboxes(data))
                    .build();
        }

        if (!data.isAutoPersist() && hasMissingIntervals(data)) {
            data = new ImmutableOgcApiDataV2.Builder()
                    .from(data)
                    .collections(computeMissingIntervals(data))
                    .build();
        }

        if (data.isAuto() && data.isAutoPersist()) {
            data = new ImmutableOgcApiDataV2.Builder()
                .from(data)
                .auto(Optional.empty())
                .autoPersist(Optional.empty())
                .build();
        }

        if (!data.getMetadata().isPresent() && featureProvider.supportsMetadata()) {
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

                                ImmutableList<String> temporalProperty = type.getProperties()
                                    .stream()
                                    .filter(FeatureSchema::isTemporal)
                                    .findFirst()
                                    .map(FeatureSchema::getName)
                                    .map(ImmutableList::of)
                                    .orElse(ImmutableList.of());

                                List<ExtensionConfiguration> extensions = new ArrayList<>();

                                if (!spatialProperty.isEmpty() || !temporalProperty.isEmpty()) {
                                  extensions.add(new ImmutableFeaturesCoreConfiguration.Builder()
                                      .queryables(new ImmutableFeaturesCollectionQueryables.Builder()
                                          .spatial(spatialProperty)
                                          .temporal(temporalProperty)
                                          .build())
                                      .build());
                                }

                                  ImmutableFeatureTypeConfigurationOgcApi collection =
                                          new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                                  .id(type.getName())
                                                  .label(type.getName())
                                                  .extensions(extensions)
                                                  .build();

                                  return new AbstractMap.SimpleImmutableEntry<>(type.getName(), collection);
                              })
                              .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean hasMissingBboxes(OgcApiDataV2 data) {
        return data.getCollections().values()
                           .stream()
                           .anyMatch(collection -> collection.getEnabled() && hasMissingBbox(data, collection.getId()));
    }

    private boolean hasMissingBbox(OgcApiDataV2 data, String collectionId) {
        return data.getExtent(collectionId)
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

                          if (hasMissingBbox(apiData, entry.getValue().getId()) && featureProvider.supportsExtents()) {
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

    private boolean hasMissingIntervals(OgcApiDataV2 data) {
        return data.getCollections()
                .values()
                .stream()
                .anyMatch(collection -> collection.getEnabled() && hasMissingInterval(data, collection.getId()));
    }

    private boolean hasMissingInterval(OgcApiDataV2 data, String collectionId) {
        return data.getExtent(collectionId)
                          .flatMap(CollectionExtent::getTemporalComputed)
                          .orElse(false);
    }

    private ImmutableMap<String, FeatureTypeConfigurationOgcApi> computeMissingIntervals(OgcApiDataV2 apiData) {
        return apiData.getCollections()
                .entrySet()
                .stream()
                .map(entry -> {
                    FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, entry.getValue());

                    if (hasMissingInterval(apiData, entry.getValue().getId()) && featureProvider.supportsExtents()) {

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

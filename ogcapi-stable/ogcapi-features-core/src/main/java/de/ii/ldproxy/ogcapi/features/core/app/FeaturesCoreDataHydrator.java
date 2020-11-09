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
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableCollectionExtent;
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableMetadata;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataHydratorExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
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

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class FeaturesCoreDataHydrator implements OgcApiDataHydratorExtension {

    private final FeaturesCoreProviders providers;
    private final CrsTransformerFactory crsTransformerFactory;

    public FeaturesCoreDataHydrator(@Requires FeaturesCoreProviders providers,
                                    @Requires CrsTransformerFactory crsTransformerFactory) {
        this.providers = providers;
        this.crsTransformerFactory = crsTransformerFactory;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }

    @Override
    public OgcApiDataV2 getHydratedData(OgcApiDataV2 apiData) {

        OgcApiDataV2 data = apiData;
        FeatureProvider2 featureProvider = providers.getFeatureProvider(data);

        if (data.isAuto() && data.getCollections()
                                  .isEmpty()) {
            data = new ImmutableOgcApiDataV2.Builder()
                    .from(data)
                    .collections(generateCollections(featureProvider))
                    .build();
        }

        if (!data.isAutoPersist() && hasMissingBboxes(data.getCollections())) {
            data = new ImmutableOgcApiDataV2.Builder()
                    .from(data)
                    .collections(computeMissingBboxes(data))
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
                        :providerMetadata.flatMap(Metadata::getFees);

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
        return featureTypes
                .entrySet()
                .stream()
                .anyMatch(entry -> entry.getValue()
                                        .getExtent()
                                        .isPresent() &&
                        entry.getValue()
                             .getExtent()
                             .get()
                             .getSpatialComputed());
    }

    private ImmutableMap<String, FeatureTypeConfigurationOgcApi> computeMissingBboxes(
            OgcApiDataV2 apiData) throws IllegalStateException {


        return apiData.getCollections()
                      .entrySet()
                      .stream()
                      .map(entry -> {

                          FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, entry.getValue());

                          if (entry.getValue()
                                   .getExtent()
                                   .isPresent() &&
                                  entry.getValue()
                                       .getExtent()
                                       .get()
                                       .getSpatialComputed() &&
                                  featureProvider.supportsExtents()) {
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
}

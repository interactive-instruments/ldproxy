/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.api;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableCollectionExtent;
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataHydratorExtension;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class OgcApiFeatureCoreDataHydrator implements OgcApiDataHydratorExtension {

    private final OgcApiFeatureCoreProviders providers;

    public OgcApiFeatureCoreDataHydrator(@Requires OgcApiFeatureCoreProviders providers) {
        this.providers = providers;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return getExtensionConfiguration(apiData, OgcApiFeaturesCoreConfiguration.class).filter(OgcApiFeaturesCoreConfiguration::getEnabled)
                                                                                        .isPresent();
    }

    @Override
    public OgcApiApiDataV2 getHydratedData(OgcApiApiDataV2 apiData) {

        OgcApiApiDataV2 newData = apiData;

        if (hasMissingBboxes(apiData.getCollections())) {

            ImmutableMap<String, FeatureTypeConfigurationOgcApi> collectionsWithComputedBboxes = computeMissingBboxes(apiData);

            newData = new ImmutableOgcApiApiDataV2.Builder()
                    .from(newData)
                    .collections(collectionsWithComputedBboxes)
                    .build();
        }

        return newData;
    }

    private boolean hasMissingBboxes(Map<String, FeatureTypeConfigurationOgcApi> featureTypes) {
        return featureTypes
                .entrySet()
                .stream()
                .anyMatch(entry -> entry.getValue()
                                        .getExtent()
                                        .getSpatialComputed());
    }

    //TODO Test
    private ImmutableMap<String, FeatureTypeConfigurationOgcApi> computeMissingBboxes(
            OgcApiApiDataV2 apiData) throws IllegalStateException {


        return apiData.getCollections()
                      .entrySet()
                      .stream()
                      .map(entry -> {

                          FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, entry.getValue());

                          if (entry.getValue()
                                   .getExtent()
                                   .getSpatialComputed() && featureProvider.supportsExtents()) {
                              Optional<BoundingBox> spatialExtent = featureProvider.extents()
                                                                                   .getSpatialExtent(entry.getValue()
                                                                                                          .getId());

                              if (spatialExtent.isPresent()) {

                                  ImmutableFeatureTypeConfigurationOgcApi featureTypeConfiguration = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                          .from(entry.getValue())
                                          .extent(new ImmutableCollectionExtent.Builder()
                                                  .from(entry.getValue()
                                                             .getExtent())
                                                  .spatial(spatialExtent.get())
                                                  .build())
                                          .build();


                                  return new AbstractMap.SimpleEntry<>(entry.getKey(), featureTypeConfiguration);
                              }
                          }
                          return entry;
                      })
                      .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

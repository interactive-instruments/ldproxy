/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataHydratorExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreValidator;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class GeojsonDataHydrator implements OgcApiDataHydratorExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeojsonDataHydrator.class);

    private final FeaturesCoreProviders providers;

    public GeojsonDataHydrator(@Requires FeaturesCoreProviders providers) {
        this.providers = providers;
    }

    @Override
    public int getSortPriority() {
        // this must be processed after the FeaturesCoreDataHydrator
        return 110;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return GeoJsonConfiguration.class;
    }

    @Override
    public OgcApiDataV2 getHydratedData(OgcApiDataV2 apiData) {

        // any GeoJSON hydration actions are not taken in STRICT validation mode;
        // STRICT: an invalid service definition will not start
        if (apiData.getApiValidation()==FeatureProviderDataV2.VALIDATION.STRICT)
            return apiData;

        OgcApiDataV2 data = apiData;

        // get Features Core configurations to process, normalize property names
        Map<String, GeoJsonConfiguration> configs = data.getCollections()
                                                                 .entrySet()
                                                                 .stream()
                                                                 .map(entry -> {
                                                                     // * normalize the property references in transformations by removing all parts in square brackets

                                                                     final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
                                                                     GeoJsonConfiguration config = collectionData.getExtension(GeoJsonConfiguration.class).orElse(null);
                                                                     if (Objects.isNull(config))
                                                                         return null;

                                                                     final String collectionId = entry.getKey();
                                                                     final String buildingBlock = config.getBuildingBlock();

                                                                     if (config.hasDeprecatedTransformationKeys())
                                                                         config = new ImmutableGeoJsonConfiguration.Builder()
                                                                                 .from(config)
                                                                                 .transformations(config.normalizeTransformationKeys(buildingBlock,collectionId))
                                                                                 .build();

                                                                     return new AbstractMap.SimpleImmutableEntry<>(collectionId, config);
                                                                 })
                                                                 .filter(Objects::nonNull)
                                                                 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (data.getApiValidation()==FeatureProviderDataV2.VALIDATION.LAX) {
            // LAX: process configuration and remove invalid configuration elements

            Map<String, FeatureSchema> featureSchemas = providers.getFeatureSchemas(apiData);

            // 2. remove invalid transformation keys
            Map<String, Collection<String>> keyMap = configs.entrySet()
                                                                .stream()
                                                                .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()
                                                                                                                                          .getTransformations()
                                                                                                                                          .keySet()))
                                                                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
            final Map<String, Collection<String>> invalidTransformationKeys = FeaturesCoreValidator.getInvalidPropertyKeys(keyMap, featureSchemas);
            invalidTransformationKeys.entrySet()
                                     .stream()
                                     .forEach(entry -> {
                                         String collectionId = entry.getKey();
                                         entry.getValue()
                                              .forEach(property -> LOGGER.warn("A transformation for property '{}' in collection '{}' is invalid, because the property was not found in the provider schema. The transformation has been dropped during hydration.", property, collectionId));
                                         configs.put(collectionId, new ImmutableGeoJsonConfiguration.Builder()
                                                 .from(configs.get(collectionId))
                                                 .transformations(configs.get(collectionId).removeTransformations(invalidTransformationKeys.get(collectionId)))
                                                 .build());
                                     });
        }

        // update data with changes
        // also update label and description, if we have information in the provider
        data = new ImmutableOgcApiDataV2.Builder()
                .from(data)
                .collections(
                        data.getCollections()
                            .entrySet()
                            .stream()
                            .map(entry -> {
                                final String collectionId = entry.getKey();
                                if (!configs.containsKey(collectionId))
                                    return entry;

                                final GeoJsonConfiguration config = configs.get(collectionId);
                                final String buildingBlock = config.getBuildingBlock();

                                return new AbstractMap.SimpleImmutableEntry<String, FeatureTypeConfigurationOgcApi>(collectionId, new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                        .from(entry.getValue())
                                        .extensions(new ImmutableList.Builder<ExtensionConfiguration>()
                                                            // do not touch any other extension
                                                            .addAll(entry.getValue()
                                                                         .getExtensions()
                                                                         .stream()
                                                                         .filter(ext -> !ext.getBuildingBlock().equals(buildingBlock))
                                                                         .collect(Collectors.toUnmodifiableList()))
                                                            // add the GeoJSON configuration
                                                            .add(config)
                                                            .build()
                                        )
                                        .build());
                            })
                            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
                .build();

        return data;
    }
}

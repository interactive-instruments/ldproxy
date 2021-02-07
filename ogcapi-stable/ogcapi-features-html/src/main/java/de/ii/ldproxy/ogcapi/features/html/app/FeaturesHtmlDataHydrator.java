/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataHydratorExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.ImmutableFeaturesHtmlConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class FeaturesHtmlDataHydrator implements OgcApiDataHydratorExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesHtmlDataHydrator.class);

    private final FeaturesCoreProviders providers;

    public FeaturesHtmlDataHydrator(@Requires FeaturesCoreProviders providers) {
        this.providers = providers;
    }

    @Override
    public int getSortPriority() {
        // this must be processed after the FeaturesCoreDataHydrator
        return 120;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesHtmlConfiguration.class;
    }

    @Override
    public OgcApiDataV2 getHydratedData(OgcApiDataV2 apiData) {

        OgcApiDataV2 data = apiData;
        FeatureProvider2 featureProvider = providers.getFeatureProvider(data);

        // process configuration and remove invalid configuration elements
        data = new ImmutableOgcApiDataV2.Builder()
                .from(data)
                .collections(
                        data.getCollections()
                            .entrySet()
                            .stream()
                            .map(entry -> {
                                // * remove unknown transformations from the configuration
                                // * normalize the property references in transformations by removing all parts in square brackets

                                final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
                                final FeaturesCoreConfiguration coreConfig = collectionData.getExtension(FeaturesCoreConfiguration.class).orElse(null);
                                final FeaturesHtmlConfiguration config = collectionData.getExtension(FeaturesHtmlConfiguration.class).orElse(null);
                                if (Objects.isNull(config))
                                    return entry;

                                final String buildingBlock = config.getBuildingBlock();
                                final String collectionId = entry.getKey();
                                final FeatureSchema schema = featureProvider.getData()
                                                                            .getTypes()
                                                                            .get(coreConfig.getFeatureType().orElse(collectionId));

                                return new AbstractMap.SimpleImmutableEntry<String, FeatureTypeConfigurationOgcApi>(collectionId, new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                        .from(entry.getValue())
                                        // validate and update the Features HTML configuration
                                        .extensions(new ImmutableList.Builder<ExtensionConfiguration>()
                                                            // do not touch any other extension
                                                            .addAll(entry.getValue()
                                                                         .getExtensions()
                                                                         .stream()
                                                                         .filter(ext -> !ext.getBuildingBlock().equals(buildingBlock))
                                                                         .collect(Collectors.toUnmodifiableList()))
                                                            // process the Features HTML configuration
                                                            .add(new ImmutableFeaturesHtmlConfiguration.Builder()
                                                                         .from(config)
                                                                         .transformations(config.validateTransformations(buildingBlock,collectionId,schema))
                                                                         .build())
                                                            .build()
                                        )
                                        .build());
                            })
                            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
                .build();

        return data;
    }
}

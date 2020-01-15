/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.application;

import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class OgcApiFeaturesCollectionsExtension implements OgcApiCollectionsExtension {

    private final OgcApiExtensionRegistry extensionRegistry;

    public OgcApiFeaturesCollectionsExtension(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, OgcApiFeaturesCoreConfiguration.class);
    }

    @Override
    public ImmutableCollections.Builder process(ImmutableCollections.Builder collectionsBuilder, OgcApiDatasetData apiData,
                                                URICustomizer uriCustomizer,
                                                OgcApiMediaType mediaType,
                                                List<OgcApiMediaType> alternateMediaTypes,
                                                Optional<Locale> language) {

        if (!isEnabledForApi(apiData)) {
            return collectionsBuilder;
        }

        List<OgcApiCollectionExtension> collectionExtenders = extensionRegistry.getExtensionsForType(OgcApiCollectionExtension.class);

        List<OgcApiCollection> collections = apiData.getFeatureTypes()
                                                      .values()
                                                      .stream()
                                                      .filter(featureType -> apiData.isCollectionEnabled(featureType.getId()))
                                                      .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                                                      .map(featureType -> OgcApiFeaturesCollectionExtension.createNestedCollection(featureType, apiData, mediaType, alternateMediaTypes, language, uriCustomizer, collectionExtenders))
                                                      .collect(Collectors.toList());

        collectionsBuilder.addAllCollections(collections);

        return collectionsBuilder; // TODO .addSections(ImmutableMap.of("collections", collections));
    }
}

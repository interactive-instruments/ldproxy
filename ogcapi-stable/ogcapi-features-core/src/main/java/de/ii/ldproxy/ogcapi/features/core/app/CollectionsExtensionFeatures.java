/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionExtension;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsExtension;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableCollections;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.URICustomizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class CollectionsExtensionFeatures implements CollectionsExtension {

    private final ExtensionRegistry extensionRegistry;

    @Inject
    public CollectionsExtensionFeatures(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }

    @Override
    public ImmutableCollections.Builder process(ImmutableCollections.Builder collectionsBuilder, OgcApiDataV2 apiData,
                                                URICustomizer uriCustomizer,
                                                ApiMediaType mediaType,
                                                List<ApiMediaType> alternateMediaTypes,
                                                Optional<Locale> language) {

        if (!isEnabledForApi(apiData)) {
            return collectionsBuilder;
        }

        List<CollectionExtension> collectionExtenders = extensionRegistry.getExtensionsForType(CollectionExtension.class);

        List<OgcApiCollection> collections = apiData.getCollections()
                                                      .values()
                                                      .stream()
                                                      .filter(featureType -> apiData.isCollectionEnabled(featureType.getId()))
                                                      .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                                                      .map(featureType -> CollectionExtensionFeatures.createNestedCollection(featureType, apiData, mediaType, alternateMediaTypes, language, uriCustomizer, collectionExtenders))
                                                      .collect(Collectors.toList());

        collectionsBuilder.addAllCollections(collections);

        return collectionsBuilder; // TODO .addSections(ImmutableMap.of("collections", collections));
    }
}

/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionExtension;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiCollection;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.observation_processing.parameters.PathParameterCollectionIdProcess;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class ObservationProcessingOnCollection implements CollectionExtension {

    @Requires
    I18n i18n;

    private final ExtensionRegistry extensionRegistry;

    public ObservationProcessingOnCollection(@Requires ExtensionRegistry extensionRegistry,
                                             @Requires FeaturesCoreProviders providers) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ObservationProcessingConfiguration.class;
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                                     FeatureTypeConfigurationOgcApi featureType,
                                                     OgcApiDataV2 apiData, URICustomizer uriCustomizer,
                                                     boolean isNested, ApiMediaType mediaType,
                                                     List<ApiMediaType> alternateMediaTypes,
                                                     Optional<Locale> language) {
        if (isNested)
            return collection;

        List<PathParameterCollectionIdProcess> params = extensionRegistry.getExtensionsForType(PathParameterCollectionIdProcess.class);
        if (params.size()!=1)
            return collection;

        // get endpoints
        if (isExtensionEnabled(featureType, ObservationProcessingConfiguration.class) &&
            params.get(0).getValues(apiData).contains(featureType.getId())) {
            final ObservationProcessingLinksGenerator linkGenerator = new ObservationProcessingLinksGenerator();
            collection.addAllLinks(linkGenerator.generateCollectionLinks(featureType, uriCustomizer, i18n, language));
        }

        return collection;
    }
}

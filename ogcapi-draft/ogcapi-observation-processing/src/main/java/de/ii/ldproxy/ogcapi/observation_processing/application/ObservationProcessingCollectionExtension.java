/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
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
public class ObservationProcessingCollectionExtension implements OgcApiCollectionExtension {

    @Requires
    I18n i18n;

    private final OgcApiExtensionRegistry extensionRegistry;

    public ObservationProcessingCollectionExtension(@Requires OgcApiExtensionRegistry extensionRegistry,
                                                    @Requires OgcApiFeatureCoreProviders providers) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class);
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                                     FeatureTypeConfigurationOgcApi featureType,
                                                     OgcApiApiDataV2 apiData, URICustomizer uriCustomizer,
                                                     boolean isNested, OgcApiMediaType mediaType,
                                                     List<OgcApiMediaType> alternateMediaTypes,
                                                     Optional<Locale> language) {
        if (isNested)
            return collection;

        List<PathParameterCollectionIdProcess> params = extensionRegistry.getExtensionsForType(PathParameterCollectionIdProcess.class);
        if (params.size()!=1)
            return collection;

        // get endpoints
        // TODO check that the collection meets the requirements
        // TODO add links to processing resources
        if (isExtensionEnabled(apiData, featureType, ObservationProcessingConfiguration.class) &&
            params.get(0).getValues(apiData).contains(featureType.getId())) {
            final ObservationProcessingLinksGenerator linkGenerator = new ObservationProcessingLinksGenerator();
            collection.addAllLinks(linkGenerator.generateCollectionLinks(uriCustomizer, i18n, language));
        }

        return collection;
    }
}

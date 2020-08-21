/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.common.domain.ImmutableLandingPage;
import de.ii.ldproxy.ogcapi.common.domain.LandingPageExtension;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessChain;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
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
public class ObservationProcessingOnLandingPage implements LandingPageExtension {

    @Requires
    I18n i18n;

    private final ExtensionRegistry extensionRegistry;
    private final FeatureProcessInfo featureProcessInfo;

    public ObservationProcessingOnLandingPage(@Requires ExtensionRegistry extensionRegistry,
                                              @Requires FeaturesCoreProviders providers,
                                              @Requires FeatureProcessInfo featureProcessInfo) {
        this.extensionRegistry = extensionRegistry;
        this.featureProcessInfo = featureProcessInfo;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ObservationProcessingConfiguration.class;
    }

    @Override
    public ImmutableLandingPage.Builder process(ImmutableLandingPage.Builder landingPageBuilder,
                                                OgcApiDataV2 apiData, URICustomizer uriCustomizer,
                                                ApiMediaType mediaType, List<ApiMediaType> alternateMediaTypes,
                                                Optional<Locale> language) {

        List<PathParameterCollectionIdProcess> params = extensionRegistry.getExtensionsForType(PathParameterCollectionIdProcess.class);
        if (params.size()!=1)
            return landingPageBuilder;

        final ObservationProcessingLinksGenerator linkGenerator = new ObservationProcessingLinksGenerator();
        List<FeatureProcessChain> chains = featureProcessInfo.getProcessingChains(apiData, ObservationProcess.class);

        apiData.getCollections()
                .values()
                .stream()
                .filter(featureType -> featureType.getEnabled())
                .filter(featureType -> chains.stream()
                                             .filter(chain -> chain.asList().get(0).getSupportedCollections(apiData).contains(featureType.getId()))
                                             .findAny()
                                             .isPresent())
                .forEach(featureType -> {
                    landingPageBuilder.addAllLinks(linkGenerator.generateLandingPageLinks(featureType, uriCustomizer, i18n, language));

                });

        return landingPageBuilder;
    }
}

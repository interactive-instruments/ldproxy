/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.crs.app;


import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionExtension;
import de.ii.ldproxy.ogcapi.crs.domain.CrsConfiguration;
import de.ii.ldproxy.ogcapi.crs.domain.CrsSupport;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiCollection;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * add CRS information to the collection information
 */
@Component
@Provides
@Instantiate
public class CrsOnCollection implements CollectionExtension {

    private final FeaturesCoreProviders providers;
    private final CrsSupport crsSupport;

    public CrsOnCollection(@Requires FeaturesCoreProviders providers,
                           @Requires CrsSupport crsSupport) {
        this.providers = providers;
        this.crsSupport = crsSupport;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CrsConfiguration.class;
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                                     FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                     OgcApiDataV2 apiData,
                                                     URICustomizer uriCustomizer,
                                                     boolean isNested,
                                                     ApiMediaType mediaType,
                                                     List<ApiMediaType> alternateMediaTypes,
                                                     Optional<Locale> language) {
        boolean hasGeometry = featureTypeConfiguration.getExtension(FeaturesCoreConfiguration.class)
                .flatMap(FeaturesCoreConfiguration::getQueryables)
                .map(FeaturesCollectionQueryables::getSpatial)
                .filter(spatial -> !spatial.isEmpty())
                .isPresent();
        if (isExtensionEnabled(featureTypeConfiguration, CrsConfiguration.class) && hasGeometry) {
            List<String> crsList;
            if (isNested) {
                // just reference the default list of coordinate reference systems
                crsList = ImmutableList.of("#/crs");
            } else {
                // this is just the collection resource, so no default to reference; include all CRSs
                crsList = crsSupport.getSupportedCrsList(apiData, featureTypeConfiguration)
                                    .stream()
                                    .map(EpsgCrs::toUriString)
                                    .collect(ImmutableList.toImmutableList());
            }
            collection.crs(crsList);

            String storageCrsUri = crsSupport.getStorageCrs(apiData, Optional.of(featureTypeConfiguration))
                                             .toUriString();

            // add native CRS as storageCRS
            collection.storageCrs(storageCrsUri);
        }

        return collection;
    }

}

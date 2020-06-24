/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.crs;


import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiCollection;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiCollectionExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
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
public class OgcApiCollectionCrs implements OgcApiCollectionExtension {

    private final OgcApiFeatureCoreProviders providers;
    private final CrsSupport crsSupport;

    public OgcApiCollectionCrs(@Requires OgcApiFeatureCoreProviders providers,
                               @Requires CrsSupport crsSupport) {
        this.providers = providers;
        this.crsSupport = crsSupport;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, CrsConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData, apiData.getCollections().get(collectionId), CrsConfiguration.class);
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                                     FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                     OgcApiApiDataV2 apiData,
                                                     URICustomizer uriCustomizer,
                                                     boolean isNested,
                                                     OgcApiMediaType mediaType,
                                                     List<OgcApiMediaType> alternateMediaTypes,
                                                     Optional<Locale> language) {
        boolean hasGeometry = featureTypeConfiguration.getExtension(OgcApiFeaturesCoreConfiguration.class)
                .flatMap(OgcApiFeaturesCoreConfiguration::getQueryables)
                .map(OgcApiFeaturesCollectionQueryables::getSpatial)
                .filter(spatial -> !spatial.isEmpty())
                .isPresent();
        if (isExtensionEnabled(apiData, featureTypeConfiguration, CrsConfiguration.class) && hasGeometry) {
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

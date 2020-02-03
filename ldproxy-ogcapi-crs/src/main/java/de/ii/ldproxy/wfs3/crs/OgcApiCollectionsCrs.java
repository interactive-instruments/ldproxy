/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.crs;


import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ImmutableCollections;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiCollectionsExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.xtraplatform.geometries.domain.EpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * add CRS information to the collection information (default list of coordinate reference systems)
 */
@Component
@Provides
@Instantiate
public class OgcApiCollectionsCrs implements OgcApiCollectionsExtension {

    private final OgcApiFeatureCoreProviders providers;

    public OgcApiCollectionsCrs(@Requires OgcApiFeatureCoreProviders providers) {
        this.providers = providers;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, CrsConfiguration.class);
    }

    @Override
    public ImmutableCollections.Builder process(ImmutableCollections.Builder collectionsBuilder,
                                                OgcApiApiDataV2 apiData,
                                                URICustomizer uriCustomizer,
                                                OgcApiMediaType mediaType,
                                                List<OgcApiMediaType> alternateMediaTypes,
                                                Optional<Locale> language) {
        if (isExtensionEnabled(apiData, CrsConfiguration.class)) {
            // list all CRSs as the list of default CRSs
            ImmutableList<String> crsList =
                    Stream.concat(
                            Stream.of(
                                    OgcApiApiDataV2.DEFAULT_CRS_URI,
                                    providers.getFeatureProvider(apiData)
                                             .getData()
                                           .getNativeCrs()
                                           .toUriString()
                            ),
                            apiData.getAdditionalCrs()
                                   .stream()
                                   .map(EpsgCrs::toUriString)
                    )
                          .distinct()
                          .collect(ImmutableList.toImmutableList());

            collectionsBuilder.crs(crsList);
        }

        return collectionsBuilder;
    }

}

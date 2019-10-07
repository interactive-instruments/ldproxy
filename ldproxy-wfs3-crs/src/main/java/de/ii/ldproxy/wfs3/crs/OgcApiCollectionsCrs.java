/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.crs;


import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * add CRS information to the collection information (default list of coordinate reference systems)
 */
@Component
@Provides
@Instantiate
public class OgcApiCollectionsCrs implements OgcApiCollectionsExtension {

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, CrsConfiguration.class);
    }

    @Override
    public ImmutableCollections.Builder process(ImmutableCollections.Builder collectionsBuilder,
                                                OgcApiDatasetData apiData,
                                                URICustomizer uriCustomizer,
                                                OgcApiMediaType mediaType,
                                                List<OgcApiMediaType> alternateMediaTypes,
                                                Optional<Locale> language) {
        if (isExtensionEnabled(apiData, CrsConfiguration.class)) {
            // list all CRSs as the list of default CRSs
            ImmutableList<String> crsList = ImmutableList.<String>builder()
                        .add(apiData.getFeatureProvider()
                                .getNativeCrs()
                                .getAsUri())
                        .addAll(apiData.getAdditionalCrs()
                                .stream()
                                .map(EpsgCrs::getAsUri)
                                .collect(Collectors.toList()))
                        .build();
            collectionsBuilder.crs(crsList);
        }

        return collectionsBuilder;
    }

}

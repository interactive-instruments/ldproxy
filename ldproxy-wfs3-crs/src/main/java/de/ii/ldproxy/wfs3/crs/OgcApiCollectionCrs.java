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
 * add CRS information to the collection information
 */
@Component
@Provides
@Instantiate
public class OgcApiCollectionCrs implements OgcApiCollectionExtension {

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, CrsConfiguration.class);
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                                     FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                     OgcApiDatasetData apiData,
                                                     URICustomizer uriCustomizer,
                                                     boolean isNested,
                                                     OgcApiMediaType mediaType,
                                                     List<OgcApiMediaType> alternateMediaTypes,
                                                     Optional<Locale> language) {
        if (isExtensionEnabled(apiData, featureTypeConfiguration, CrsConfiguration.class)) {
            ImmutableList<String> crsList;
            if (isNested) {
                // just reference the default list of coordinate reference systems
                crsList = ImmutableList.<String>builder()
                        .add("#/crs")
                        .build();
            } else {
                // this is just the collection resource, so no default to reference; include all CRSs
                crsList = ImmutableList.<String>builder()
                        .add(apiData.getFeatureProvider()
                                    .getNativeCrs()
                                    .getAsUri())
                        .addAll(apiData.getAdditionalCrs()
                                .stream()
                                .map(EpsgCrs::getAsUri)
                                .collect(Collectors.toList()))
                        .build();
            }
            collection.crs(crsList);

            // add native CRS as storageCRS
            String storageCrs = apiData.getFeatureProvider()
                                .getNativeCrs()
                                .getAsUri();
            collection.storageCrs(storageCrs);
        }

        return collection;
    }

}

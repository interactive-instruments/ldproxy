/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.crs;


import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiCollection;
import de.ii.ldproxy.ogcapi.domain.OgcApiCollectionExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

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
                crsList = ImmutableList.of("#/crs");
            } else {
                // this is just the collection resource, so no default to reference; include all CRSs
                crsList = Stream.concat(
                        Stream.of(
                                OgcApiDatasetData.DEFAULT_CRS_URI,
                                apiData.getFeatureProvider()
                                       .getNativeCrs()
                                       .getAsUri()
                        ),
                        apiData.getAdditionalCrs()
                               .stream()
                               .map(EpsgCrs::getAsUri)
                )
                                .distinct()
                                .collect(ImmutableList.toImmutableList());
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

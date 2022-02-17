/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.crs.app;


import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableCollections;
import de.ii.ldproxy.ogcapi.crs.domain.CrsConfiguration;
import de.ii.ldproxy.ogcapi.crs.domain.CrsSupport;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.URICustomizer;
import de.ii.xtraplatform.crs.domain.EpsgCrs;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * add CRS information to the collection information (default list of coordinate reference systems)
 */
@Singleton
@AutoBind
public class CrsOnCollections implements CollectionsExtension {

    private final CrsSupport crsSupport;

    @Inject
    public CrsOnCollections(CrsSupport crsSupport) {
        this.crsSupport = crsSupport;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CrsConfiguration.class;
    }

    @Override
    public ImmutableCollections.Builder process(ImmutableCollections.Builder collectionsBuilder,
                                                OgcApiDataV2 apiData,
                                                URICustomizer uriCustomizer,
                                                ApiMediaType mediaType,
                                                List<ApiMediaType> alternateMediaTypes,
                                                Optional<Locale> language) {
        if (isExtensionEnabled(apiData, CrsConfiguration.class)) {

            // list all CRSs as the list of default CRSs
            ImmutableList<String> crsList = crsSupport.getSupportedCrsList(apiData)
                                                      .stream()
                                                      .map(EpsgCrs::toUriString)
                                                      .collect(ImmutableList.toImmutableList());

            collectionsBuilder.crs(crsList);
        }

        return collectionsBuilder;
    }

}

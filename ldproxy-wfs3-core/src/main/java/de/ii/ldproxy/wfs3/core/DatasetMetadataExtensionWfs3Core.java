/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.OgcApiCollection;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class DatasetMetadataExtensionWfs3Core implements OgcApiLandingPageExtension {

    @Requires
    private Wfs3Core wfs3Core;

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, Wfs3CoreConfiguration.class);
    }

    @Override
    public ImmutableDataset.Builder process(ImmutableDataset.Builder datasetBuilder, OgcApiDatasetData apiData,
                                            URICustomizer uriCustomizer,
                                            OgcApiMediaType mediaType,
                                            List<OgcApiMediaType> alternateMediaTypes) {

        if (!isEnabledForApi(apiData)) {
            return datasetBuilder;
        }

        //TODO
        List<OgcApiCollection> collections = apiData.getFeatureTypes()
                                                      .values()
                                                      .stream()
                                                      //TODO
                                                      .filter(featureType -> apiData.isFeatureTypeEnabled(featureType.getId()))
                                                      .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                                                      .map(featureType -> wfs3Core.createCollection(featureType, apiData, mediaType, alternateMediaTypes, uriCustomizer, true))
                                                      .collect(Collectors.toList());

        return datasetBuilder.addSections(ImmutableMap.of("collections", collections));
    }
}

/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.domain.Wfs3DatasetMetadataExtension;
import de.ii.ldproxy.ogcapi.domain.Wfs3Collection;
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
public class DatasetMetadataExtensionWfs3Core implements Wfs3DatasetMetadataExtension {

    @Requires
    private Wfs3Core wfs3Core;

    @Override
    public ImmutableDataset.Builder process(ImmutableDataset.Builder datasetBuilder, OgcApiDatasetData datasetData,
                                            URICustomizer uriCustomizer,
                                            OgcApiMediaType mediaType,
                                            List<OgcApiMediaType> alternativeMediaTypes) {

        if (!isExtensionEnabled(datasetData, Wfs3CoreConfiguration.class)) {
            return datasetBuilder;
        }

        //TODO
        List<Wfs3Collection> collections = datasetData.getFeatureTypes()
                                                      .values()
                                                      .stream()
                                                      //TODO
                                                      .filter(featureType -> datasetData.isFeatureTypeEnabled(featureType.getId()))
                                                      .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                                                      .map(featureType -> wfs3Core.createCollection(featureType, datasetData, mediaType, alternativeMediaTypes, uriCustomizer, true))
                                                      .collect(Collectors.toList());

        return datasetBuilder.addSections(ImmutableMap.of("collections", collections));
    }
}

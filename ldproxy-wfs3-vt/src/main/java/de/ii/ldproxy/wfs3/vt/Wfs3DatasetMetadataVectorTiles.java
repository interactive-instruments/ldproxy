/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import de.ii.ldproxy.ogcapi.domain.ImmutableDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.domain.Wfs3DatasetMetadataExtension;
import de.ii.ldproxy.ogcapi.domain.Wfs3Link;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.List;

/**
 * add tiling information to the dataset metadata
 */
@Component
@Provides
@Instantiate
public class Wfs3DatasetMetadataVectorTiles implements Wfs3DatasetMetadataExtension {

    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData dataset) {
        return isExtensionEnabled(dataset, TilesConfiguration.class);
    }

    @Override
    public ImmutableDataset.Builder process(ImmutableDataset.Builder datasetBuilder, OgcApiDatasetData datasetData,
                                            URICustomizer uriCustomizer, OgcApiMediaType mediaType,
                                            List<OgcApiMediaType> alternativeMediaTypes) {

        if (checkTilesEnabled(datasetData)) {
            final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
            List<Wfs3Link> wfs3Links = vectorTilesLinkGenerator.generateDatasetLinks(uriCustomizer);
            datasetBuilder.addAllLinks(wfs3Links);
        }
        return datasetBuilder;
    }

    private boolean checkTilesEnabled(OgcApiDatasetData datasetData) {
        return datasetData.getFeatureTypes()
                          .values()
                          .stream()
                          .anyMatch(featureTypeConfigurationOgcApi -> isExtensionEnabled(datasetData, featureTypeConfigurationOgcApi, TilesConfiguration.class));
    }
}

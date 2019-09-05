/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
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
public class OgcApiLandingPageVectorTiles implements OgcApiLandingPageExtension {

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    @Override
    public ImmutableDataset.Builder process(ImmutableDataset.Builder datasetBuilder, OgcApiDatasetData apiData,
                                            URICustomizer uriCustomizer, OgcApiMediaType mediaType,
                                            List<OgcApiMediaType> alternateMediaTypes) {

        if (checkTilesEnabled(apiData)) {
            final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
            List<OgcApiLink> ogcApiLinks = vectorTilesLinkGenerator.generateDatasetLinks(uriCustomizer);
            datasetBuilder.addAllLinks(ogcApiLinks);
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

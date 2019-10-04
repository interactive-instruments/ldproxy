/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;


import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.List;


/**
 * add tiling information to the collection metadata (supported tiling schemes, links)
 *
 * @author portele
 */
@Component
@Provides
@Instantiate
public class OgcApiCollectionVectorTiles implements OgcApiCollectionExtension {

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                                     FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                     OgcApiDatasetData apiData,
                                                     URICustomizer uriCustomizer, boolean isNested,
                                                     OgcApiMediaType mediaType,
                                                     List<OgcApiMediaType> alternateMediaTypes) {
        // The hrefs are URI templates and not URIs, so the templates should not be percent encoded!
        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        if (!isNested && isExtensionEnabled(apiData, featureTypeConfiguration, TilesConfiguration.class)) {
            collection.addAllLinks(vectorTilesLinkGenerator.generateTilesLinks(uriCustomizer));
        }

        return collection;
    }

}

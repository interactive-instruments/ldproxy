/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;


import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiCollection;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.domain.OgcApiCollectionExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                                                     URICustomizer uriCustomizer, boolean isNested,
                                                     OgcApiDatasetData apiData) {
        // The hrefs are URI templates and not URIs, so the templates should not be percent encoded!
        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        if (!isNested && isExtensionEnabled(apiData, featureTypeConfiguration, TilesConfiguration.class)) {
            List<Map<String, Object>> wfs3LinksList = new ArrayList<>();
            TilesConfiguration tiles = getExtensionConfiguration(apiData, featureTypeConfiguration, TilesConfiguration.class).get();
            Set<String> tilingSchemeIds = tiles.getZoomLevels()
                                               .keySet();
            for (String tilingSchemeId : tilingSchemeIds) {
                Map<String, Object> tilingSchemeInCollection = new HashMap<>();
                tilingSchemeInCollection.put("identifier", tilingSchemeId);
                tilingSchemeInCollection.put("links", vectorTilesLinkGenerator.generateTilesLinks(uriCustomizer, tilingSchemeId));
                wfs3LinksList.add(tilingSchemeInCollection);
            }
            collection.putExtensions("tilingSchemes", wfs3LinksList);
        }

        return collection;
    }

}

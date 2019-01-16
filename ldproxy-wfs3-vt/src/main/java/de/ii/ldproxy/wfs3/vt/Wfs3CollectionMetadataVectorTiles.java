/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;


import de.ii.ldproxy.wfs3.api.*;
import de.ii.ldproxy.wfs3.core.Wfs3CollectionMetadataExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.*;

import static de.ii.ldproxy.wfs3.vt.TilesConfiguration.EXTENSION_KEY;

/**
 * add tiling information to the collection metadata (supported tiling schemes, links)
 *
 * @author portele
 */
@Component
@Provides
@Instantiate
public class Wfs3CollectionMetadataVectorTiles implements Wfs3CollectionMetadataExtension {

    @Override
    public ImmutableWfs3Collection.Builder process(ImmutableWfs3Collection.Builder collection, FeatureTypeConfigurationWfs3 featureTypeConfigurationWfs3, URICustomizer uriCustomizer, boolean isNested,Wfs3ServiceData serviceData) {
        // The hrefs are URI templates and not URIs, so the templates should not be percent encoded!
        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        if (!isNested && isExtensionEnabled(serviceData,featureTypeConfigurationWfs3, EXTENSION_KEY)) {
            List<Map<String, Object>> wfs3LinksList = new ArrayList<>();
            TilesConfiguration tiles = (TilesConfiguration) getExtensionConfiguration(serviceData,featureTypeConfigurationWfs3, EXTENSION_KEY).get();
            Set<String> tilingSchemeIds = tiles.getZoomLevels().keySet();
            for(String tilingSchemeId : tilingSchemeIds) {
                Map<String, Object> tilingSchemeInCollection = new HashMap<>();
                tilingSchemeInCollection.put("identifier", tilingSchemeId);
                tilingSchemeInCollection.put("links", vectorTilesLinkGenerator.generateTilesLinks(uriCustomizer, tilingSchemeId));
                wfs3LinksList.add(tilingSchemeInCollection);
            }
            collection.putExtensions("tilingSchemes",wfs3LinksList);
        }

        return collection;
    }

}

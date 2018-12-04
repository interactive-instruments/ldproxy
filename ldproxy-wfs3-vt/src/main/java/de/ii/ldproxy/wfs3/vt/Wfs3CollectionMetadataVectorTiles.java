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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public ImmutableWfs3Collection.Builder process(ImmutableWfs3Collection.Builder collection, FeatureTypeConfigurationWfs3 featureTypeConfigurationWfs3, URICustomizer uriCustomizer, boolean isNested) {
        // The hrefs are URI templates and not URIs, so the templates should not be percent encoded!
        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        if (!isNested) {

            List<Map<String, Object>> wfs3LinksList = new ArrayList<>();
            Map<String, Object> wfs3LinksMap = new HashMap<>();
            //  for(Object tilingSchemeId : TODO tilingSchemeIDs) {

            wfs3LinksMap.put("identifier", "default"); //TODO replace with tilingSchemeId
            wfs3LinksMap.put("links", vectorTilesLinkGenerator.generateTilesLinks(uriCustomizer, "default")); //TODO replace with tilingSchemeId
            wfs3LinksList.add(wfs3LinksMap);

            //    }
            collection.putExtensions("tilingSchemes",wfs3LinksList);
        }

        return collection;
    }

}

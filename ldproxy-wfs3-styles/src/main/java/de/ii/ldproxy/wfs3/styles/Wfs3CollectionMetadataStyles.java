/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;


import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableWfs3Collection;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.domain.Wfs3CollectionMetadataExtension;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * add styles information to the collection metadata
 *
 */
@Component
@Provides
@Instantiate
public class Wfs3CollectionMetadataStyles implements Wfs3CollectionMetadataExtension {

    @Requires
    private StylesStore stylesStore;


    @Override
    public ImmutableWfs3Collection.Builder process(ImmutableWfs3Collection.Builder collection,
                                                   FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                   URICustomizer uriCustomizer, boolean isNested,
                                                   OgcApiDatasetData datasetData) {

        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

        String collectionId = featureTypeConfiguration.getId();

        List<String> styles = stylesStore.ids(datasetData.getId(), collectionId);
        List<Map<String, Object>> wfs3LinksList = new ArrayList<>();


        for (String key : styles) {
            Map<String, Object> wfs3StylesInCollections = new HashMap<>();
            String styleId = key.split("\\.")[0];
            wfs3StylesInCollections.put("id", styleId);
            wfs3StylesInCollections.put("links", stylesLinkGenerator.generateStylesLinksCollectionMetadata(uriCustomizer, collectionId, styleId));
            wfs3LinksList.add(wfs3StylesInCollections);
        }


        collection.putExtensions("styles", wfs3LinksList);


        return collection;
    }

}

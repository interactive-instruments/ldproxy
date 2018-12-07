/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;


import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.api.*;
import de.ii.ldproxy.wfs3.core.Wfs3CollectionMetadataExtension;
import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.configstore.file.FileConfigStore;
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
    private KeyValueStore keyValueStore; //maybe


    @Override
    public ImmutableWfs3Collection.Builder process(ImmutableWfs3Collection.Builder collection, FeatureTypeConfigurationWfs3 featureTypeConfigurationWfs3, URICustomizer uriCustomizer, boolean isNested) {

        final StylesLinkGenerator stylesLinkGenerator= new StylesLinkGenerator();



        List<Map<String, Object>> wfs3LinksList = new ArrayList<>();
        Map<String, Object> wfs3StylesInCollections = new HashMap<>();

        //TODO get the data - only example
        wfs3StylesInCollections.put("identifier","default");
        wfs3StylesInCollections.put("description","... a description of the style...");
        wfs3StylesInCollections.put("accept", ImmutableList.of("???","???"));
        wfs3StylesInCollections.put("supports", ImmutableList.of("application/geo+json","application/vnd.mapbox-vector-tile"));
        wfs3StylesInCollections.put("links", stylesLinkGenerator.generateStylesLinksCollection(uriCustomizer,"daraa"));
        wfs3LinksList.add(wfs3StylesInCollections);
        collection.putExtensions("styles",wfs3LinksList);

        return collection;
    }

}

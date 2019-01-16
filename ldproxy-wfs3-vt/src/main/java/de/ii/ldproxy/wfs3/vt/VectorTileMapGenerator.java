/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;

import java.util.*;

import static de.ii.ldproxy.wfs3.vt.TilesConfiguration.EXTENSION_KEY;

/**
 * class, which creates Maps for easier access from the service Data
 *
 */
public class VectorTileMapGenerator {

/**
 * checks if the tiles extension is available and returns a Set with all collectionIds
 * @param serviceData       the service data of the Wfs3 Service
 * @return a set with all CollectionIds, which have the tiles Extension
 */
public static Set<String> getAllCollectionIdsWithTileExtension(Wfs3ServiceData serviceData){
    Set<String> collectionIds = new HashSet<String>();
    for(String collectionId: serviceData.getFeatureTypes().keySet())
        if (serviceData.getFeatureTypes().get(collectionId).getExtensions().containsKey(EXTENSION_KEY)) {
            collectionIds.add(collectionId);
        }
    return collectionIds;
}
    /**
     * checks if the tiles extension is available and returns a Map with all available collections and a boolean value if the tiles
     * support is currently enabled
     * @param serviceData       the service data of the Wfs3 Service
     * @return a map with all CollectionIds, which have the tiles Extension and the value of the tiles Parameter  "enabled"
     */
    public static Map<String,Boolean> getEnabledMap(Wfs3ServiceData serviceData){
        Map<String,Boolean> enabledMap = new HashMap<>();

        for(String collectionId: serviceData.getFeatureTypes().keySet()) {
            if (serviceData.getFeatureTypes().get(collectionId).getExtensions().containsKey(EXTENSION_KEY)) {
                final TilesConfiguration tilesConfiguration = (TilesConfiguration) serviceData
                        .getFeatureTypes()
                        .get(collectionId)
                        .getExtensions()
                        .get(EXTENSION_KEY);
                ImmutableMap<Integer, Boolean> tilesEnabled = tilesConfiguration.getTiles()
                        .stream()
                        .collect(ImmutableMap.toImmutableMap(TilesConfiguration.Tiles::getId, TilesConfiguration.Tiles::getEnabled));

                enabledMap.put(collectionId,tilesEnabled.values().asList().get(0));
            }
        }
        return enabledMap;
    }

    /**
     * checks if the tiles extension is available and returns a Map with all available collections and the supported formats
     * @param serviceData       the service data of the Wfs3 Service
     * @return a map with all CollectionIds, which have the tiles Extension and the supported formats
     */
    public static Map<String, List<String>> getFormatsMap(Wfs3ServiceData serviceData){

        Map<String,List<String>> formatsMap = new HashMap<>();

        for(String collectionId : serviceData.getFeatureTypes().keySet()) {

            if (serviceData.getFeatureTypes().get(collectionId).getExtensions().containsKey(EXTENSION_KEY)) {

                final TilesConfiguration tilesConfiguration = (TilesConfiguration) serviceData.getFeatureTypes()
                        .get(collectionId)
                        .getExtensions()
                        .get(EXTENSION_KEY);

                try {
                    ImmutableMap<Integer, List<String>> formatsList = tilesConfiguration.getTiles()
                            .stream()
                            .collect(ImmutableMap.toImmutableMap(TilesConfiguration.Tiles::getId, TilesConfiguration.Tiles::getFormats));
                    formatsMap.put(collectionId,formatsList.values().asList().get(0));
                }catch(NullPointerException ignored){
                    formatsMap.put(collectionId, ImmutableList.of("application/json","application/vnd.mapbox-vector-tile"));
                }
            }
        }
        return formatsMap;

    }

    /**
     * checks if the tiles extension is available and returns a Map with entrys for each collection and their zoomLevel or seeding
     * @param serviceData       the service data of the Wfs3 Service
     * @param seeding           if seeding true, we observe seeding MinMax, if false zoomLevel MinMax
     * @return a map with all CollectionIds, which have the tiles Extension and the zoomLevel or seeding
     */
    public static Map<String, Map<String, TilesConfiguration.Tiles.MinMax>> getMinMaxMap(Wfs3ServiceData serviceData,Boolean seeding){
        Map<String, Map<String, TilesConfiguration.Tiles.MinMax>> minMaxMap = new HashMap<>();

        for(String collectionId: serviceData.getFeatureTypes().keySet()) {
            if (serviceData.getFeatureTypes().get(collectionId).getExtensions().containsKey(EXTENSION_KEY)) {
                final TilesConfiguration tilesConfiguration = (TilesConfiguration) serviceData
                        .getFeatureTypes()
                        .get(collectionId)
                        .getExtensions()
                        .get(EXTENSION_KEY);
                ImmutableMap<Integer, Map<String, TilesConfiguration.Tiles.MinMax>> minMaxList =null;
                if(!seeding){
                    try{
                        minMaxList = tilesConfiguration.getTiles()
                                .stream()
                                .collect(ImmutableMap.toImmutableMap(TilesConfiguration.Tiles::getId, TilesConfiguration.Tiles::getZoomLevels));
                        minMaxMap.put(collectionId,minMaxList.values().asList().get(0));

                    }catch (NullPointerException ignored){
                        minMaxMap.put(collectionId,null);
                    }

                }
                if(seeding){
                    try{
                        minMaxList = tilesConfiguration.getTiles()
                                .stream()
                                .collect(ImmutableMap.toImmutableMap(TilesConfiguration.Tiles::getId, TilesConfiguration.Tiles::getSeeding));
                        minMaxMap.put(collectionId,minMaxList.values().asList().get(0));
                    }catch (NullPointerException ignored){

                        minMaxMap.put(collectionId,null);
                    }

                }

            }
        }
        return minMaxMap;
    }

}

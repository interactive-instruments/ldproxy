/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.api.ExtensionConfiguration;
import de.ii.ldproxy.wfs3.api.Wfs3Extension;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;

import java.util.*;

import static de.ii.ldproxy.wfs3.vt.TilesConfiguration.EXTENSION_KEY;

/**
 * class, which creates Maps for easier access from the service Data
 *
 */
public class VectorTileMapGenerator implements Wfs3Extension {

    /**
     * checks if the tiles extension is available and returns a Set with all collectionIds
     * @param serviceData       the service data of the Wfs3 Service
     * @return a set with all CollectionIds, which have the tiles Extension
     */
    public Set<String> getAllCollectionIdsWithTileExtension(Wfs3ServiceData serviceData){
        Set<String> collectionIds = new HashSet<String>();
        for(String collectionId: serviceData.getFeatureTypes().keySet())
            if (isExtensionEnabled(serviceData, serviceData.getFeatureTypes().get(collectionId),EXTENSION_KEY)) {
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
    public Map<String,Boolean> getEnabledMap(Wfs3ServiceData serviceData){
        Map<String,Boolean> enabledMap = new HashMap<>();
        for(String collectionId: serviceData.getFeatureTypes().keySet()) {
            if (isExtensionEnabled(serviceData,serviceData.getFeatureTypes().get(collectionId),EXTENSION_KEY)) {
                final TilesConfiguration tilesConfiguration = (TilesConfiguration) getExtensionConfiguration(serviceData,serviceData.getFeatureTypes().get(collectionId),EXTENSION_KEY).get();


                boolean tilesEnabled =tilesConfiguration.getEnabled();

                enabledMap.put(collectionId,tilesEnabled);
            }
        }
        return enabledMap;
    }

    /**
     * checks if the tiles extension is available and returns a Map with all available collections and the supported formats
     * @param serviceData       the service data of the Wfs3 Service
     * @return a map with all CollectionIds, which have the tiles Extension and the supported formats
     */
    public Map<String, List<String>> getFormatsMap(Wfs3ServiceData serviceData){

        Map<String,List<String>> formatsMap = new HashMap<>();

        for(String collectionId : serviceData.getFeatureTypes().keySet()) {

            if (isExtensionEnabled(serviceData,serviceData.getFeatureTypes().get(collectionId),EXTENSION_KEY)) {

                final TilesConfiguration tilesConfiguration = (TilesConfiguration) getExtensionConfiguration(serviceData,serviceData.getFeatureTypes().get(collectionId), EXTENSION_KEY).get();

                List<String> formatsList=tilesConfiguration.getFormats();
                if(formatsList==null){
                    formatsList=(ImmutableList.of("application/json","application/vnd.mapbox-vector-tile"));
                }
                formatsMap.put(collectionId,formatsList);

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
    public Map<String, Map<String, TilesConfiguration.   MinMax>> getMinMaxMap(Wfs3ServiceData serviceData,Boolean seeding){
        Map<String, Map<String, TilesConfiguration.MinMax>> minMaxMap = new HashMap<>();

        for(String collectionId: serviceData.getFeatureTypes().keySet()) {
            if (isExtensionEnabled(serviceData,serviceData.getFeatureTypes().get(collectionId),EXTENSION_KEY)) {
                final TilesConfiguration tilesConfiguration = (TilesConfiguration) getExtensionConfiguration(serviceData,serviceData.getFeatureTypes().get(collectionId),EXTENSION_KEY).get();
                Map<String,TilesConfiguration.MinMax> minMax =null;
                if(!seeding){
                    try{
                        minMax = tilesConfiguration.getZoomLevels();
                        minMaxMap.put(collectionId,minMax);

                    }catch (NullPointerException ignored){
                        minMaxMap.put(collectionId,null);
                    }

                }
                if(seeding){
                    try{
                        minMax=tilesConfiguration.getSeeding();
                        minMaxMap.put(collectionId,minMax);
                    }catch (NullPointerException ignored){

                        minMaxMap.put(collectionId,null);
                    }

                }

            }
        }
        return minMaxMap;
    }

}

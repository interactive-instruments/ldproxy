/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * class, which creates Maps for easier access from the service Data
 *
 */
//TODO: this is not an extension, misused to have access to isExtensionEnabled and getExtensionConfiguration
public class VectorTileMapGenerator implements OgcApiExtension {

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    /**
     * checks if the tiles extension is available and returns a Set with all collectionIds
     * @param datasetData       the service data of the Wfs3 Service
     * @return a set with all CollectionIds, which have the tiles Extension
     */
    public Set<String> getAllCollectionIdsWithTileExtension(OgcApiApiDataV2 datasetData) {
        Set<String> collectionIds = new HashSet<String>();
        for (String collectionId : datasetData.getCollections()
                                              .keySet())
            if (isExtensionEnabled(datasetData, datasetData.getCollections()
                                                           .get(collectionId), TilesConfiguration.class)) {
                collectionIds.add(collectionId);
            }
        return collectionIds;
    }

    /**
     * checks if the tiles extension is available and returns a Map with all available collections and a boolean value if the tiles
     * support is currently enabled
     * @param datasetData       the service data of the Wfs3 Service
     * @return a map with all CollectionIds, which have the tiles Extension and the value of the tiles Parameter  "enabled"
     */
    public Map<String, Boolean> getEnabledMap(OgcApiApiDataV2 datasetData) {
        Map<String, Boolean> enabledMap = new HashMap<>();
        for (String collectionId : datasetData.getCollections()
                                              .keySet()) {
            if (isExtensionEnabled(datasetData, datasetData.getCollections()
                                                           .get(collectionId), TilesConfiguration.class)) {
                final TilesConfiguration tilesConfiguration = (TilesConfiguration) getExtensionConfiguration(datasetData, datasetData.getCollections()
                                                                                                                                     .get(collectionId), TilesConfiguration.class).get();


                boolean tilesEnabled = tilesConfiguration.getEnabled();

                enabledMap.put(collectionId, tilesEnabled);
            }
        }
        return enabledMap;
    }

    /**
     * checks if the tiles extension is available and returns a Map with all available collections and the supported formats
     * @param datasetData       the service data of the Wfs3 Service
     * @return a map with all CollectionIds, which have the tiles Extension and the supported formats
     */
    public Map<String, List<String>> getFormatsMap(OgcApiApiDataV2 datasetData) {

        Map<String, List<String>> formatsMap = new HashMap<>();

        for (String collectionId : datasetData.getCollections()
                                              .keySet()) {

            if (isExtensionEnabled(datasetData, datasetData.getCollections()
                                                           .get(collectionId), TilesConfiguration.class)) {

                final TilesConfiguration tilesConfiguration = (TilesConfiguration) getExtensionConfiguration(datasetData, datasetData.getCollections()
                                                                                                                                     .get(collectionId), TilesConfiguration.class).get();

                List<String> formatsList = tilesConfiguration.getFormats();
                if (formatsList == null) {
                    formatsList = (ImmutableList.of("application/geo+json", "application/vnd.mapbox-vector-tile"));
                }
                formatsMap.put(collectionId, formatsList);

            }
        }
        return formatsMap;

    }

    /**
     * checks if the tiles extension is available and returns a Map with entrys for each collection and their zoomLevel or seeding
     * @param datasetData       the service data of the Wfs3 Service
     * @param seeding           if seeding true, we observe seeding MinMax, if false zoomLevel MinMax
     * @return a map with all CollectionIds, which have the tiles Extension and the zoomLevel or seeding
     */
    public Map<String, Map<String, MinMax>> getMinMaxMap(OgcApiApiDataV2 datasetData,
                                                         Boolean seeding) {
        Map<String, Map<String, MinMax>> minMaxMap = new HashMap<>();

        for (String collectionId : datasetData.getCollections()
                                              .keySet()) {
            if (isExtensionEnabled(datasetData, datasetData.getCollections()
                                                           .get(collectionId), TilesConfiguration.class)) {
                final TilesConfiguration tilesConfiguration = (TilesConfiguration) getExtensionConfiguration(datasetData, datasetData.getCollections()
                                                                                                                                     .get(collectionId), TilesConfiguration.class).get();
                Map<String, MinMax> minMax = null;
                if (!seeding) {
                    try {
                        minMax = tilesConfiguration.getZoomLevels();
                        minMaxMap.put(collectionId, minMax);

                    } catch (NullPointerException ignored) {
                        minMaxMap.put(collectionId, null);
                    }

                }
                if (seeding) {
                    try {
                        minMax = tilesConfiguration.getSeeding();
                        minMaxMap.put(collectionId, minMax);
                    } catch (NullPointerException ignored) {

                        minMaxMap.put(collectionId, null);
                    }

                }

            }
        }
        return minMaxMap;
    }

}

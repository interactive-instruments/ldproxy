/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.ii.ldproxy.wfs3.vt;

import de.ii.ldproxy.target.geojson.Wfs3OutputFormatGeoJson;
import de.ii.ldproxy.wfs3.api.FeatureTypeTiles;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3MediaType;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.api.Wfs3StartupTask;
import de.ii.xsf.core.server.CoreServerConfig;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.feature.transformer.api.TransformingFeatureProvider;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS;
import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

@Component
@Provides
@Instantiate
public class VectorTileSeeding implements Wfs3StartupTask {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointTiles.class);
    private final VectorTilesCache cache;
    private Thread t = null;
    private Map<Thread, String> threadMap = new HashMap<>();


    @Requires
    private CrsTransformation crsTransformation;

    @Requires
    private CoreServerConfig coreServerConfig;

    @Requires
    private Wfs3OutputFormatGeoJson wfs3OutputFormatGeoJson;

    public VectorTileSeeding(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) {
        String dataDirectory = bundleContext.getProperty(DATA_DIR_KEY);
        cache = new VectorTilesCache(dataDirectory);
    }


    @Override
    public Runnable getTask(Wfs3ServiceData wfs3ServiceData, TransformingFeatureProvider featureProvider) {


        Runnable startSeeding = () -> {

            Set<String> collectionIdsDataset = Wfs3EndpointTiles.getCollectionIdsDataset(wfs3ServiceData, false, false, true);
            try {
                boolean tilesDatasetEnabled = false;
                boolean seedingDatasetEnabled = false;

                if (!collectionIdsDataset.isEmpty())
                    tilesDatasetEnabled = true;


                if (tilesDatasetEnabled) {
                    for (String collectionId : collectionIdsDataset) {
                        if (wfs3ServiceData.getFeatureTypes()
                                           .get(collectionId)
                                           .getTiles()
                                           .getSeeding() != null) {
                            seedingDatasetEnabled = true;
                            break;
                        }
                    }
                }


                if (tilesDatasetEnabled && seedingDatasetEnabled) {
                    seedingDataset(collectionIdsDataset, wfs3ServiceData, crsTransformation, cache, featureProvider, coreServerConfig);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


        };
        t = new Thread(startSeeding);
        t.setDaemon(true);
        t.start();
        threadMap.put(t, wfs3ServiceData.getId());
        return startSeeding;

    }

    public Map<Thread, String> getThreadMap() {
        return threadMap;
    }

    public void removeThreadMapEntry(Thread t) {
        threadMap.remove(t);
    }


    private File generateSeedingMVT(Wfs3ServiceData serviceData, String collectionId, String tilingSchemeId, int z, int x, int y, VectorTilesCache cache, CrsTransformation crsTransformation, TransformingFeatureProvider featureProvider, CoreServerConfig coreServerConfig) {

        try {
            LOGGER.debug("seeding - ZoomLevel: " + Integer.toString(z) + " row: " + Integer.toString(x) + " col: " + Integer.toString(y));
            VectorTile tile = new VectorTile(collectionId, tilingSchemeId, Integer.toString(z), Integer.toString(x), Integer.toString(y), serviceData, false, cache, featureProvider, wfs3OutputFormatGeoJson);
            File tileFileMvt = tile.getFile(cache, "pbf");
            if (!tileFileMvt.exists()) {
                File tileFileJson = generateSeedingJSON(serviceData, collectionId, tilingSchemeId, z, x, y, cache, crsTransformation, featureProvider, coreServerConfig);
                Map<String, File> layers = new HashMap<>();
                layers.put(collectionId, tileFileJson);
                boolean success = tile.generateTileMvt(tileFileMvt, layers, null, crsTransformation);
                if (!success) {
                    String msg = "Internal server error: could not generate protocol buffers for a tile.";
                    LOGGER.error(msg);
                    throw new InternalServerErrorException(msg);
                }
                return tileFileJson;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private File generateSeedingJSON(Wfs3ServiceData serviceData, String collectionId, String tilingSchemeId, int z, int x, int y, VectorTilesCache cache, CrsTransformation crsTransformation, TransformingFeatureProvider featureProvider, CoreServerConfig coreServerConfig) {
        try {
            VectorTile tile = new VectorTile(collectionId, tilingSchemeId, Integer.toString(z), Integer.toString(x), Integer.toString(y), serviceData, false, cache, featureProvider, wfs3OutputFormatGeoJson);
            File tileFileJson = tile.getFile(cache, "json");

            if (!tileFileJson.exists()) {
                String prefix = coreServerConfig.getExternalUrl();
                String uriString = prefix + "/" + serviceData.getId() + "/" + "collections" + "/"
                        + collectionId + "/tiles/" + tilingSchemeId + "/" + z + "/" + y + "/" + x;

                URI uri = null;
                try {
                    uri = new URI(uriString);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                URICustomizer uriCustomizer = new URICustomizer(uri);

                Wfs3MediaType mediaType;
                mediaType = ImmutableWfs3MediaType.builder()
                                                  .main(new MediaType("application", "json"))
                                                  .label("JSON")
                                                  .build();
                tile.generateTileJson(tileFileJson, crsTransformation, null, null, null, uriCustomizer, mediaType, true);
            }
            return tileFileJson;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, Integer> computeMinMax(int zoomLevel, TilingScheme tilingScheme, CrsTransformation crsTransformation, double xMin, double xMax, double yMin, double yMax) throws CrsTransformationException {
        int row = 0;
        int col = 0;
        Map<String, Integer> minMax = new HashMap<>();
        double getXMax = crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel, col, row)
                                                                      .getEpsgCrs(), DEFAULT_CRS)
                                          .transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel, col, row))
                                          .getXmin();
        double getXMin = crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel, col, row)
                                                                      .getEpsgCrs(), DEFAULT_CRS)
                                          .transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel, col, row))
                                          .getXmax();
        double getYMin = crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel, col, row)
                                                                      .getEpsgCrs(), DEFAULT_CRS)
                                          .transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel, col, row))
                                          .getYmin();
        double getYMax = crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel, col, row)
                                                                      .getEpsgCrs(), DEFAULT_CRS)
                                          .transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel, col, row))
                                          .getYmax();

        while (getXMin < xMin) {
            col++;
            getXMin = crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel, col, row)
                                                                   .getEpsgCrs(), DEFAULT_CRS)
                                       .transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel, col, row))
                                       .getXmin();
        }
        minMax.put("colMin", col);

        getXMax = getXMin;
        while (getXMax < xMax) {
            col++;
            getXMax = crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel, col, row)
                                                                   .getEpsgCrs(), DEFAULT_CRS)
                                       .transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel, col, row))
                                       .getXmax();
        }

        minMax.put("colMax", col);

        while (getYMax > yMax) {
            row++;
            getYMax = crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel, col, row)
                                                                   .getEpsgCrs(), DEFAULT_CRS)
                                       .transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel, col, row))
                                       .getYmax();
        }
        minMax.put("rowMin", row);

        getYMin = getYMax;
        while (getYMin > yMin) {
            row++;
            getYMin = crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel, col, row)
                                                                   .getEpsgCrs(), DEFAULT_CRS)
                                       .transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel, col, row))
                                       .getYmin();
        }
        minMax.put("rowMax", row);

        return minMax;
    }


    private void seedingDataset(Set<String> collectionIdsDataset, Wfs3ServiceData wfs3ServiceData, CrsTransformation crsTransformation, VectorTilesCache cache, TransformingFeatureProvider featureProvider, CoreServerConfig coreServerConfig) throws FileNotFoundException {

        /*Computation of the minimum and maximum values for x and y from the minimum/maximum spatial extent
         * TODO: Maybe a spatial extent for the whole dataset in the config?*/
        List<Double> xMinList = new ArrayList<>();
        List<Double> xMaxList = new ArrayList<>();
        List<Double> yMinList = new ArrayList<>();
        List<Double> yMaxList = new ArrayList<>();
        List<Integer> minZoomList = new ArrayList<>();
        List<Integer> maxZoomList = new ArrayList<>();
        Set<String> tilingSchemeIdsCollection = null;
        for (String collectionId : collectionIdsDataset) {
            tilingSchemeIdsCollection = wfs3ServiceData.getFeatureTypes()
                                                       .get(collectionId)
                                                       .getTiles()
                                                       .getSeeding()
                                                       .keySet();
            for (String tilingSchemeId : tilingSchemeIdsCollection) {
                try {
                    Map<String, FeatureTypeTiles.MinMax> seeding = wfs3ServiceData.getFeatureTypes()
                                                                                  .get(collectionId)
                                                                                  .getTiles()
                                                                                  .getSeeding();
                    BoundingBox spatial = wfs3ServiceData.getFeatureTypes()
                                                         .get(collectionId)
                                                         .getExtent()
                                                         .getSpatial();
                    if (spatial == null) {
                    }
                    if (seeding.size() != 0 && spatial != null) {
                        int maxZoom = seeding.get(tilingSchemeId)
                                             .getMax();
                        int minZoom = seeding.get(tilingSchemeId)
                                             .getMin();
                        double xMin = spatial.getXmin();
                        double xMax = spatial.getXmax();
                        double yMin = spatial.getYmin();
                        double yMax = spatial.getYmax();
                        maxZoomList.add(maxZoom);
                        minZoomList.add(minZoom);
                        if (xMin != -180)
                            xMinList.add(xMin);
                        if (xMax != 180)
                            xMaxList.add(xMax);
                        if (yMin != -90)
                            yMinList.add(yMin);
                        if (yMax != 90)
                            yMaxList.add(yMax);
                    }
                } catch (Exception e) {
                }
            }
        }
        int minZoomDataset = minZoomList.stream()
                                        .min(Comparator.comparing(Integer::intValue))
                                        .orElseThrow(NoSuchElementException::new);
        int maxZoomDataset = maxZoomList.stream()
                                        .max(Comparator.comparing(Integer::intValue))
                                        .orElseThrow(NoSuchElementException::new);
        double xMinDataset = xMinList.stream()
                                     .min(Comparator.comparing(Double::doubleValue))
                                     .orElseThrow(NoSuchElementException::new);
        double xMaxDataset = xMaxList.stream()
                                     .max(Comparator.comparing(Double::doubleValue))
                                     .orElseThrow(NoSuchElementException::new);
        double yMinDataset = yMinList.stream()
                                     .min(Comparator.comparing(Double::doubleValue))
                                     .orElseThrow(NoSuchElementException::new);
        double yMaxDataset = yMaxList.stream()
                                     .max(Comparator.comparing(Double::doubleValue))
                                     .orElseThrow(NoSuchElementException::new);
        /*Comupation end*/

        /*Begin seeding*/
        for (int z = minZoomDataset; z <= maxZoomDataset; z++) {

            Map<String, Integer> minMax = null;
            try {
                minMax = computeMinMax(z, new DefaultTilingScheme(), crsTransformation, xMinDataset, xMaxDataset, yMinDataset, yMaxDataset);
            } catch (CrsTransformationException e) {
                e.printStackTrace();
            }

            int rowMin = minMax.get("rowMin");
            int rowMax = minMax.get("rowMax");
            int colMin = minMax.get("colMin");
            int colMax = minMax.get("colMax");

            for (int x = rowMin; x <= rowMax; x++) {
                for (int y = colMin; y <= colMax; y++) {
                    for (String tilingSchemeId : tilingSchemeIdsCollection) {
                        VectorTile tile = new VectorTile(null, tilingSchemeId, Integer.toString(z), Integer.toString(x), Integer.toString(y), wfs3ServiceData, false, cache, featureProvider, wfs3OutputFormatGeoJson);

                        // generate tile
                        File tileFileMvt = tile.getFile(cache, "pbf");
                        if (!tileFileMvt.exists()) {

                            Map<String, File> layers = new HashMap<String, File>();
                            Set<String> collectionIdsMVTEnabled = Wfs3EndpointTiles.getCollectionIdsDataset(wfs3ServiceData, true, false, true);


                            for (String collectionId : collectionIdsMVTEnabled) {
                                // include only the requested layers / collections
                                File tileFileMvtCollection = generateSeedingMVT(wfs3ServiceData, collectionId, tilingSchemeId, z, x, y, cache, crsTransformation, featureProvider, coreServerConfig);
                                layers.put(collectionId, tileFileMvtCollection);
                            }
                            boolean success = tile.generateTileMvt(tileFileMvt, layers, null, crsTransformation);
                            if (!success) {
                                String msg = "Internal server error: could not generate protocol buffers for a tile.";
                                LOGGER.error(msg);
                                throw new InternalServerErrorException(msg);
                            }
                            Set<String> collectionIdsOnlyJSON = Wfs3EndpointTiles.getCollectionIdsDataset(wfs3ServiceData, false, true, false);

                            for (String collectionId : collectionIdsOnlyJSON) {
                                generateSeedingJSON(wfs3ServiceData, collectionId, tilingSchemeId, z, x, y, cache, crsTransformation, featureProvider, coreServerConfig);
                            }
                        }
                    }
                }
            }
        }
    }
}

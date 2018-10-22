/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package de.ii.ldproxy.wfs3.vt;

import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.*;
import de.ii.ldproxy.wfs3.vt.VectorTilesCache;
import de.ii.ldproxy.wfs3.vt.Wfs3EndpointTiles;
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
import javax.ws.rs.core.UriInfo;
import javax.xml.crypto.dsig.Transform;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS;
import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

@Component
@Provides
@Instantiate
public class VectorTileSeeding implements Wfs3StartupTask {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointTiles.class);
    private final VectorTilesCache cache;

    @Requires
    private CrsTransformation crsTransformation;

    public VectorTileSeeding(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext){
        String dataDirectory = bundleContext.getProperty(DATA_DIR_KEY);
        cache = new VectorTilesCache(dataDirectory);
    }
    @Override
    public Runnable getTask(Wfs3ServiceData wfs3ServiceData, TransformingFeatureProvider featureProvider) {


        Runnable startSeeding=()->{

            //service, bundleContext, crsTransformation
            Set<String> collectionIdsDataset = Wfs3EndpointTiles.getCollectionIdsDataset(wfs3ServiceData);

            for(String collectionId : collectionIdsDataset){
                Set <String> tilingSchemeIdsCollection= wfs3ServiceData.getFeatureTypes().get(collectionId).getTiles().getSeeding().keySet();
                for(String tilingSchemeId : tilingSchemeIdsCollection){
                    try {
                        LOGGER.debug("Seeding - Service: "+ wfs3ServiceData.getId() +" Collection: " +collectionId + " TilingScheme: " + tilingSchemeId);
                        seeding(wfs3ServiceData,collectionId,tilingSchemeId,cache, crsTransformation,featureProvider);
                    }catch (CrsTransformationException e){}
                }
            }
        };

        new Thread(startSeeding).start();

        return startSeeding;
    }

    public static void seeding(Wfs3ServiceData serviceData, String collectionId, String tilingSchemeId, VectorTilesCache cache, CrsTransformation crsTransformation,TransformingFeatureProvider featureProvider) throws CrsTransformationException {

        int maxZoom = 0;
        int minZoom = 0;

        double xMin = 0;
        double xMax = 0;
        double yMin = 0;
        double yMax = 0;
        int rowMin=0;
        int rowMax=0;
        int colMin=0;
        int colMax=0;

        Map<String, FeatureTypeTiles.MinMax> seeding = serviceData.getFeatureTypes().get(collectionId).getTiles().getSeeding();
        BoundingBox spatial = serviceData.getFeatureTypes().get(collectionId).getExtent().getSpatial();

        //only do seeding if there are values for it
        if (seeding.size() != 0 && spatial!= null) { //TODO test empty spatial (spatial!= null) ist wahrscheinlich nicht korrekt!!!
            maxZoom = seeding.get(tilingSchemeId).getMax();
            minZoom = seeding.get(tilingSchemeId).getMin();
            xMin =  spatial.getXmin();
            xMax = spatial.getXmax();
            yMin = spatial.getYmin();
            yMax = spatial.getYmax();

            for(int z =minZoom; z<=maxZoom;z++) {

                Map<String,Integer>minMax=computeMinMax(z,new DefaultTilingScheme(),crsTransformation,xMin,xMax,yMin,yMax);

                rowMin=minMax.get("rowMin");
                rowMax=minMax.get("rowMax");
                colMin=minMax.get("colMin");
                colMax=minMax.get("colMax");

                for (int x = rowMin; x <= rowMax; x++) {
                    for (int y = colMin; y <= colMax; y++) {
                        generateSeedingMVT(serviceData, collectionId,tilingSchemeId,z,x,y,cache,crsTransformation,featureProvider);
                    }
                }
            }
        }
    }

    private static void generateSeedingMVT(Wfs3ServiceData serviceData, String collectionId, String tilingSchemeId, int z, int x, int y, VectorTilesCache cache, CrsTransformation crsTransformation, TransformingFeatureProvider featureProvider){

        try {

            LOGGER.debug("seeding - ZoomLevel: " + Integer.toString(z) + " row: " + Integer.toString(x) +" col: " + Integer.toString(y));
            VectorTile tile = new VectorTile(collectionId, tilingSchemeId, Integer.toString(z), Integer.toString(x), Integer.toString(y), serviceData, false, cache,featureProvider);
            File tileFileMvt = tile.getFile(cache, "pbf");
            if (!tileFileMvt.exists()) {

                File tileFileJson=generateSeedingJSON(serviceData, collectionId,tilingSchemeId,z,x,y,cache,crsTransformation,featureProvider);

                Map<String, File> layers = new HashMap<>();
                layers.put(collectionId, tileFileJson);
                boolean success = tile.generateTileMvt(tileFileMvt, layers, null, crsTransformation);
                if (!success) {
                    String msg = "Internal server error: could not generate protocol buffers for a tile.";
                    LOGGER.error(msg);
                    throw new InternalServerErrorException(msg);
                }
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
    }

    private static File generateSeedingJSON(Wfs3ServiceData serviceData, String collectionId, String tilingSchemeId, int z, int x, int y, VectorTilesCache cache, CrsTransformation crsTransformation,TransformingFeatureProvider featureProvider){
        try{
            VectorTile tile = new VectorTile(collectionId, tilingSchemeId, Integer.toString(z), Integer.toString(x), Integer.toString(y), serviceData, false, cache,featureProvider);
            File tileFileJson = tile.getFile(cache, "json");
            if (!tileFileJson.exists()) {
                String prefix = "https://services.interactive-instruments.de/vtp/"; //TODO dynamic URI
                String uriString = prefix + "/" + serviceData.getId() + "/" + "collections" + "/"
                        + collectionId + "/tiles/" + tilingSchemeId + "/" + z +"/" + y + "/" + x;

                URI uri= null;
                try {
                    uri = new URI(uriString);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                URICustomizer uriCustomizer  = new URICustomizer(uri);


                Wfs3MediaType mediaType;
                mediaType = ImmutableWfs3MediaType.builder()
                        .main(new MediaType("application", "json"))
                        .label("JSON")
                        .build();

                tile.generateTileJson(tileFileJson, crsTransformation,null, null,null,uriCustomizer,mediaType,true);
            }
            return tileFileJson;
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
        return null;
    }
    public static Map<String, Integer> computeMinMax(int zoomLevel, TilingScheme tilingScheme, CrsTransformation crsTransformation, double xMin, double xMax, double yMin, double yMax) throws CrsTransformationException {
        int row=0;
        int col=0;
        Map<String, Integer> minMax=new HashMap<>();
        double getXMax=crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel,col,row).getEpsgCrs(),DEFAULT_CRS).transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel,col,row)).getXmin();
        double getXMin=crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel,col,row).getEpsgCrs(),DEFAULT_CRS).transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel,col,row)).getXmax();
        double getYMin=crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel,col,row).getEpsgCrs(),DEFAULT_CRS).transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel,col,row)).getYmin();
        double getYMax=crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel,col,row).getEpsgCrs(),DEFAULT_CRS).transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel,col,row)).getYmax();

        while(getXMin<xMin){
            col++;
            getXMin=crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel,col,row).getEpsgCrs(),DEFAULT_CRS).transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel,col,row)).getXmin();
        }
        minMax.put("colMin",col);

        getXMax=getXMin;
        while(getXMax<xMax){
            col++;
            getXMax=crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel,col,row).getEpsgCrs(),DEFAULT_CRS).transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel,col,row)).getXmax();
        }

        minMax.put("colMax",col);

        while(getYMax>yMax){
            row++;
            getYMax=crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel,col,row).getEpsgCrs(),DEFAULT_CRS).transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel,col,row)).getYmax();
        }
        minMax.put("rowMin",row);

        getYMin=getYMax;
        while(getYMin>yMin){
            row++;
            getYMin=crsTransformation.getTransformer(tilingScheme.getBoundingBox(zoomLevel,col,row).getEpsgCrs(),DEFAULT_CRS).transformBoundingBox(tilingScheme.getBoundingBox(zoomLevel,col,row)).getYmin();
        }
        minMax.put("rowMax",row);

        return minMax;
    }


}

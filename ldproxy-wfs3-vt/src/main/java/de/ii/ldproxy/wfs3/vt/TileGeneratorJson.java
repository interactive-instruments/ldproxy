package de.ii.ldproxy.wfs3.vt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.ldproxy.wfs3.ImmutableFeatureTransformationContextGeneric;
import de.ii.ldproxy.wfs3.ImmutableWfs3RequestContextImpl;
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.*;
import de.ii.ldproxy.wfs3.core.Wfs3EndpointCore;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.feature.query.api.FeatureStream;
import de.ii.xtraplatform.feature.query.api.ImmutableFeatureQuery;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.TransformingFeatureProvider;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletionException;

import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS;

/**
 * This class is responsible for generation and deletion of JSON Tiles.
 *
 */

public class TileGeneratorJson {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointTiles.class);


    /**
     * generate the GeoJSON tile file in the cache
     *
     * @param tileFile          the file object of the tile in the cache
     * @param crsTransformation the coordinate reference system transformation object to transform coordinates
     * @param uriInfo           context parameter to get the query parameters
     * @param filters           filters specified in the query
     * @param filterableFields  all possible fields you can use as a filter
     * @param uriCustomizer     the URI, split in host, path and query
     * @param mediaType         the media type, here json
     * @param isCollection      boolean collection or dataset Tile
     * @param tile              the tile which should be generated
     * @return true, if the file was generated successfully, false, if an error occurred
     */
    static boolean generateTileJson(File tileFile, CrsTransformation crsTransformation, @Context UriInfo uriInfo, Map<String, String> filters, Map<String, String> filterableFields, URICustomizer uriCustomizer, Wfs3MediaType mediaType, boolean isCollection, VectorTile tile) {
        // TODO add support for multi-collection GeoJSON output

        String collectionId = tile.getCollectionId();
        Wfs3ServiceData serviceData = tile.getServiceData();
        TilingScheme tilingScheme = tile.getTilingScheme();
        int level = tile.getLevel();
        int col = tile.getCol();
        int row = tile.getRow();
        TransformingFeatureProvider featureProvider = tile.getFeatureProvider();
        Wfs3OutputFormatExtension wfs3OutputFormatGeoJson = tile.getWfs3OutputFormatGeoJson();

        if (collectionId == null)
            return false;


        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(tileFile);
        } catch (FileNotFoundException e) {
            LOGGER.error("File not found: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        String geometryField = serviceData
                .getFilterableFieldsForFeatureType(collectionId)
                .get("bbox");

        String filter = null;
        try {
            filter = tile.getSpatialFilter(geometryField, crsTransformation);
        } catch (CrsTransformationException e) {
            LOGGER.error("CRS transformation error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        // calculate maxAllowableOffset
        double maxAllowableOffsetTilingScheme = tilingScheme.getMaxAllowableOffset(level, row, col);
        double maxAllowableOffsetNative = maxAllowableOffsetTilingScheme; // TODO convert to native CRS units
        double maxAllowableOffsetCrs84 = 0;
        try {
            maxAllowableOffsetCrs84 = tilingScheme.getMaxAllowableOffset(level, row, col, DEFAULT_CRS, crsTransformation);
        } catch (CrsTransformationException e) {
            LOGGER.error("CRS transformation error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }


        ImmutableFeatureQuery.Builder queryBuilder;
        if (uriInfo != null) {
            MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();

            List<String> propertiesList = Wfs3EndpointCore.getPropertiesList(queryParameters);

            queryBuilder = ImmutableFeatureQuery.builder()
                    .type(collectionId)
                    .filter(filter)
                    .maxAllowableOffset(maxAllowableOffsetNative)
                    .fields(propertiesList);


            if (filters != null && filterableFields != null) {
                if (!filters.isEmpty()) {
                    String cql = tile.getCQLFromFilters(filters, filterableFields);
                    LOGGER.debug("CQL {}", cql);
                    queryBuilder
                            .filter(cql + " AND " + filter)
                            .type(collectionId)
                            .maxAllowableOffset(maxAllowableOffsetNative)
                            .fields(propertiesList);
                }
            }
        } else {
            queryBuilder = ImmutableFeatureQuery.builder()
                    .type(collectionId)
                    .filter(filter)
                    .maxAllowableOffset(maxAllowableOffsetNative);
        }
        queryBuilder.build();


        List<Wfs3Link> wfs3Links = new ArrayList<>();


        if (isCollection) {
            Wfs3MediaType alternativeMediatype;
            alternativeMediatype = ImmutableWfs3MediaType.builder()
                    .main(new MediaType("application", "vnd.mapbox-vector-tile"))
                    .label("MVT")
                    .build();
            final VectorTilesLinkGenerator vectorTilesLinkGenerator= new VectorTilesLinkGenerator();
            wfs3Links = vectorTilesLinkGenerator.generateGeoJSONTileLinks(uriCustomizer, mediaType, alternativeMediatype, tilingScheme.getId(), Integer.toString(level), Integer.toString(row), Integer.toString(col), VectorTile.checkFormat(VectorTileMapGenerator.getFormatsMap(serviceData), collectionId, Wfs3MediaTypes.MVT, true), VectorTile.checkFormat(VectorTileMapGenerator.getFormatsMap(serviceData), collectionId, Wfs3MediaTypes.JSON, true));
        }


        FeatureStream<FeatureTransformer> featureTransformStream = featureProvider.getFeatureTransformStream(queryBuilder.build());

        try {
            FeatureTransformationContext transformationContext = ImmutableFeatureTransformationContextGeneric.builder()
                    .serviceData(serviceData)
                    .collectionName(collectionId)
                    .wfs3Request(ImmutableWfs3RequestContextImpl.builder()
                            .requestUri(uriCustomizer.build())
                            .mediaType(mediaType)
                            .build())
                    .crsTransformer(crsTransformation.getTransformer(serviceData.getFeatureProvider()
                            .getNativeCrs(), DEFAULT_CRS))
                    .links(wfs3Links)
                    .isFeatureCollection(true)
                    .limit(0) //TODO
                    .offset(0)
                    .serviceUrl("")
                    .maxAllowableOffset(maxAllowableOffsetCrs84)
                    .outputStream(outputStream)
                    .build();

            Optional<FeatureTransformer> featureTransformer = wfs3OutputFormatGeoJson.getFeatureTransformer(transformationContext);

            if (featureTransformer.isPresent()) {
                featureTransformStream.apply(featureTransformer.get())
                        .toCompletableFuture()
                        .join();
            } else {
                throw new IllegalStateException("Could not acquire FeatureTransformer");
            }

        } catch (CompletionException | URISyntaxException e) {
            if (e.getCause() instanceof WebApplicationException) {
                throw (WebApplicationException) e.getCause();
            }
            throw new IllegalStateException("Feature stream error", e.getCause());
        }

        return true;
    }

    /**
     * generates an empty JSON Tile with the required links
     *
     * @param tileFile                  the file object of the tile in the cache
     * @param tilingScheme              the tilingScheme the JSON Tile should have
     * @param serviceData               the Service data of the Wfs3Service
     * @param wfs3OutputFormatGeoJson   the wfs3OutputFormatGeoJSON
     * @param collectionId              the id of the collection in which the Tile should be generated
     * @param isCollection              boolean collection or dataset Tile
     * @param wfs3Request               the request
     * @param level                     the zoom level as an integer
     * @param row                       the row number as an integer
     * @param col                       the col number as an integer
     * @param crsTransformation         system transformation object to transform coordinates
     * @param service                   the service
     * @return true, if the file was generated successfully, false, if an error occurred
     */
    public static boolean generateEmptyJSON(File tileFile, TilingScheme tilingScheme, Wfs3ServiceData serviceData, Wfs3OutputFormatExtension wfs3OutputFormatGeoJson, String collectionId, boolean isCollection, Wfs3RequestContext wfs3Request, int level, int row, int col, CrsTransformation crsTransformation, Wfs3Service service) {

        if (collectionId == null)
            return false;


        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(tileFile);
        } catch (FileNotFoundException e) {
            LOGGER.error("File not found: " + e.getMessage());
            e.printStackTrace();
            return false;
        }


        double maxAllowableOffsetTilingScheme = tilingScheme.getMaxAllowableOffset(level, row, col);
        double maxAllowableOffsetNative = maxAllowableOffsetTilingScheme; // TODO convert to native CRS units
        double maxAllowableOffsetCrs84 = 0;
        try {
            maxAllowableOffsetCrs84 = tilingScheme.getMaxAllowableOffset(level, row, col, DEFAULT_CRS, crsTransformation);
        } catch (CrsTransformationException e) {
            LOGGER.error("CRS transformation error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        List<Wfs3Link> wfs3Links = new ArrayList<>();


        if (isCollection) {
            Wfs3MediaType alternativeMediatype;
            alternativeMediatype = ImmutableWfs3MediaType.builder()
                    .main(new MediaType("application", "vnd.mapbox-vector-tile"))
                    .label("MVT")
                    .build();
            final VectorTilesLinkGenerator vectorTilesLinkGenerator= new VectorTilesLinkGenerator();
            wfs3Links = vectorTilesLinkGenerator.generateGeoJSONTileLinks(wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), alternativeMediatype, tilingScheme.getId(), Integer.toString(level), Integer.toString(row), Integer.toString(col), VectorTile.checkFormat(VectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, Wfs3MediaTypes.MVT, true), VectorTile.checkFormat(VectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, Wfs3MediaTypes.JSON, true));
        }

        FeatureTransformationContext transformationContext = ImmutableFeatureTransformationContextGeneric.builder()
                .serviceData(serviceData)
                .collectionName(collectionId)
                .wfs3Request(wfs3Request)
                .links(wfs3Links)
                .isFeatureCollection(true)
                .maxAllowableOffset(maxAllowableOffsetCrs84)
                .outputStream(outputStream)
                .build();

        Optional<FeatureTransformer> featureTransformer = wfs3OutputFormatGeoJson.getFeatureTransformer(transformationContext);

        if (featureTransformer.isPresent()) {
            OptionalLong numRet = OptionalLong.of(0);
            OptionalLong numMat = OptionalLong.of(0);

            try {
                featureTransformer.get().onStart(numRet, numMat);
                featureTransformer.get().onEnd();


            } catch (Exception e) {
                throw new IllegalStateException("Could not generate empty GeoJson");
            }

        } else {
            throw new IllegalStateException("Could not acquire FeatureTransformer");
        }

        return true;
    }

    /**
     * deletes the specified JsonTile. This is used when a JsonTile is corrupt. After the deletion the JsonTile is immediately generated again.
     *
     * @param tileFileJson      The Json Tile, which should be deleted
     * @return true if the json Tile is successfully deleted
     * @throws FileNotFoundException
     */
    public static boolean deleteJSON(File tileFileJson) throws FileNotFoundException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonFeatureCollection;
        BufferedReader br = new BufferedReader(new FileReader(tileFileJson));
        try {
            if (br.readLine() == null) {
                jsonFeatureCollection = null;
            } else {
                jsonFeatureCollection = mapper.readValue(tileFileJson, new TypeReference<LinkedHashMap>() {
                });
            }
        } catch (IOException e) {
            tileFileJson.delete();
            return true;
        }
        return false;
    }
}

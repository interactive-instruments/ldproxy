/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.api.ImmutableFeatureTransformationContextGeneric;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesQuery;
import de.ii.ldproxy.ogcapi.infra.rest.ImmutableOgcApiRequestContext;
import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.CqlPredicate;
import de.ii.xtraplatform.cql.domain.Intersects;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureStream2;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import org.apache.http.NameValuePair;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletionException;

/**
 * This class is responsible for generation and deletion of JSON Tiles.
 */

public class TileGeneratorJson {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointTiles.class);

    private static final VectorTileMapGenerator vectorTileMapGenerator = new VectorTileMapGenerator();

    /**
     * generate the GeoJSON tile file in the cache
     *
     * @param tileFile          the file object of the tile in the cache
     * @param crsTransformerFactory the coordinate reference system transformation object to transform coordinates
     * @param uriInfo           context parameter to get the query parameters
     * @param filters           filters specified in the query
     * @param filterableFields  all possible fields you can use as a filter
     * @param uriCustomizer     the URI, split in host, path and query
     * @param mediaType         the media type, here json
     * @param isCollection      boolean collection or dataset Tile
     * @param tile              the tile which should be generated
     * @return true, if the file was generated successfully, false, if an error occurred
     */
    static boolean generateTileJson(File tileFile, CrsTransformerFactory crsTransformerFactory, @Context UriInfo uriInfo,
                                    Map<String, String> filters, Map<String, String> filterableFields,
                                    URICustomizer uriCustomizer, OgcApiMediaType mediaType, boolean isCollection,
                                    VectorTile tile, I18n i18n, Optional<Locale> language, OgcApiFeaturesQuery queryParser) {
        // TODO add support for multi-collection GeoJSON output

        String collectionId = tile.getCollectionId();
        OgcApiApiDataV2 serviceData = tile.getApiData();
        TileMatrixSet tileMatrixSet = tile.getTileMatrixSet();
        int level = tile.getLevel();
        int col = tile.getCol();
        int row = tile.getRow();
        FeatureProvider2 featureProvider = tile.getFeatureProvider();
        OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson = tile.getWfs3OutputFormatGeoJson();

        if (collectionId == null || !featureProvider.supportsQueries()) {
            return false;
        }


        OutputStream outputStream;
        try {
            outputStream = new FileOutputStream(tileFile);
        } catch (FileNotFoundException e) {
            LOGGER.error("File not found: " + e.getMessage());
            return false;
        }

        String geometryField = filterableFields.get("bbox");

        CqlPredicate spatialPredicate = CqlPredicate.of(Intersects.of(geometryField, tile.getBoundingBox()));

        // calculate maxAllowableOffset
        double maxAllowableOffsetTileMatrixSet = tileMatrixSet.getMaxAllowableOffset(level, row, col);
        double maxAllowableOffsetNative = maxAllowableOffsetTileMatrixSet; // TODO convert to native CRS units
        double maxAllowableOffsetCrs84 = 0;
        try {
            maxAllowableOffsetCrs84 = tileMatrixSet.getMaxAllowableOffset(level, row, col, OgcCrs.CRS84, crsTransformerFactory);
        } catch (CrsTransformationException e) {
            LOGGER.error("CRS transformation error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }


        ImmutableFeatureQuery.Builder queryBuilder;
        if (uriInfo != null) {
            MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();

            List<String> propertiesList = VectorTile.getPropertiesList(queryParameters);

            //TODO: support 3d?
            queryBuilder = ImmutableFeatureQuery.builder()
                                                .type(collectionId)
                                                .filter(CqlFilter.of(spatialPredicate))
                                                .maxAllowableOffset(maxAllowableOffsetNative)
                                                .fields(propertiesList)
                                                .crs(OgcCrs.CRS84);


            if (filters != null && filterableFields != null) {
                if (!filters.isEmpty()) {
                    Optional<String> filterLang = uriCustomizer.getQueryParams().stream()
                            .filter(param -> "filter-lang".equals(param.getName()))
                            .map(NameValuePair::getValue)
                            .findFirst();
                    Cql.Format cqlFormat = Cql.Format.TEXT;
                    if (filterLang.isPresent() && "cql-json".equals(filterLang.get())) {
                        cqlFormat = Cql.Format.JSON;
                    }
                    Optional<CqlFilter> otherFilters = queryParser.getFilterFromQuery(filters, filterableFields, ImmutableSet.of("filter"), cqlFormat);
                    CqlFilter combinedFilter = otherFilters.isPresent() ? CqlFilter.of(And.of(otherFilters.get(), spatialPredicate)) : CqlFilter.of(spatialPredicate);

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Filter: {}", combinedFilter);
                    }

                    queryBuilder
                            .filter(combinedFilter)
                            .type(collectionId)
                            .maxAllowableOffset(maxAllowableOffsetNative)
                            .fields(propertiesList);
                }
            }
        } else {
            queryBuilder = ImmutableFeatureQuery.builder()
                                                .type(collectionId)
                                                .filter(CqlFilter.of(spatialPredicate))
                                                .maxAllowableOffset(maxAllowableOffsetNative);
        }
        queryBuilder.build();


        List<OgcApiLink> ogcApiLinks = new ArrayList<>();


        if (isCollection) {
            OgcApiMediaType alternateMediaType;
            alternateMediaType = new ImmutableOgcApiMediaType.Builder()
                    .type(new MediaType("application", "vnd.mapbox-vector-tile"))
                    .label("MVT")
                    .build();
            final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
            ogcApiLinks = vectorTilesLinkGenerator.generateGeoJSONTileLinks(uriCustomizer, mediaType, alternateMediaType, tileMatrixSet.getId(), Integer.toString(level), Integer.toString(row), Integer.toString(col), VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(serviceData), collectionId, "application/vnd.mapbox-vector-tile", true), VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(serviceData), collectionId, "application/json", true), i18n, language);
        }


        FeatureStream2 featureTransformStream = featureProvider.queries().getFeatureStream2(queryBuilder.build());

        try {
            FeatureTransformationContext transformationContext = new ImmutableFeatureTransformationContextGeneric.Builder()
                    .apiData(serviceData)
                    .collectionId(collectionId)
                    .ogcApiRequest(new ImmutableOgcApiRequestContext.Builder()
                            .api(tile.getApi())
                            .requestUri(uriCustomizer.build())
                            .mediaType(mediaType)
                            .build())
                    //TODO: support 3d?
                    .crsTransformer(crsTransformerFactory.getTransformer(featureProvider.crs().getNativeCrs(), OgcCrs.CRS84))
                    .defaultCrs(OgcCrs.CRS84)
                    .links(ogcApiLinks)
                    .isFeatureCollection(true)
                    .limit(0) //TODO
                    .offset(0)
                    .maxAllowableOffset(maxAllowableOffsetCrs84)
                    .outputStream(outputStream)
                    .build();

            Optional<FeatureTransformer2> featureTransformer = wfs3OutputFormatGeoJson.getFeatureTransformer(transformationContext, language);

            if (featureTransformer.isPresent()) {
                featureTransformStream.runWith(featureTransformer.get())
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
     * @param tileFile                the file object of the tile in the cache
     * @param tileMatrixSet           the tiling scheme the JSON Tile should have
     * @param datasetData             the Service data of the Wfs3Service
     * @param wfs3OutputFormatGeoJson the wfs3OutputFormatGeoJSON
     * @param collectionId            the id of the collection in which the Tile should be generated
     * @param isCollection            boolean collection or dataset Tile
     * @param wfs3Request             the request
     * @param level                   the zoom level as an integer
     * @param row                     the row number as an integer
     * @param col                     the col number as an integer
     * @param crsTransformerFactory       system transformation object to transform coordinates
     * @param service                 the service
     * @return true, if the file was generated successfully, false, if an error occurred
     */
    public static boolean generateEmptyJSON(File tileFile, TileMatrixSet tileMatrixSet, OgcApiApiDataV2 datasetData,
                                            OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson, String collectionId,
                                            boolean isCollection, OgcApiRequestContext wfs3Request, int level, int row,
                                            int col, CrsTransformerFactory crsTransformerFactory, OgcApiApi service,
                                            I18n i18n, Optional<Locale> language) {

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


        double maxAllowableOffsetTileMatrixSet = tileMatrixSet.getMaxAllowableOffset(level, row, col);
        double maxAllowableOffsetNative = maxAllowableOffsetTileMatrixSet; // TODO convert to native CRS units
        double maxAllowableOffsetCrs84 = 0;
        try {
            maxAllowableOffsetCrs84 = tileMatrixSet.getMaxAllowableOffset(level, row, col, OgcCrs.CRS84, crsTransformerFactory);
        } catch (CrsTransformationException e) {
            LOGGER.error("CRS transformation error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        List<OgcApiLink> ogcApiLinks = new ArrayList<>();


        if (isCollection) {
            OgcApiMediaType alternateMediaType;
            alternateMediaType = new ImmutableOgcApiMediaType.Builder()
                    .type(new MediaType("application", "vnd.mapbox-vector-tile"))
                    .label("MVT")
                    .build();
            final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
            ogcApiLinks = vectorTilesLinkGenerator.generateGeoJSONTileLinks(wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), alternateMediaType, tileMatrixSet.getId(), Integer.toString(level), Integer.toString(row), Integer.toString(col), VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, "application/vnd.mapbox-vector-tile", true), VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, "application/json", true), i18n, language);
        }

        FeatureTransformationContext transformationContext = new ImmutableFeatureTransformationContextGeneric.Builder()
                .apiData(datasetData)
                .collectionId(collectionId)
                .ogcApiRequest(wfs3Request)
                .defaultCrs(OgcCrs.CRS84)
                .links(ogcApiLinks)
                .isFeatureCollection(true)
                .maxAllowableOffset(maxAllowableOffsetCrs84)
                .outputStream(outputStream)
                .limit(0) // TODO
                .offset(0)
                .build();

        Optional<FeatureTransformer2> featureTransformer = wfs3OutputFormatGeoJson.getFeatureTransformer(transformationContext, wfs3Request.getLanguage());

        if (featureTransformer.isPresent()) {
            OptionalLong numRet = OptionalLong.of(0);
            OptionalLong numMat = OptionalLong.of(0);

            try {
                featureTransformer.get()
                                  .onStart(numRet, numMat);
                featureTransformer.get()
                                  .onEnd();


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
     * @param tileFileJson The Json Tile, which should be deleted
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

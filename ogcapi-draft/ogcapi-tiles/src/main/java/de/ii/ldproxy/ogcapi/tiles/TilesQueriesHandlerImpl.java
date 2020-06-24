/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import de.ii.ldproxy.ogcapi.application.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreQueriesHandlerImpl;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.xtraplatform.codelists.CodelistRegistry;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureStream2;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import org.apache.commons.io.FileUtils;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

@Component
@Instantiate
@Provides
public class TilesQueriesHandlerImpl implements TilesQueriesHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TilesQueriesHandlerImpl.class);

    private final I18n i18n;
    private final CrsTransformerFactory crsTransformerFactory;
    private final Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> queryHandlers;
    private final MetricRegistry metricRegistry;
    private CodelistRegistry codelistRegistry;
    private final OgcApiExtensionRegistry extensionRegistry;
    private final TileMatrixSetLimitsGenerator limitsGenerator;
    private final TilesCache tilesCache;

    public TilesQueriesHandlerImpl(@Requires I18n i18n,
                                   @Requires CrsTransformerFactory crsTransformerFactory,
                                   @Requires Dropwizard dropwizard,
                                   @Requires CodelistRegistry codelistRegistry,
                                   @Requires OgcApiExtensionRegistry extensionRegistry,
                                   @Requires TileMatrixSetLimitsGenerator limitsGenerator,
                                   @Requires TilesCache tilesCache) {
        this.i18n = i18n;
        this.crsTransformerFactory = crsTransformerFactory;
        this.codelistRegistry = codelistRegistry;

        this.metricRegistry = dropwizard.getEnvironment()
                                        .metrics();
        this.extensionRegistry = extensionRegistry;
        this.limitsGenerator = limitsGenerator;
        this.tilesCache = tilesCache;

        this.queryHandlers = new ImmutableMap.Builder()
                .put(Query.TILE_SETS, OgcApiQueryHandler.with(OgcApiQueryInputTileSets.class, this::getTileSetsResponse))
                .put(Query.TILE_SET, OgcApiQueryHandler.with(OgcApiQueryInputTileSet.class, this::getTileSetResponse))
                .put(Query.SINGLE_LAYER_TILE, OgcApiQueryHandler.with(OgcApiQueryInputTileSingleLayer.class, this::getSingleLayerTileResponse))
                .put(Query.MULTI_LAYER_TILE, OgcApiQueryHandler.with(OgcApiQueryInputTileMultiLayer.class, this::getMultiLayerTileResponse))
                .put(Query.EMPTY_TILE, OgcApiQueryHandler.with(OgcApiQueryInputTileEmpty.class, this::getEmptyTileResponse))
                .put(Query.TILE_FILE, OgcApiQueryHandler.with(OgcApiQueryInputTileFile.class, this::getTileFileResponse))
                .build();
    }

    @Override
    public Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    private Response getTileSetsResponse(OgcApiQueryInputTileSets queryInput, OgcApiRequestContext requestContext) {
        OgcApiApi api = requestContext.getApi();
        Optional<String> collectionId = queryInput.getCollectionId();
        String path = collectionId.isPresent() ? 
                "/collections/"+collectionId.get()+"/tiles" :
                "/tiles";

        TileSetsFormatExtension outputFormat = api.getOutputFormat(TileSetsFormatExtension.class, requestContext.getMediaType(), path)
                .orElseThrow(NotAcceptableException::new);

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        Optional<FeatureTypeConfigurationOgcApi> featureType = collectionId.isPresent() ?
                Optional.of(requestContext.getApi().getData().getCollections().get(collectionId.get())) :
                Optional.empty();
        Map<String, MinMax> tileMatrixSetZoomLevels = queryInput.getTileMatrixSetZoomLevels();
        Optional<double[]> center = Optional.ofNullable(queryInput.getCenter());

        List<OgcApiMediaType> tileSetFormats = extensionRegistry.getExtensionsForType(TileSetFormatExtension.class)
                .stream()
                .filter(format -> format.isEnabledForApi(api.getData(),collectionId))
                .filter(format -> {
                    Optional<TilesConfiguration> config = collectionId.isPresent() ?
                            format.getExtensionConfiguration(api.getData(), api.getData().getCollections().get(collectionId.get()), TilesConfiguration.class) :
                            format.getExtensionConfiguration(api.getData(), TilesConfiguration.class);
                    return config.isPresent() && (config.get().getFormats()==null || (config.get().getFormats().isEmpty() || config.get().getFormats().contains(format.getMediaType().type().toString())));
                })
                .map(format -> format.getMediaType())
                .collect(Collectors.toList());

        List<OgcApiMediaType> tileFormats = extensionRegistry.getExtensionsForType(TileFormatExtension.class)
                .stream()
                .filter(format -> format.isEnabledForApi(api.getData(),collectionId))
                .filter(format -> {
                    Optional<TilesConfiguration> config = collectionId.isPresent() ?
                            format.getExtensionConfiguration(api.getData(), api.getData().getCollections().get(collectionId.get()), TilesConfiguration.class) :
                            format.getExtensionConfiguration(api.getData(), TilesConfiguration.class);
                    return config.isPresent() && (config.get().getFormats()==null || (config.get().getFormats().isEmpty() || config.get().getFormats().contains(format.getMediaType().type().toString())));
                })
                .map(format -> format.getMediaType())
                .collect(Collectors.toList());

        List<OgcApiLink> links = vectorTilesLinkGenerator.generateTilesLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                queryInput.getIncludeHomeLink(),
                collectionId.isPresent(),
                false,
                tileSetFormats,
                tileFormats,
                i18n,
                requestContext.getLanguage());

        TileSets tiles = ImmutableTileSets.builder()
                .title(featureType.isPresent()?
                        featureType.get().getLabel() :
                        api.getData().getLabel())
                .description(featureType.isPresent()?
                        featureType.get().getDescription().orElse("") :
                        api.getData().getDescription().orElse(""))
                .tileMatrixSetLinks(
                        tileMatrixSetZoomLevels
                                .keySet()
                                .stream()
                                .map(tileMatrixSetId -> ImmutableTileSet.builder()
                                        .tileMatrixSet(tileMatrixSetId)
                                        .tileMatrixSetURI(requestContext.getUriCustomizer()
                                                .copy()
                                                .removeLastPathSegments(3)
                                                .clearParameters()
                                                .ensureLastPathSegments("tileMatrixSets", tileMatrixSetId)
                                                .toString())
                                        .tileMatrixSetLimits(collectionId.isPresent() ?
                                                limitsGenerator.getCollectionTileMatrixSetLimits(
                                                    api.getData(), collectionId.get(), getTileMatrixSetById(tileMatrixSetId),
                                                    tileMatrixSetZoomLevels.get(tileMatrixSetId), crsTransformerFactory) :
                                                limitsGenerator.getTileMatrixSetLimits(api.getData(), getTileMatrixSetById(tileMatrixSetId),
                                                        tileMatrixSetZoomLevels.get(tileMatrixSetId), crsTransformerFactory))
                                        .defaultZoomLevel(Objects.nonNull(tileMatrixSetZoomLevels.get(tileMatrixSetId)) ? tileMatrixSetZoomLevels.get(tileMatrixSetId).getDefault() : Optional.empty())
                                        .build())
                                .filter(value -> Objects.nonNull(value))
                                .collect(Collectors.toList()))
                .defaultCenter(center)
                .links(links)
                .build();

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(outputFormat.getTileSetsEntity(tiles, collectionId, api, requestContext))
                .build();
    }

    private Response getTileSetResponse(OgcApiQueryInputTileSet queryInput, OgcApiRequestContext requestContext) {
        OgcApiApi api = requestContext.getApi();
        String tileMatrixSetId = queryInput.getTileMatrixSetId();
        Optional<String> collectionId = queryInput.getCollectionId();
        String path = collectionId.isPresent() ?
                "/collections/"+collectionId.get()+"/tiles/"+tileMatrixSetId :
                "/tiles/"+tileMatrixSetId;

        TileSetFormatExtension outputFormat = api.getOutputFormat(TileSetFormatExtension.class, requestContext.getMediaType(), path)
                .orElseThrow(NotAcceptableException::new);

        List<OgcApiMediaType> tileFormats = extensionRegistry.getExtensionsForType(TileFormatExtension.class)
                .stream()
                .filter(format -> format.isEnabledForApi(api.getData(), collectionId))
                .filter(format -> {
                    Optional<TilesConfiguration> config = collectionId.isPresent() ?
                            format.getExtensionConfiguration(api.getData(), api.getData().getCollections().get(collectionId.get()), TilesConfiguration.class) :
                            format.getExtensionConfiguration(api.getData(), TilesConfiguration.class);
                    return config.isPresent() && (config.get().getFormats()==null || (config.get().getFormats().isEmpty() || config.get().getFormats().contains(format.getMediaType().type().toString())));
                })
                .map(format -> format.getMediaType())
                .collect(Collectors.toList());

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
        List<OgcApiLink> links = vectorTilesLinkGenerator.generateTilesLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                queryInput.getIncludeHomeLink(),
                queryInput.getCollectionId().isPresent(),
                true,
                ImmutableList.of(),
                tileFormats,
                i18n,
                requestContext.getLanguage());

        MinMax zoomLevels = queryInput.getZoomLevels();
        double[] center = queryInput.getCenter();
        
        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(outputFormat.getTileSetEntity(api.getData(), requestContext, collectionId,
                                                      getTileMatrixSetById(tileMatrixSetId), zoomLevels, center, links))
                .build();
    }

    private Response getSingleLayerTileResponse(OgcApiQueryInputTileSingleLayer queryInput, OgcApiRequestContext requestContext) {
        OgcApiApi api = requestContext.getApi();
        Tile tile = queryInput.getTile();
        String collectionId = tile.getCollectionId();
        FeatureProvider2 featureProvider = tile.getFeatureProvider();
        FeatureQuery query = queryInput.getQuery();
        TileMatrixSet tileMatrixSet = tile.getTileMatrixSet();
        String tileMatrixSetId = tileMatrixSet.getId();
        int tileLevel = tile.getTileLevel();
        int tileRow = tile.getTileRow();
        int tileCol = tile.getTileCol();
        String path = "/collections/"+collectionId+"/tiles/"+tileMatrixSetId+"/"+tileLevel+"/"+tileRow+"/"+tileCol;
        TileFormatExtension outputFormat = tile.getOutputFormat();

        // process parameters and generate query
        Optional<CrsTransformer> crsTransformer = Optional.empty();
        boolean swapCoordinates = false;

        EpsgCrs targetCrs = query.getCrs()
                                 .orElse(queryInput.getDefaultCrs());
        if (featureProvider.supportsCrs()) {
            EpsgCrs sourceCrs = featureProvider.crs()
                                               .getNativeCrs();
            crsTransformer = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
            swapCoordinates = crsTransformer.isPresent() ?
                    crsTransformer.get()
                                  .needsCoordinateSwap() :
                    query.getCrs().isPresent() && featureProvider.crs().shouldSwapCoordinates(query.getCrs().get());
        }

        List<OgcApiLink> links = new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                i18n,
                requestContext.getLanguage());

        ImmutableFeatureTransformationContextTiles.Builder transformationContext = new ImmutableFeatureTransformationContextTiles.Builder()
                .apiData(api.getData())
                .tile(tile)
                .tileFile(tilesCache.getFile(tile))
                .collectionId(collectionId)
                .ogcApiRequest(requestContext)
                .crsTransformer(crsTransformer)
                .crsTransformerFactory(crsTransformerFactory)
                .shouldSwapCoordinates(swapCoordinates)
                .codelists(codelistRegistry.getCodelists())
                .defaultCrs(queryInput.getDefaultCrs())
                .links(links)
                .isFeatureCollection(true)
                .fields(query.getFields())
                .limit(query.getLimit())
                .offset(0)
                .i18n(i18n);

        if (queryInput.getOutputStream().isPresent()) {

            // do not stream as a response
            if (outputFormat.canTransformFeatures()) {
                FeatureStream2 featureStream = featureProvider.queries()
                        .getFeatureStream2(query);

                try {
                    Optional<FeatureTransformer2> featureTransformer = outputFormat.getFeatureTransformer(
                            transformationContext.outputStream(queryInput.getOutputStream().get()).build(),
                            requestContext.getLanguage());

                    if (featureTransformer.isPresent()) {
                        featureStream.runWith(featureTransformer.get())
                                .toCompletableFuture()
                                .join();
                    } else {
                        throw new IllegalStateException("Could not acquire FeatureTransformer");
                    }

                } catch (CompletionException e) {
                    if (e.getCause() instanceof WebApplicationException) {
                        throw (WebApplicationException) e.getCause();
                    }
                    throw new IllegalStateException("Feature stream error", e.getCause());
                }
            } else {
                throw new NotAcceptableException();
            }

            return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                    .entity(queryInput.getOutputStream().get())
                    .build();
        }

        // streaming output as response
        StreamingOutput streamingOutput;
        if (outputFormat.canTransformFeatures()) {
            FeatureStream2 featureStream = featureProvider.queries()
                    .getFeatureStream2(query);

            streamingOutput = stream(featureStream, false,
                    outputStream -> outputFormat.getFeatureTransformer(
                            transformationContext.outputStream(outputStream).build(),
                            requestContext.getLanguage())
                            .get());
        } else {
            throw new NotAcceptableException();
        }

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(streamingOutput)
                .build();
    }

    private Response getMultiLayerTileResponse(OgcApiQueryInputTileMultiLayer queryInput, OgcApiRequestContext requestContext) {
        OgcApiApi api = requestContext.getApi();
        Tile multiLayerTile = queryInput.getTile();
        List<String> collectionIds = multiLayerTile.getCollectionIds();
        Map<String, FeatureQuery> queryMap = queryInput.getQueryMap();
        Map<String, Tile> singleLayerTileMap = queryInput.getSingleLayerTileMap();
        FeatureProvider2 featureProvider = multiLayerTile.getFeatureProvider();
        TileMatrixSet tileMatrixSet = multiLayerTile.getTileMatrixSet();
        String tileMatrixSetId = tileMatrixSet.getId();
        int tileLevel = multiLayerTile.getTileLevel();
        int tileRow = multiLayerTile.getTileRow();
        int tileCol = multiLayerTile.getTileCol();
        String path = "/tiles/"+tileMatrixSetId+"/"+tileLevel+"/"+tileRow+"/"+tileCol;
        TileFormatExtension outputFormat = multiLayerTile.getOutputFormat();

        // process parameters and generate query
        Optional<CrsTransformer> crsTransformer = Optional.empty();
        boolean swapCoordinates = false;

        EpsgCrs targetCrs = tileMatrixSet.getCrs();
        if (featureProvider.supportsCrs()) {
            EpsgCrs sourceCrs = featureProvider.crs()
                    .getNativeCrs();
            crsTransformer = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
            swapCoordinates = crsTransformer.isPresent() ?
                    crsTransformer.get()
                            .needsCoordinateSwap() :
                    featureProvider.crs().shouldSwapCoordinates(targetCrs);
        }

        List<OgcApiLink> links = new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                i18n,
                requestContext.getLanguage());

        Map<String, ByteArrayOutputStream> byteArrayMap = collectionIds.stream()
                .collect(ImmutableMap.toImmutableMap(collectionId -> collectionId, collectionId -> new ByteArrayOutputStream()));

        for (String collectionId : collectionIds) {
            // TODO limitation of the current model: all layers have to come from the same feature provider and use the same CRS

            Tile tile = singleLayerTileMap.get(collectionId);
            File tileFile = tilesCache.getFile(tile);
            ByteArrayOutputStream singleLayerOutputStream = byteArrayMap.get(collectionId);
            if (!multiLayerTile.getTemporary() && tileFile.exists()) {
                // use cached tile
                try {
                    if (FileUtils.sizeOf(tileFile)>0) {
                        singleLayerOutputStream.write(FileUtils.readFileToByteArray(tileFile));
                        singleLayerOutputStream.flush();
                    }
                    continue;
                } catch (IOException e) {
                    // could not read the cache, generate the tile
                }
            }

            FeatureQuery query = queryMap.get(collectionId);
            ImmutableFeatureTransformationContextTiles transformationContext = new ImmutableFeatureTransformationContextTiles.Builder()
                    .apiData(api.getData())
                    .tile(tile)
                    .tileFile(tilesCache.getFile(tile))
                    .collectionId(collectionId)
                    .ogcApiRequest(requestContext)
                    .crsTransformer(crsTransformer)
                    .crsTransformerFactory(crsTransformerFactory)
                    .shouldSwapCoordinates(swapCoordinates)
                    .codelists(codelistRegistry.getCodelists())
                    .defaultCrs(queryInput.getDefaultCrs())
                    .links(links)
                    .isFeatureCollection(true)
                    .fields(query.getFields())
                    .limit(query.getLimit())
                    .offset(0)
                    .i18n(i18n)
                    .outputStream(singleLayerOutputStream)
                    .build();

            if (outputFormat.canTransformFeatures()) {
                FeatureStream2 featureStream = featureProvider.queries()
                        .getFeatureStream2(query);

                try {
                    Optional<FeatureTransformer2> featureTransformer = outputFormat.getFeatureTransformer(transformationContext, requestContext.getLanguage());

                    if (featureTransformer.isPresent()) {
                        featureStream.runWith(featureTransformer.get())
                                .toCompletableFuture()
                                .join();
                    } else {
                        throw new IllegalStateException("Could not acquire FeatureTransformer");
                    }

                } catch (CompletionException e) {
                    if (e.getCause() instanceof WebApplicationException) {
                        throw (WebApplicationException) e.getCause();
                    }
                    throw new IllegalStateException("Feature stream error", e.getCause());
                }
            } else {
                throw new NotAcceptableException();
            }
        }

        TileFormatExtension.MultiLayerTileContent result = outputFormat.combineSingleLayerTilesToMultiLayerTile(tileMatrixSet, singleLayerTileMap, byteArrayMap);

        // try to write/update tile in cache, if all collections have been processed
        if (result.isComplete) {
            File tileFile = tilesCache.getFile(multiLayerTile);
            if (!tileFile.exists() || tileFile.canWrite()) {
                try {
                    FileUtils.writeByteArrayToFile(tileFile, result.byteArray);
                } catch (IOException e) {
                    String msg = "Failure to write the multi-layer file of tile {}/{}/{}/{} in dataset '{}', format '{}' to the cache.";
                    LOGGER.info(msg, tileMatrixSet.getId(), tileLevel, tileRow, tileCol, api.getId(), outputFormat.getExtension());
                }
            }
        }

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(result.byteArray)
                .build();
    }

    private Response getTileFileResponse(OgcApiQueryInputTileFile queryInput, OgcApiRequestContext requestContext) {

        StreamingOutput streamingOutput = outputStream -> {
            ByteStreams.copy(new FileInputStream(queryInput.getTileFile()), outputStream);
        };

        List<OgcApiLink> links = new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                i18n,
                requestContext.getLanguage());

        return prepareSuccessResponse(requestContext.getApi(), requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(streamingOutput)
                .build();
    }

    private Response getEmptyTileResponse(OgcApiQueryInputTileEmpty queryInput, OgcApiRequestContext requestContext) {

        List<OgcApiLink> links = new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                i18n,
                requestContext.getLanguage());

        Tile tile = queryInput.getTile();
        return prepareSuccessResponse(requestContext.getApi(), requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(tile.getOutputFormat().getEmptyTile(tile))
                .build();
    }

    private StreamingOutput stream(FeatureStream2 featureTransformStream, boolean failIfEmpty,
                                   final Function<OutputStream, FeatureTransformer2> featureTransformer) {
        Timer.Context timer = metricRegistry.timer(name(OgcApiFeaturesCoreQueriesHandlerImpl.class, "stream"))
                                            .time();

        return outputStream -> {
            try {
                FeatureStream2.Result result = featureTransformStream.runWith(featureTransformer.apply(outputStream))
                                                                     .toCompletableFuture()
                                                                     .join();
                timer.stop();

                if (result.getError()
                          .isPresent()) {
                    LOGGER.error("Feature stream error", result.getError()
                                                               .get());

                    throw new InternalServerErrorException("There was an error processing your request. It has been logged.");
                }

                if (result.isEmpty() && failIfEmpty) {
                    throw new NotFoundException();
                }

            } catch (CompletionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException) e.getCause();
                }
                throw new IllegalStateException("Feature stream error", e.getCause());
            }
        };
    }

    private TileMatrixSet getTileMatrixSetById(String tileMatrixSetId) {
        return extensionRegistry.getExtensionsForType(TileMatrixSet.class).stream()
                    .filter(tms -> tms.getId().equals(tileMatrixSetId))
                    .findAny()
                    .orElseThrow(() -> new ServerErrorException("TileMatrixSet not found: "+tileMatrixSetId, 500));
    }
}

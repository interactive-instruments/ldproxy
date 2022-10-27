/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.DefaultLinksGenerator;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HeaderCaching;
import de.ii.ogcapi.foundation.domain.HeaderContentDisposition;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.tiles.domain.ImmutableTileSets;
import de.ii.ogcapi.tiles.domain.ImmutableTileSets.Builder;
import de.ii.ogcapi.tiles.domain.MinMax;
import de.ii.ogcapi.tiles.domain.StaticTileProviderStore;
import de.ii.ogcapi.tiles.domain.Tile;
import de.ii.ogcapi.tiles.domain.TileCache;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileFormatWithQuerySupportExtension;
import de.ii.ogcapi.tiles.domain.TileSet;
import de.ii.ogcapi.tiles.domain.TileSet.DataType;
import de.ii.ogcapi.tiles.domain.TileSetFormatExtension;
import de.ii.ogcapi.tiles.domain.TileSets;
import de.ii.ogcapi.tiles.domain.TileSetsFormatExtension;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileProvider;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import de.ii.ogcapi.tiles.domain.provider.TileResult.Status;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.domain.FeatureStream;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.web.domain.ETag;
import de.ii.xtraplatform.web.domain.LastModified;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class TilesQueriesHandlerImpl implements TilesQueriesHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(TilesQueriesHandlerImpl.class);

  private final I18n i18n;
  private final CrsTransformerFactory crsTransformerFactory;
  private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
  private final EntityRegistry entityRegistry;
  private final ExtensionRegistry extensionRegistry;
  private final TileMatrixSetLimitsGenerator limitsGenerator;
  private final TileCache tileCache;
  private final StaticTileProviderStore staticTileProviderStore;
  private final FeaturesCoreProviders providers;
  private final TileMatrixSetRepository tileMatrixSetRepository;
  private final TileProvider tileProvider;

  @Inject
  public TilesQueriesHandlerImpl(
      I18n i18n,
      CrsTransformerFactory crsTransformerFactory,
      EntityRegistry entityRegistry,
      ExtensionRegistry extensionRegistry,
      TileMatrixSetLimitsGenerator limitsGenerator,
      TileCache tileCache,
      StaticTileProviderStore staticTileProviderStore,
      FeaturesCoreProviders providers,
      TileMatrixSetRepository tileMatrixSetRepository,
      TileProvider tileProvider) {
    this.i18n = i18n;
    this.crsTransformerFactory = crsTransformerFactory;
    this.entityRegistry = entityRegistry;
    this.extensionRegistry = extensionRegistry;
    this.limitsGenerator = limitsGenerator;
    this.tileCache = tileCache;
    this.staticTileProviderStore = staticTileProviderStore;
    this.providers = providers;
    this.tileMatrixSetRepository = tileMatrixSetRepository;
    this.tileProvider = tileProvider;

    this.queryHandlers =
        ImmutableMap.<Query, QueryHandler<? extends QueryInput>>builder()
            .put(
                Query.TILE_SETS,
                QueryHandler.with(QueryInputTileSets.class, this::getTileSetsResponse))
            .put(
                Query.TILE_SET,
                QueryHandler.with(QueryInputTileSet.class, this::getTileSetResponse))
            .put(
                Query.SINGLE_LAYER_TILE,
                QueryHandler.with(
                    QueryInputTileSingleLayer.class, this::getSingleLayerTileResponse))
            .put(
                Query.MULTI_LAYER_TILE,
                QueryHandler.with(QueryInputTileMultiLayer.class, this::getMultiLayerTileResponse))
            .put(
                Query.EMPTY_TILE,
                QueryHandler.with(QueryInputTileEmpty.class, this::getEmptyTileResponse))
            .put(
                Query.TILE_STREAM,
                QueryHandler.with(QueryInputTileStream.class, this::getTileStreamResponse))
            .put(
                Query.MBTILES_TILE,
                QueryHandler.with(QueryInputTileMbtilesTile.class, this::getMbtilesTileResponse))
            .put(
                Query.TILESERVER_TILE,
                QueryHandler.with(
                    QueryInputTileTileServerTile.class, this::getTileServerTileResponse))
            .build();
  }

  @Override
  public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
    return queryHandlers;
  }

  private Response getTileSetsResponse(
      QueryInputTileSets queryInput, ApiRequestContext requestContext) {
    OgcApi api = requestContext.getApi();
    OgcApiDataV2 apiData = api.getData();
    Optional<String> collectionId = queryInput.getCollectionId();
    String definitionPath = queryInput.getPath();
    String path =
        collectionId
            .map(value -> definitionPath.replace("{collectionId}", value))
            .orElse(definitionPath);
    boolean onlyWebMercatorQuad = queryInput.getOnlyWebMercatorQuad();

    TileSetsFormatExtension outputFormat =
        api.getOutputFormat(
                TileSetsFormatExtension.class, requestContext.getMediaType(), path, collectionId)
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported for this resource.",
                            requestContext.getMediaType())));

    final TilesLinkGenerator tilesLinkGenerator = new TilesLinkGenerator();

    Optional<FeatureTypeConfigurationOgcApi> featureType =
        collectionId.map(s -> apiData.getCollections().get(s));
    Map<String, MinMax> tileMatrixSetZoomLevels = queryInput.getTileMatrixSetZoomLevels();
    List<Double> center = queryInput.getCenter();

    List<TileFormatExtension> tileFormats =
        extensionRegistry.getExtensionsForType(TileFormatExtension.class).stream()
            .filter(
                format ->
                    collectionId
                        .map(s -> format.isApplicable(apiData, s, definitionPath))
                        .orElseGet(() -> format.isApplicable(apiData, definitionPath)))
            // sort formats in the order specified in the configuration for consistency;
            // the first one will always used in the HTML representation
            .sorted(
                Comparator.comparing(
                    format -> queryInput.getTileEncodings().indexOf(format.getMediaType().label())))
            .collect(Collectors.toUnmodifiableList());

    Optional<DataType> dataType =
        tileFormats.stream().map(TileFormatExtension::getDataType).findAny();

    List<Link> links =
        tilesLinkGenerator.generateTileSetsLinks(
            requestContext.getUriCustomizer(),
            requestContext.getMediaType(),
            requestContext.getAlternateMediaTypes(),
            tileFormats,
            i18n,
            requestContext.getLanguage());

    Builder builder =
        ImmutableTileSets.builder()
            .title(featureType.isPresent() ? featureType.get().getLabel() : apiData.getLabel())
            .description(
                featureType
                    .map(ft -> ft.getDescription().orElse(""))
                    .orElseGet(() -> apiData.getDescription().orElse("")))
            .links(links);

    List<TileMatrixSet> tileMatrixSets =
        tileMatrixSetZoomLevels.keySet().stream()
            .map(this::getTileMatrixSetById)
            .filter(
                tileMatrixSet ->
                    !onlyWebMercatorQuad || tileMatrixSet.getId().equals("WebMercatorQuad"))
            .collect(Collectors.toUnmodifiableList());

    dataType.ifPresent(
        type ->
            builder.tilesets(
                tileMatrixSets.stream()
                    .map(
                        tileMatrixSet ->
                            TilesHelper.buildTileSet(
                                api,
                                tileMatrixSet,
                                tileMatrixSetZoomLevels.get(tileMatrixSet.getId()),
                                center,
                                collectionId,
                                type,
                                tilesLinkGenerator.generateTileSetEmbeddedLinks(
                                    requestContext.getUriCustomizer(),
                                    tileMatrixSet.getId(),
                                    tileFormats,
                                    i18n,
                                    requestContext.getLanguage()),
                                Optional.of(requestContext.getUriCustomizer().copy()),
                                crsTransformerFactory,
                                limitsGenerator,
                                providers,
                                entityRegistry))
                    .collect(Collectors.toUnmodifiableList())));

    TileSets tileSets = builder.build();

    Date lastModified = getLastModified(queryInput);
    EntityTag etag =
        !outputFormat.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
                || (collectionId.isEmpty()
                        ? apiData.getExtension(HtmlConfiguration.class)
                        : apiData.getExtension(HtmlConfiguration.class, collectionId.get()))
                    .map(HtmlConfiguration::getSendEtags)
                    .orElse(false)
            ? ETag.from(tileSets, TileSets.FUNNEL, outputFormat.getMediaType().label())
            : null;
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) return response.build();

    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader() ? links : null,
            HeaderCaching.of(lastModified, etag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format("tilesets.%s", outputFormat.getMediaType().fileExtension())))
        .entity(outputFormat.getTileSetsEntity(tileSets, collectionId, api, requestContext))
        .build();
  }

  private Response getTileSetResponse(
      QueryInputTileSet queryInput, ApiRequestContext requestContext) {
    OgcApi api = requestContext.getApi();
    OgcApiDataV2 apiData = api.getData();
    String tileMatrixSetId = queryInput.getTileMatrixSetId();
    Optional<String> collectionId = queryInput.getCollectionId();
    String definitionPath = queryInput.getPath();
    String path =
        collectionId
            .map(value -> definitionPath.replace("{collectionId}", value))
            .orElse(definitionPath)
            .replace("{tileMatrixSetId}", tileMatrixSetId);

    TileSetFormatExtension outputFormat =
        api.getOutputFormat(
                TileSetFormatExtension.class, requestContext.getMediaType(), path, collectionId)
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported for this resource.",
                            requestContext.getMediaType())));

    List<TileFormatExtension> tileFormats =
        extensionRegistry.getExtensionsForType(TileFormatExtension.class).stream()
            .filter(
                format ->
                    collectionId
                        .map(s -> format.isApplicable(apiData, s, definitionPath))
                        .orElseGet(() -> format.isApplicable(apiData, definitionPath)))
            .collect(Collectors.toUnmodifiableList());

    DataType dataType =
        tileFormats.stream()
            .map(TileFormatExtension::getDataType)
            .findAny()
            .orElseThrow(() -> new NotFoundException("No encoding found for this tile set."));

    final TilesLinkGenerator tilesLinkGenerator = new TilesLinkGenerator();
    List<Link> links =
        tilesLinkGenerator.generateTileSetLinks(
            requestContext.getUriCustomizer(),
            requestContext.getMediaType(),
            requestContext.getAlternateMediaTypes(),
            tileFormats,
            i18n,
            requestContext.getLanguage());

    MinMax zoomLevels = queryInput.getZoomLevels();
    List<Double> center = queryInput.getCenter();
    TileSet tileset =
        TilesHelper.buildTileSet(
            api,
            getTileMatrixSetById(tileMatrixSetId),
            zoomLevels,
            center,
            collectionId,
            dataType,
            links,
            Optional.of(requestContext.getUriCustomizer().copy()),
            crsTransformerFactory,
            limitsGenerator,
            providers,
            entityRegistry);

    Date lastModified = getLastModified(queryInput);
    EntityTag etag =
        !outputFormat.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
                || (collectionId.isEmpty()
                        ? apiData.getExtension(HtmlConfiguration.class)
                        : apiData.getExtension(HtmlConfiguration.class, collectionId.get()))
                    .map(HtmlConfiguration::getSendEtags)
                    .orElse(false)
            ? ETag.from(tileset, TileSet.FUNNEL, outputFormat.getMediaType().label())
            : null;
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) return response.build();

    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader() ? links : null,
            HeaderCaching.of(lastModified, etag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format(
                    "%s.%s",
                    tileset.getTileMatrixSetId(), outputFormat.getMediaType().fileExtension())))
        .entity(outputFormat.getTileSetEntity(tileset, apiData, collectionId, requestContext))
        .build();
  }

  private Response getSingleLayerTileResponse(
      QueryInputTileSingleLayer queryInput, ApiRequestContext requestContext) {
    /*
    OgcApi api = requestContext.getApi();
    OgcApiDataV2 apiData = api.getData();
    Tile tile = queryInput.getTile();
    String collectionId = tile.getCollectionId();
    FeatureProvider2 featureProvider = tile.getFeatureProvider().get();
    FeatureQuery query = queryInput.getQuery();

    if (!(tile.getOutputFormat() instanceof TileFormatWithQuerySupportExtension))
      throw new RuntimeException(
          String.format(
              "Unexpected tile format without query support. Found: %s",
              tile.getOutputFormat().getClass().getSimpleName()));
    TileFormatWithQuerySupportExtension outputFormat =
        (TileFormatWithQuerySupportExtension) tile.getOutputFormat();

    // process parameters and generate query
    Optional<CrsTransformer> crsTransformer = Optional.empty();

    EpsgCrs targetCrs = query.getCrs().orElse(queryInput.getDefaultCrs());
    if (featureProvider.supportsCrs()) {
      EpsgCrs sourceCrs = featureProvider.crs().getNativeCrs();
      crsTransformer = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
    }

    List<Link> links =
        new DefaultLinksGenerator()
            .generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                i18n,
                requestContext.getLanguage());

    String featureTypeId =
        apiData
            .getCollections()
            .get(collectionId)
            .getExtension(FeaturesCoreConfiguration.class)
            .map(cfg -> cfg.getFeatureType().orElse(collectionId))
            .orElse(collectionId);

    FeatureTransformationContextTiles transformationContext;
    try {
      transformationContext =
          new ImmutableFeatureTransformationContextTiles.Builder()
              .api(api)
              .apiData(apiData)
              .featureSchema(featureProvider.getData().getTypes().get(featureTypeId))
              .tile(tile)
              .collectionId(collectionId)
              .ogcApiRequest(requestContext)
              .crsTransformer(crsTransformer)
              .codelists(
                  entityRegistry.getEntitiesForType(Codelist.class).stream()
                      .collect(Collectors.toMap(PersistentEntity::getId, c -> c)))
              .defaultCrs(queryInput.getDefaultCrs())
              .links(links)
              .isFeatureCollection(true)
              .fields(query.getFields())
              .limit(query.getLimit())
              .offset(0)
              .i18n(i18n)
              .outputStream(new OutputStreamToByteConsumer())
              .build();
    } catch (Exception e) {
      throw new RuntimeException("Error building the tile transformation context.", e);
    }

    Optional<FeatureTokenEncoder<?>> encoder =
        outputFormat.getFeatureEncoder(transformationContext);

    if (outputFormat.supportsFeatureQuery() && encoder.isPresent()) {

      FeatureStream featureStream = featureProvider.queries().getFeatureStream(query);

      ResultReduced<byte[]> result =
          generateTile(
              featureStream, encoder.get(), transformationContext, outputFormat, featureTypeId);

      // internal processing, no need to process headers
      return prepareSuccessResponse(requestContext).entity(result.reduced()).build();
    } else {
      throw new NotAcceptableException(
          MessageFormat.format(
              "The requested media type {0} cannot be generated, because it does not support streaming.",
              requestContext.getMediaType().type()));
    }*/

    TileQuery tileQuery =
        ImmutableTileQuery.builder()
            .from(queryInput)
            // .outputFormat(queryInput.getOutputFormat())
            .layer(queryInput.getCollectionId()) // TODO: get layer name from cfg
            // .collectionIds(ImmutableList.of(collectionId))
            // .temporary(!useCache)
            // .isDatasetTile(false)
            .build();

    OgcApiDataV2 apiData = requestContext.getApi().getData();
    String featureTypeId =
        apiData
            .getCollections()
            .get(queryInput.getCollectionId())
            .getExtension(FeaturesCoreConfiguration.class)
            .map(cfg -> cfg.getFeatureType().orElse(queryInput.getCollectionId()))
            .orElse(queryInput.getCollectionId());

    // TODO
    if (!(queryInput.getOutputFormat() instanceof TileFormatWithQuerySupportExtension))
      throw new RuntimeException(
          String.format(
              "Unexpected tile format without query support. Found: %s",
              queryInput.getOutputFormat().getClass().getSimpleName()));
    TileFormatWithQuerySupportExtension outputFormat =
        (TileFormatWithQuerySupportExtension) queryInput.getOutputFormat();

    TileResult result = tileProvider.getTile(tileQuery);

    if (result.getStatus() == Status.OutOfBounds) {
      throw new NotFoundException();
    }

    // TODO: adjust and use getTileStreamResponse

    try {
      if (result.isAvailable()) {
        return prepareSuccessResponse(requestContext)
            .entity(result.getContent().get().readAllBytes())
            .build();
      }

      if (result.getStatus() == Status.NotFound && tileProvider.supportsGeneration()) {
        if (tileProvider.generator().supports(outputFormat.getMediaType().type())) {
          byte[] bytes =
              tileProvider.generator().generateTile(tileQuery, outputFormat.getMediaType().type());

          return prepareSuccessResponse(requestContext).entity(bytes).build();
        } else if (outputFormat.supportsFeatureQuery()) { // TODO: canEncode
          // TODO: pass encoder into or return FeatureStream?
          FeatureStream tileSource = tileProvider.generator().getTileSource(tileQuery);
        }
      }

    } catch (Throwable e) {
      boolean br = true;
    }

    // TODO: empty tile
    return null;
  }

  private Response getMultiLayerTileResponse(
      QueryInputTileMultiLayer queryInput, ApiRequestContext requestContext) {
    /*OgcApi api = requestContext.getApi();
    OgcApiDataV2 apiData = api.getData();
    Tile multiLayerTile = queryInput.getTile();
    List<String> collectionIds = multiLayerTile.getCollectionIds();
    Map<String, FeatureQuery> queryMap = queryInput.getQueryMap();
    Map<String, Tile> singleLayerTileMap = queryInput.getSingleLayerTileMap();
    FeatureProvider2 featureProvider = multiLayerTile.getFeatureProvider().get();
    TileMatrixSet tileMatrixSet = multiLayerTile.getTileMatrixSet();
    int tileLevel = multiLayerTile.getTileLevel();
    int tileRow = multiLayerTile.getTileRow();
    int tileCol = multiLayerTile.getTileCol();

    if (!(multiLayerTile.getOutputFormat() instanceof TileFormatWithQuerySupportExtension))
      throw new RuntimeException(
          String.format(
              "Unexpected tile format without query support. Found: %s",
              multiLayerTile.getOutputFormat().getClass().getSimpleName()));
    TileFormatWithQuerySupportExtension outputFormat =
        (TileFormatWithQuerySupportExtension) multiLayerTile.getOutputFormat();

    // process parameters and generate query
    Optional<CrsTransformer> crsTransformer = Optional.empty();

    EpsgCrs targetCrs = tileMatrixSet.getCrs();
    if (featureProvider.supportsCrs()) {
      EpsgCrs sourceCrs = featureProvider.crs().getNativeCrs();
      crsTransformer = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
    }

    List<Link> links =
        new DefaultLinksGenerator()
            .generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                i18n,
                requestContext.getLanguage());

    Map<String, ByteArrayOutputStream> byteArrayMap = new HashMap<>();

    for (String collectionId : collectionIds) {
      // TODO limitation of the current model: all layers have to come from the same feature
      // provider and use the same CRS

      Tile tile = singleLayerTileMap.get(collectionId);

      if (!multiLayerTile.getTemporary()) {
        // use cached tile
        try {
          Optional<InputStream> tileContent = tileCache.getTile(tile);
          if (tileContent.isPresent()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ByteStreams.copy(tileContent.get(), buffer);
            byteArrayMap.put(collectionId, buffer);
            continue;
          }
        } catch (SQLException | IOException e) {
          // could not read the cache, generate the tile
        }
      }

      String featureTypeId =
          apiData
              .getCollections()
              .get(collectionId)
              .getExtension(FeaturesCoreConfiguration.class)
              .map(cfg -> cfg.getFeatureType().orElse(collectionId))
              .orElse(collectionId);

      FeatureQuery query = queryMap.get(collectionId);
      ImmutableFeatureTransformationContextTiles transformationContext;
      try {
        transformationContext =
            new ImmutableFeatureTransformationContextTiles.Builder()
                .api(api)
                .apiData(apiData)
                .featureSchema(featureProvider.getData().getTypes().get(featureTypeId))
                .tile(tile)
                .collectionId(collectionId)
                .ogcApiRequest(requestContext)
                .crsTransformer(crsTransformer)
                .codelists(
                    entityRegistry.getEntitiesForType(Codelist.class).stream()
                        .collect(Collectors.toMap(PersistentEntity::getId, c -> c)))
                .defaultCrs(queryInput.getDefaultCrs())
                .links(links)
                .isFeatureCollection(true)
                .fields(query.getFields())
                .limit(query.getLimit())
                .offset(0)
                .i18n(i18n)
                .outputStream(new OutputStreamToByteConsumer())
                .build();
      } catch (Exception e) {
        throw new RuntimeException("Error building the tile transformation context.", e);
      }

      Optional<FeatureTokenEncoder<?>> encoder =
          outputFormat.getFeatureEncoder(transformationContext);

      if (outputFormat.supportsFeatureQuery() && encoder.isPresent()) {

        FeatureStream featureStream = featureProvider.queries().getFeatureStream(query);

        ResultReduced<byte[]> result =
            generateTile(
                featureStream, encoder.get(), transformationContext, outputFormat, featureTypeId);

        if (result.isSuccess()) {
          byte[] bytes = result.reduced();
          ByteArrayOutputStream buffer = new ByteArrayOutputStream(bytes.length);
          buffer.write(bytes, 0, bytes.length);
          byteArrayMap.put(collectionId, buffer);
        }
      } else {
        throw new NotAcceptableException(
            MessageFormat.format(
                "The requested media type {0} cannot be generated, because it does not support streaming.",
                requestContext.getMediaType().type()));
      }
    }

    TileFormatWithQuerySupportExtension.MultiLayerTileContent result;
    try {
      result =
          outputFormat.combineSingleLayerTilesToMultiLayerTile(
              tileMatrixSet, singleLayerTileMap, byteArrayMap);
    } catch (IOException e) {
      throw new RuntimeException("Error accessing the tile cache.", e);
    }

    // try to write/update tile in cache, if all collections have been processed
    if (result.isComplete) {
      try {
        tileCache.storeTile(multiLayerTile, result.byteArray);
      } catch (Throwable e) {
        String msg =
            "Failure to write the multi-layer file of tile {}/{}/{}/{} in dataset '{}', format '{}' to the cache";
        LogContext.errorAsInfo(
            LOGGER,
            e,
            msg,
            tileMatrixSet.getId(),
            tileLevel,
            tileRow,
            tileCol,
            api.getId(),
            outputFormat.getExtension());
      }
    }

    Date lastModified = null;
    EntityTag etag = ETag.from(result.byteArray);
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) return response.build();

    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader() ? links : null,
            HeaderCaching.of(lastModified, etag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format(
                    "%s_%d_%d_%d.%s",
                    tileMatrixSet.getId(),
                    tileLevel,
                    tileRow,
                    tileCol,
                    outputFormat.getMediaType().fileExtension())))
        .entity(result.byteArray)
        .build();*/
    return null;
  }

  private Response getTileStreamResponse(
      QueryInputTileStream queryInput, ApiRequestContext requestContext) {

    byte[] content;
    try {
      content = queryInput.getTileContent().readAllBytes();
    } catch (IOException e) {
      throw new RuntimeException("Could not read tile from cache.", e);
    }

    StreamingOutput streamingOutput =
        outputStream -> ByteStreams.copy(new ByteArrayInputStream(content), outputStream);

    List<Link> links =
        new DefaultLinksGenerator()
            .generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                i18n,
                requestContext.getLanguage());

    Date lastModified = queryInput.getLastModified().orElse(null);
    EntityTag etag = ETag.from(content);
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) return response.build();

    Tile tile = queryInput.getTile();
    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader() ? links : null,
            HeaderCaching.of(lastModified, etag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format(
                    "%s_%d_%d_%d.%s",
                    tile.getTileMatrixSet().getId(),
                    tile.getTileLevel(),
                    tile.getTileRow(),
                    tile.getTileCol(),
                    tile.getOutputFormat().getMediaType().fileExtension())))
        .entity(streamingOutput)
        .build();
  }

  private Response getMbtilesTileResponse(
      QueryInputTileMbtilesTile queryInput, ApiRequestContext requestContext) {

    String mbtilesFilename = queryInput.getProvider().getFilename();

    java.nio.file.Path provider =
        staticTileProviderStore.getTileProvider(queryInput.getTile().getApiData(), mbtilesFilename);

    if (!provider.toFile().exists())
      throw new RuntimeException(String.format("Mbtiles file '%s' does not exist", provider));

    StreamingOutput streamingOutput =
        outputStream ->
            ByteStreams.copy(
                staticTileProviderStore.getTile(provider, queryInput.getTile()), outputStream);

    List<Link> links =
        new DefaultLinksGenerator()
            .generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                ImmutableList.of(),
                i18n,
                requestContext.getLanguage());

    Date lastModified = LastModified.from(provider);
    EntityTag etag = ETag.from(staticTileProviderStore.getTile(provider, queryInput.getTile()));
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) return response.build();

    Tile tile = queryInput.getTile();
    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader() ? links : null,
            HeaderCaching.of(lastModified, etag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format(
                    "%s_%d_%d_%d.%s",
                    tile.getTileMatrixSet().getId(),
                    tile.getTileLevel(),
                    tile.getTileRow(),
                    tile.getTileCol(),
                    tile.getOutputFormat().getMediaType().fileExtension())))
        .entity(streamingOutput)
        .build();
  }

  private Response getTileServerTileResponse(
      QueryInputTileTileServerTile queryInput, ApiRequestContext requestContext) {

    Tile tile = queryInput.getTile();

    final String urlTemplate =
        tile.isDatasetTile()
            ? queryInput.getProvider().getUrlTemplate()
            : queryInput.getProvider().getUrlTemplateSingleCollection();

    if (Objects.isNull(urlTemplate))
      throw new IllegalStateException(
          "The MAP_TILES configuration is invalid, no 'urlTemplate' was found.");

    ApiMediaType mediaType = tile.getOutputFormat().getMediaType();
    WebTarget client =
        ClientBuilder.newClient()
            .target(urlTemplate)
            .resolveTemplate("tileMatrix", tile.getTileLevel())
            .resolveTemplate("tileRow", tile.getTileRow())
            .resolveTemplate("tileCol", tile.getTileCol())
            .resolveTemplate("fileExtension", mediaType.fileExtension());
    if (Objects.nonNull(tile.getCollectionId()))
      client = client.resolveTemplate("collectionId", tile.getCollectionId());
    Response response = client.request(mediaType.type()).get();

    // unsuccessful? just forward the error response
    if (response.getStatus() != 200) return response;

    List<Link> links =
        new DefaultLinksGenerator()
            .generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                ImmutableList.of(),
                i18n,
                requestContext.getLanguage());

    byte[] content;
    try {
      content = response.readEntity(InputStream.class).readAllBytes();
    } catch (IOException e) {
      throw new RuntimeException("Could not read map tile from TileServer.", e);
    }
    Date lastModified = null;
    EntityTag etag = ETag.from(content);
    Response.ResponseBuilder responseBuilder =
        evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(responseBuilder)) return responseBuilder.build();

    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader() ? links : null,
            HeaderCaching.of(lastModified, etag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format(
                    "%s_%d_%d_%d.%s",
                    tile.getTileMatrixSet().getId(),
                    tile.getTileLevel(),
                    tile.getTileRow(),
                    tile.getTileCol(),
                    tile.getOutputFormat().getMediaType().fileExtension())))
        .entity(content)
        .build();
  }

  private Response getEmptyTileResponse(
      QueryInputTileEmpty queryInput, ApiRequestContext requestContext) {

    List<Link> links =
        new DefaultLinksGenerator()
            .generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                i18n,
                requestContext.getLanguage());

    Tile tile = queryInput.getTile();

    if (!(tile.getOutputFormat() instanceof TileFormatWithQuerySupportExtension))
      throw new RuntimeException(
          String.format(
              "Unexpected tile format without query support. Found: %s",
              tile.getOutputFormat().getClass().getSimpleName()));
    TileFormatWithQuerySupportExtension outputFormat =
        (TileFormatWithQuerySupportExtension) tile.getOutputFormat();

    Date lastModified = null;
    EntityTag etag = ETag.from(tile.getOutputFormat().getEmptyTile(tile));
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) return response.build();

    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader() ? links : null,
            HeaderCaching.of(lastModified, etag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format(
                    "%s_%d_%d_%d.%s",
                    tile.getTileMatrixSet().getId(),
                    tile.getTileLevel(),
                    tile.getTileRow(),
                    tile.getTileCol(),
                    tile.getOutputFormat().getMediaType().fileExtension())))
        .entity(tile.getOutputFormat().getEmptyTile(tile))
        .build();
  }

  private TileMatrixSet getTileMatrixSetById(String tileMatrixSetId) {
    return tileMatrixSetRepository
        .get(tileMatrixSetId)
        .orElseThrow(
            () -> new ServerErrorException("TileMatrixSet not found: " + tileMatrixSetId, 500));
  }
}

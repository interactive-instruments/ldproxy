/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import static de.ii.ogcapi.tiles.app.TilesBuildingBlock.DATASET_TILES;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaObject;
import de.ii.ogcapi.features.core.domain.JsonSchemaCache;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HeaderCaching;
import de.ii.ogcapi.foundation.domain.HeaderContentDisposition;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimitsGenerator;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetOgcApi;
import de.ii.ogcapi.tiles.domain.ImmutableOwsOnlineResource;
import de.ii.ogcapi.tiles.domain.ImmutableOwsServiceIdentification;
import de.ii.ogcapi.tiles.domain.ImmutableOwsServiceProvider;
import de.ii.ogcapi.tiles.domain.ImmutableTileLayer;
import de.ii.ogcapi.tiles.domain.ImmutableTilePoint;
import de.ii.ogcapi.tiles.domain.ImmutableTileSet;
import de.ii.ogcapi.tiles.domain.ImmutableTileSets;
import de.ii.ogcapi.tiles.domain.ImmutableWmtsContents;
import de.ii.ogcapi.tiles.domain.ImmutableWmtsLayer;
import de.ii.ogcapi.tiles.domain.ImmutableWmtsResourceURL;
import de.ii.ogcapi.tiles.domain.ImmutableWmtsServiceMetadata;
import de.ii.ogcapi.tiles.domain.ImmutableWmtsStyle;
import de.ii.ogcapi.tiles.domain.ImmutableWmtsTileMatrix;
import de.ii.ogcapi.tiles.domain.ImmutableWmtsTileMatrixSet;
import de.ii.ogcapi.tiles.domain.ImmutableWmtsTileMatrixSetLink;
import de.ii.ogcapi.tiles.domain.ImmutableWmtsWGS84BoundingBox;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileGenerationUserParameter;
import de.ii.ogcapi.tiles.domain.TileSet;
import de.ii.ogcapi.tiles.domain.TileSet.DataType;
import de.ii.ogcapi.tiles.domain.TileSetFormatExtension;
import de.ii.ogcapi.tiles.domain.TileSets;
import de.ii.ogcapi.tiles.domain.TileSetsFormatExtension;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesConfiguration.WmtsScope;
import de.ii.ogcapi.tiles.domain.TilesProviders;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.ogcapi.tiles.domain.WmtsCapabilitiesFormatExtension;
import de.ii.ogcapi.tiles.domain.WmtsLayer;
import de.ii.ogcapi.tiles.domain.WmtsServiceMetadata;
import de.ii.xtraplatform.base.domain.ETag;
import de.ii.xtraplatform.base.domain.resiliency.AbstractVolatileComposed;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.base.domain.resiliency.VolatileUnavailableException;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.tiles.domain.ImmutableTileGenerationParametersTransient;
import de.ii.xtraplatform.tiles.domain.ImmutableTileQuery;
import de.ii.xtraplatform.tiles.domain.ImmutableTilesBoundingBox;
import de.ii.xtraplatform.tiles.domain.MinMax;
import de.ii.xtraplatform.tiles.domain.TileAccess;
import de.ii.xtraplatform.tiles.domain.TileGenerationParametersTransient;
import de.ii.xtraplatform.tiles.domain.TileGenerationSchema;
import de.ii.xtraplatform.tiles.domain.TileMatrixSet;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetRepository;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import de.ii.xtraplatform.tiles.domain.TileQuery;
import de.ii.xtraplatform.tiles.domain.TileResult;
import de.ii.xtraplatform.tiles.domain.TilesFormat;
import de.ii.xtraplatform.tiles.domain.TilesetMetadata;
import de.ii.xtraplatform.tiles.domain.WithCenter.LonLat;
import de.ii.xtraplatform.values.domain.ValueStore;
import de.ii.xtraplatform.values.domain.Values;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@AutoBind
public class TilesQueriesHandlerImpl extends AbstractVolatileComposed
    implements TilesQueriesHandler {

  private final I18n i18n;
  private final CrsTransformerFactory crsTransformerFactory;
  private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
  private final Values<Codelist> codelistStore;
  private final ExtensionRegistry extensionRegistry;
  private final TileMatrixSetLimitsGenerator limitsGenerator;
  private final TilesProviders tilesProviders;
  private final TileMatrixSetRepository tileMatrixSetRepository;

  @Inject
  public TilesQueriesHandlerImpl(
      I18n i18n,
      CrsTransformerFactory crsTransformerFactory,
      ValueStore valueStore,
      ExtensionRegistry extensionRegistry,
      TileMatrixSetLimitsGenerator limitsGenerator,
      TilesProviders tilesProviders,
      TileMatrixSetRepository tileMatrixSetRepository,
      VolatileRegistry volatileRegistry) {
    super(TilesQueriesHandler.class.getSimpleName(), volatileRegistry, true);
    this.i18n = i18n;
    this.crsTransformerFactory = crsTransformerFactory;
    this.codelistStore = valueStore.forType(Codelist.class);
    this.extensionRegistry = extensionRegistry;
    this.limitsGenerator = limitsGenerator;
    this.tilesProviders = tilesProviders;
    this.tileMatrixSetRepository = tileMatrixSetRepository;

    this.queryHandlers =
        ImmutableMap.<Query, QueryHandler<? extends QueryInput>>builder()
            .put(
                Query.TILE_SETS,
                QueryHandler.with(QueryInputTileSets.class, this::getTileSetsResponse))
            .put(
                Query.TILE_SET,
                QueryHandler.with(QueryInputTileSet.class, this::getTileSetResponse))
            .put(Query.TILE, QueryHandler.with(QueryInputTile.class, this::getTileResponse))
            .put(Query.WMTS, QueryHandler.with(QueryInputWmts.class, this::getWmtsCapabilities))
            .build();

    onVolatileStart();

    addSubcomponent(crsTransformerFactory);
    addSubcomponent(codelistStore);
    addSubcomponent(tileMatrixSetRepository);

    onVolatileStarted();
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
    boolean onlyWebMercatorQuad = queryInput.getOnlyWebMercatorQuad();

    TileSetsFormatExtension outputFormat =
        api.getOutputFormat(
                TileSetsFormatExtension.class, requestContext.getMediaType(), collectionId)
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported for this resource.",
                            requestContext.getMediaType())));

    final TilesLinkGenerator tilesLinkGenerator = new TilesLinkGenerator();

    Optional<FeatureTypeConfigurationOgcApi> featureType =
        collectionId.map(s -> apiData.getCollections().get(s));
    List<String> tileMatrixSetIds = queryInput.getTileMatrixSetIds();

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
                    format ->
                        queryInput
                            .getTileEncodings()
                            .indexOf(TilesFormat.of(format.getMediaType().label()))))
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

    ImmutableTileSets.Builder builder =
        ImmutableTileSets.builder()
            .title(featureType.isPresent() ? featureType.get().getLabel() : apiData.getLabel())
            .description(
                featureType
                    .map(ft -> ft.getDescription().orElse(""))
                    .orElseGet(() -> apiData.getDescription().orElse("")))
            .links(links);

    List<TileMatrixSet> tileMatrixSets =
        tileMatrixSetIds.stream()
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
                            buildTileSet(
                                api,
                                tileMatrixSet,
                                collectionId,
                                type,
                                tilesLinkGenerator.generateTileSetEmbeddedLinks(
                                    requestContext.getUriCustomizer(),
                                    tileMatrixSet.getId(),
                                    collectionId,
                                    tileFormats,
                                    i18n,
                                    requestContext.getLanguage())))
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
        .entity(
            outputFormat.getTileSetsEntity(
                tileSets,
                dataType.orElse(
                    queryInput.getPath().contains("/map/") ? DataType.map : DataType.vector),
                collectionId,
                api,
                requestContext))
        .build();
  }

  private Response getTileSetResponse(
      QueryInputTileSet queryInput, ApiRequestContext requestContext) {
    OgcApi api = requestContext.getApi();
    OgcApiDataV2 apiData = api.getData();
    String tileMatrixSetId = queryInput.getTileMatrixSetId();
    Optional<String> collectionId = queryInput.getCollectionId();
    String definitionPath = queryInput.getPath();

    TileSetFormatExtension outputFormat =
        api.getOutputFormat(
                TileSetFormatExtension.class, requestContext.getMediaType(), collectionId)
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
            tileMatrixSetId,
            collectionId,
            tileFormats,
            i18n,
            requestContext.getLanguage());

    TileSet tileset =
        buildTileSet(api, getTileMatrixSetById(tileMatrixSetId), collectionId, dataType, links);

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
                    "tileset.%s.%s",
                    tileset.getTileMatrixSetId(), outputFormat.getMediaType().fileExtension())))
        .entity(outputFormat.getTileSetEntity(tileset, apiData, collectionId, requestContext))
        .build();
  }

  private Response getTileResponse(QueryInputTile queryInput, ApiRequestContext requestContext) {
    TileProvider tileProvider =
        tilesProviders.getTileProviderOrThrow(
            requestContext.getApi().getData(),
            queryInput
                .getCollectionId()
                .flatMap(id -> requestContext.getApi().getData().getCollectionData(id)));

    if (!tileProvider.access().isAvailable()) {
      throw new VolatileUnavailableException("Tile provider not available");
    }

    TileAccess tileAccess = tileProvider.access().get();

    TileQuery tileQuery = getTileQuery(queryInput, requestContext, tileProvider);

    TileResult result = tileAccess.getTile(tileQuery);

    if (!result.isAvailable()) {
      if (result.isOutsideLimits() || tileAccess.tilesMayBeUnavailable()) {
        throw result.getError().map(NotFoundException::new).orElseGet(NotFoundException::new);
      } else {
        throw result
            .getError()
            .map(IllegalStateException::new)
            .orElseThrow(IllegalStateException::new);
      }
    }

    byte[] content = result.getContent().get();
    EntityTag eTag = ETag.from(content);
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, null, eTag);

    if (Objects.nonNull(response)) {
      // TODO add support for empty/full for Features and MBTiles caches
      if (result.isEmpty()) {
        response.header("OATiles-hint", "empty");
      } else if (result.isFull()) {
        response.header("OATiles-hint", "full");
      }
      return response.build();
    }

    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader() ? getLinks(requestContext, i18n) : ImmutableList.of(),
            HeaderCaching.of(null, eTag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format(
                    "%s_%s_%d_%d_%d.%s",
                    tileQuery.getTileset(),
                    tileQuery.getTileMatrixSet().getId(),
                    tileQuery.getLevel(),
                    tileQuery.getRow(),
                    tileQuery.getCol(),
                    queryInput.getOutputFormat().getMediaType().fileExtension())))
        .entity(result.getContent().get())
        .build();
  }

  private Response getWmtsCapabilities(
      QueryInputWmts queryInput, ApiRequestContext requestContext) {
    OgcApi api = requestContext.getApi();
    OgcApiDataV2 apiData = api.getData();

    WmtsCapabilitiesFormatExtension outputFormat =
        api.getOutputFormat(
                WmtsCapabilitiesFormatExtension.class,
                requestContext.getMediaType(),
                Optional.empty())
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported for this resource.",
                            requestContext.getMediaType())));

    Optional<ApiMetadata> md = apiData.getMetadata();
    Set<String> tmsIds = new HashSet<>();

    ImmutableWmtsServiceMetadata.Builder builder =
        ImmutableWmtsServiceMetadata.builder()
            .serviceMetadataURL(
                ImmutableOwsOnlineResource.builder()
                    .href(String.format("%s%s", api.getUri(), "/wmts/1.0.0/WMTSCapabilities.xml"))
                    .build())
            .serviceIdentification(
                ImmutableOwsServiceIdentification.builder()
                    .title(apiData.getLabel())
                    .getAbstract(apiData.getDescription())
                    .addAllKeywords(md.map(ApiMetadata::getKeywords).orElse(ImmutableList.of()))
                    .accessConstraints(
                        md.flatMap(ApiMetadata::getLicenseName)
                            .map(s -> String.format("License: %s", s)))
                    .build());
    md.flatMap(ApiMetadata::getPublisherName)
        .ifPresent(
            publisher -> {
              ImmutableOwsServiceProvider.Builder providerBuilder =
                  ImmutableOwsServiceProvider.builder().providerName(publisher);
              md.flatMap(ApiMetadata::getPublisherUrl)
                  .ifPresent(
                      url ->
                          providerBuilder.providerSite(
                              ImmutableOwsOnlineResource.builder().href(url).build()));
              builder.serviceProvider(providerBuilder.build());
            });

    ImmutableWmtsContents.Builder contentsBuilder = ImmutableWmtsContents.builder();

    WmtsScope scope = queryInput.getScope();
    if (scope != WmtsScope.COLLECTIONS
        && scope != WmtsScope.COLLECTIONS_MAP
        && scope != WmtsScope.COLLECTIONS_VECTOR) {
      tilesProviders
          .getTilesetMetadata(apiData)
          .ifPresent(
              tilesetMetadata -> {
                String title = apiData.getLabel();
                Optional<String> abstract_ = apiData.getDescription();
                String identifier = apiData.getId();
                getLayer(
                        tilesetMetadata,
                        scope,
                        title,
                        abstract_,
                        identifier,
                        api,
                        Optional.empty(),
                        tmsIds)
                    .ifPresent(contentsBuilder::addLayers);
              });
    }

    if (scope != WmtsScope.DATASET
        && scope != WmtsScope.DATASET_MAP
        && scope != WmtsScope.DATASET_VECTOR) {
      apiData
          .getCollections()
          .values()
          .forEach(
              collectionData -> {
                tilesProviders
                    .getTilesetMetadata(apiData, collectionData)
                    .ifPresent(
                        tilesetMetadata -> {
                          String title = collectionData.getLabel();
                          Optional<String> abstract_ = collectionData.getDescription();
                          String identifier = collectionData.getId();
                          getLayer(
                                  tilesetMetadata,
                                  scope,
                                  title,
                                  abstract_,
                                  identifier,
                                  api,
                                  Optional.of(collectionData),
                                  tmsIds)
                              .ifPresent(contentsBuilder::addLayers);
                        });
              });
    }

    tmsIds.stream()
        .sorted()
        .forEach(
            tmsId -> {
              TileMatrixSet tmsObject = getTileMatrixSetById(tmsId);
              ImmutableWmtsTileMatrixSet.Builder tmsBuilder =
                  ImmutableWmtsTileMatrixSet.builder()
                      .identifier(tmsId)
                      .supportedCRS(tmsObject.getCrs().toUriString());
              tmsObject
                  .getWellKnownScaleSet()
                  .ifPresent(wkss -> tmsBuilder.wellKnownScaleSet(wkss.toString()));
              tmsObject
                  .getTileMatrices(tmsObject.getMinLevel(), tmsObject.getMaxLevel())
                  .forEach(
                      tm ->
                          tmsBuilder.addTileMatrix(
                              ImmutableWmtsTileMatrix.builder()
                                  .identifier(tm.getId())
                                  .scaleDenominator(tm.getScaleDenominator().doubleValue())
                                  .topLeftCornerValues(tm.getPointOfOrigin())
                                  .tileWidth(tm.getTileWidth())
                                  .tileHeight(tm.getTileHeight())
                                  .matrixWidth(tm.getMatrixWidth())
                                  .matrixHeight(tm.getMatrixHeight())
                                  .build()));

              contentsBuilder.addTileMatrixSets(tmsBuilder.build());
            });

    builder.contents(contentsBuilder.build());

    WmtsServiceMetadata capabilities = builder.build();

    Date lastModified = getLastModified(queryInput);
    EntityTag etag =
        ETag.from(capabilities, WmtsServiceMetadata.FUNNEL, outputFormat.getMediaType().label());
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) return response.build();

    return prepareSuccessResponse(
            requestContext,
            null,
            HeaderCaching.of(lastModified, etag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format("WMTSCapabilities.%s", outputFormat.getMediaType().fileExtension())))
        .entity(outputFormat.getEntity(capabilities, api, requestContext))
        .build();
  }

  private Optional<WmtsLayer> getLayer(
      TilesetMetadata tilesetMetadata,
      WmtsScope scope,
      String title,
      Optional<String> abstract_,
      String identifier,
      OgcApi api,
      Optional<FeatureTypeConfigurationOgcApi> collectionData,
      Set<String> tmsIds) {
    Set<TilesFormat> formats =
        tilesetMetadata.getEncodings().stream()
            .filter(
                f ->
                    scope == WmtsScope.ALL
                        || (scope == WmtsScope.DATASET_VECTOR
                            && collectionData.isEmpty()
                            && f.isVector())
                        || (scope == WmtsScope.COLLECTIONS_VECTOR
                            && collectionData.isPresent()
                            && f.isVector())
                        || (scope == WmtsScope.VECTOR && f.isVector())
                        || (scope == WmtsScope.DATASET_MAP
                            && collectionData.isEmpty()
                            && f.isRaster())
                        || (scope == WmtsScope.COLLECTIONS_MAP
                            && collectionData.isPresent()
                            && f.isRaster())
                        || (scope == WmtsScope.MAP && f.isRaster())
                        || (scope == WmtsScope.DATASET && collectionData.isEmpty())
                        || (scope == WmtsScope.COLLECTIONS && collectionData.isPresent()))
            .collect(Collectors.toSet());

    if (formats.isEmpty()) {
      return Optional.empty();
    }

    ImmutableWmtsLayer.Builder layerBuilder =
        ImmutableWmtsLayer.builder()
            .title(title)
            .getAbstract(abstract_)
            .identifier(identifier)
            // TODO support styles
            .addStyle(ImmutableWmtsStyle.builder().identifier("default").build());
    api.getSpatialExtent(collectionData.map(FeatureTypeConfigurationOgcApi::getId))
        .ifPresent(
            bbox ->
                layerBuilder.wGS84BoundingBox(
                    ImmutableWmtsWGS84BoundingBox.builder()
                        .addLowerCornerValues(bbox.getXmin(), bbox.getYmin())
                        .addUpperCornerValues(bbox.getXmax(), bbox.getYmax())
                        .build()));

    formats.stream()
        .sorted(Comparator.comparing(TilesFormat::name))
        .forEach(
            f -> {
              layerBuilder.addFormats(f.asMediaType().toString());
              String template =
                  String.format(
                      "%s%s%s/tiles/{TileMatrixSet}/{TileMatrix}/{TileRow}/{TileCol}?f=%s",
                      api.getUri(),
                      collectionData
                          .map(cd -> String.format("/collections/%s", cd.getId()))
                          .orElse(""),
                      f == TilesFormat.MVT ? "" : "/map",
                      f.asFString());
              layerBuilder.addResourceURL(
                  ImmutableWmtsResourceURL.builder()
                      .format(f.asMediaType().toString())
                      .template(template)
                      .resourceType("tile")
                      .build());
            });

    Set<String> tms = tilesetMetadata.getTileMatrixSets();
    Map<String, Range<Integer>> tmsRanges = tilesetMetadata.getTmsRanges();
    tms.forEach(
        tmsId -> {
          TileMatrixSet tmsObject = getTileMatrixSetById(tmsId);
          ImmutableWmtsTileMatrixSetLink.Builder tmsLinkBuilder =
              ImmutableWmtsTileMatrixSetLink.builder().tileMatrixSet(tmsId);
          Range<Integer> range = tmsRanges.get(tmsId);
          IntStream.range(range.lowerEndpoint(), range.upperEndpoint())
              .forEachOrdered(
                  i ->
                      tmsLinkBuilder.addTileMatrixSetLimits(
                          limitsGenerator.getTileMatrixSetLimits(
                              api, tmsObject, i, Optional.empty())));
          layerBuilder.addTileMatrixSetLink(tmsLinkBuilder.build());
          tmsIds.add(tmsId);
        });

    return Optional.of(layerBuilder.build());
  }

  private TileQuery getTileQuery(
      QueryInputTile queryInput, ApiRequestContext requestContext, TileProvider tileProvider) {
    OgcApiDataV2 apiData = requestContext.getApi().getData();
    Optional<FeatureTypeConfigurationOgcApi> collectionData =
        queryInput.getCollectionId().flatMap(apiData::getCollectionData);
    TileFormatExtension outputFormat = queryInput.getOutputFormat();
    String tileset =
        collectionData.isPresent()
            ? collectionData
                .flatMap(cd -> cd.getExtension(TilesConfiguration.class))
                .flatMap(cfg -> Optional.ofNullable(cfg.getTileProviderTileset()))
                .orElse(collectionData.get().getId())
            : apiData
                .getExtension(TilesConfiguration.class)
                .flatMap(cfg -> Optional.ofNullable(cfg.getTileProviderTileset()))
                .orElse(DATASET_TILES);

    ImmutableTileQuery.Builder tileQueryBuilder =
        ImmutableTileQuery.builder()
            .from(queryInput)
            .tileset(tileset)
            .mediaType(outputFormat.getMediaType().type());

    tileQueryBuilder
        .generationParametersBuilder()
        .clipBoundingBox(
            requestContext
                .getApi()
                .getSpatialExtent(
                    queryInput.getCollectionId(), queryInput.getBoundingBox().getEpsgCrs()))
        .propertyTransformations(
            collectionData
                .flatMap(cd -> cd.getExtension(FeaturesCoreConfiguration.class))
                .map(
                    pt ->
                        pt.withSubstitutions(
                            FeaturesCoreProviders.DEFAULT_SUBSTITUTIONS.apply(
                                requestContext.getApiUri()))));

    Optional<TileGenerationSchema> generationSchema =
        tileProvider.generator().isAvailable()
            ? Optional.of(tileProvider.generator().get().getGenerationSchema(tileset))
            : Optional.empty();
    ImmutableTileGenerationParametersTransient.Builder userParametersBuilder =
        new ImmutableTileGenerationParametersTransient.Builder();
    queryInput
        .getParameters()
        .forEach(
            TileGenerationUserParameter.class,
            parameter ->
                parameter.applyTo(
                    userParametersBuilder, queryInput.getParameters(), generationSchema));
    TileGenerationParametersTransient userParameters = userParametersBuilder.build();
    if (!userParameters.isEmpty()) {
      tileQueryBuilder.generationParametersTransient(userParameters);
    }
    return tileQueryBuilder.build();
  }

  private TileMatrixSet getTileMatrixSetById(String tileMatrixSetId) {
    return tileMatrixSetRepository
        .get(tileMatrixSetId)
        .orElseThrow(
            () -> new ServerErrorException("TileMatrixSet not found: " + tileMatrixSetId, 500));
  }

  /**
   * generate the tile set metadata according to the OGC Tile Matrix Set standard (version 2.0.0,
   * draft from June 2021)
   *
   * @param api the API
   * @param tileMatrixSet the tile matrix set
   * @param collectionId the collection, empty = all collections in the dataset
   * @param dataType vector, map or coverage
   * @param links links to include in the object
   * @return the tile set metadata
   */
  public TileSet buildTileSet(
      OgcApi api,
      TileMatrixSet tileMatrixSet,
      Optional<String> collectionId,
      TileSet.DataType dataType,
      List<Link> links) {
    OgcApiDataV2 apiData = api.getData();
    Optional<TilesetMetadata> tilesetMetadata =
        tilesProviders.getTilesetMetadata(
            apiData, collectionId.flatMap(apiData::getCollectionData));
    Optional<MinMax> levels =
        tilesetMetadata.flatMap(
            metadata -> Optional.ofNullable(metadata.getLevels().get(tileMatrixSet.getId())));
    Optional<LonLat> center = tilesetMetadata.flatMap(TilesetMetadata::getCenter);

    ImmutableTileSet.Builder builder = ImmutableTileSet.builder().dataType(dataType);

    builder.tileMatrixSetId(tileMatrixSet.getId());

    if (tileMatrixSet.getURI().isPresent())
      builder.tileMatrixSetURI(tileMatrixSet.getURI().get().toString());
    else builder.tileMatrixSet(TileMatrixSetOgcApi.of(tileMatrixSet.getTileMatrixSetData()));

    if (levels.isEmpty()) {
      builder.tileMatrixSetLimits(ImmutableList.of());
    } else
      builder.tileMatrixSetLimits(
          limitsGenerator.getTileMatrixSetLimits(api, tileMatrixSet, levels.get(), collectionId));

    BoundingBox boundingBox = null;
    try {
      boundingBox =
          tilesetMetadata
              .flatMap(TilesetMetadata::getBounds)
              .orElse(
                  api.getSpatialExtent(collectionId)
                      .orElse(tileMatrixSet.getBoundingBoxCrs84(crsTransformerFactory)));
      builder.boundingBox(
          new ImmutableTilesBoundingBox.Builder()
              .lowerLeft(
                  BigDecimal.valueOf(boundingBox.getXmin()).setScale(7, RoundingMode.HALF_UP),
                  BigDecimal.valueOf(boundingBox.getYmin()).setScale(7, RoundingMode.HALF_UP))
              .upperRight(
                  BigDecimal.valueOf(boundingBox.getXmax()).setScale(7, RoundingMode.HALF_UP),
                  BigDecimal.valueOf(boundingBox.getYmax()).setScale(7, RoundingMode.HALF_UP))
              .crs(OgcCrs.CRS84.toUriString())
              .build());
    } catch (CrsTransformationException e) {
      builder.boundingBox(
          new ImmutableTilesBoundingBox.Builder()
              .lowerLeft(BigDecimal.valueOf(-180), BigDecimal.valueOf(-90))
              .upperRight(BigDecimal.valueOf(180), BigDecimal.valueOf(90))
              .crs(OgcCrs.CRS84.toUriString())
              .build());
    }

    if (levels.flatMap(MinMax::getDefault).isPresent() || center.isPresent()) {
      ImmutableTilePoint.Builder builder2 = new ImmutableTilePoint.Builder();
      if (levels.isPresent()) {
        levels
            .flatMap(MinMax::getDefault)
            .ifPresent(def -> builder2.tileMatrix(String.valueOf(def)));
      }
      BoundingBox finalBoundingBox = boundingBox;
      center.ifPresentOrElse(
          lonLat -> builder2.coordinates(lonLat.asList()),
          () -> {
            if (Objects.nonNull(finalBoundingBox)) {
              builder2.coordinates(
                  ImmutableList.of(
                      (finalBoundingBox.getXmax() + finalBoundingBox.getXmin()) / 2.0,
                      (finalBoundingBox.getYmax() + finalBoundingBox.getYmin()) / 2.0));
            }
          });
      builder.centerPoint(builder2.build());
    }

    if (tilesetMetadata.isPresent()) {
      JsonSchemaCache schemaCache = new SchemaCacheTileSet(codelistStore::asMap);

      List<FeatureSchema> vectorSchemas = tilesetMetadata.get().getVectorSchemas();

      vectorSchemas.forEach(
          vectorSchema -> {
            FeatureTypeConfigurationOgcApi collectionData =
                collectionId
                    .flatMap(apiData::getCollectionData)
                    .orElseGet(
                        () ->
                            TilesConfiguration.getCollectionData(apiData, vectorSchema.getName())
                                .orElse(
                                    new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                        .id(vectorSchema.getName())
                                        .label(
                                            vectorSchema.getLabel().orElse(vectorSchema.getName()))
                                        .description(vectorSchema.getDescription())
                                        .build()));

            JsonSchemaDocument jsonSchema =
                schemaCache.getSchema(vectorSchema, apiData, collectionData, Optional.empty());

            ImmutableTileLayer.Builder builder2 =
                ImmutableTileLayer.builder()
                    .id(collectionData.getId())
                    .title(collectionData.getLabel())
                    .description(collectionData.getDescription())
                    .dataType(dataType);

            if (levels.isPresent()) {
              builder2
                  .minTileMatrix(String.valueOf(levels.get().getMin()))
                  .maxTileMatrix(String.valueOf(levels.get().getMax()));
            }

            switch (vectorSchema
                .getPrimaryGeometry()
                .flatMap(FeatureSchema::getGeometryType)
                .orElse(SimpleFeatureGeometry.ANY)) {
              case POINT:
              case MULTI_POINT:
                builder2.geometryDimension(0);
                break;
              case LINE_STRING:
              case MULTI_LINE_STRING:
                builder2.geometryDimension(1);
                break;
              case POLYGON:
              case MULTI_POLYGON:
                builder2.geometryDimension(2);
                break;
            }

            builder2.propertiesSchema(
                new ImmutableJsonSchemaObject.Builder()
                    .required(jsonSchema.getRequired())
                    .properties(jsonSchema.getProperties())
                    .patternProperties(jsonSchema.getPatternProperties())
                    .build());

            builder.addLayers(builder2.build());
          });
    }

    builder.links(links);

    return builder.build();
  }
}

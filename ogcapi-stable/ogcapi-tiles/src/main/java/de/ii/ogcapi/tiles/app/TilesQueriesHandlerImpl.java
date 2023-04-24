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
import de.ii.ogcapi.collections.queryables.domain.QueryablesConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
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
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimitsGenerator;
import de.ii.ogcapi.tiles.domain.ImmutableTileSets;
import de.ii.ogcapi.tiles.domain.ImmutableTileSets.Builder;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileGenerationUserParameter;
import de.ii.ogcapi.tiles.domain.TileSet;
import de.ii.ogcapi.tiles.domain.TileSet.DataType;
import de.ii.ogcapi.tiles.domain.TileSetFormatExtension;
import de.ii.ogcapi.tiles.domain.TileSets;
import de.ii.ogcapi.tiles.domain.TileSetsFormatExtension;
import de.ii.ogcapi.tiles.domain.TilesProviders;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.tiles.domain.ImmutableTileGenerationParametersTransient;
import de.ii.xtraplatform.tiles.domain.ImmutableTileQuery;
import de.ii.xtraplatform.tiles.domain.MinMax;
import de.ii.xtraplatform.tiles.domain.TileGenerationParametersTransient;
import de.ii.xtraplatform.tiles.domain.TileGenerationSchema;
import de.ii.xtraplatform.tiles.domain.TileMatrixSet;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetRepository;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import de.ii.xtraplatform.tiles.domain.TileQuery;
import de.ii.xtraplatform.tiles.domain.TileResult;
import de.ii.xtraplatform.web.domain.ETag;
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
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
  private final FeaturesCoreProviders providers;
  private final TilesProviders tilesProviders;
  private final TileMatrixSetRepository tileMatrixSetRepository;
  // TODO
  private final FeaturesQuery featuresQuery;

  @Inject
  public TilesQueriesHandlerImpl(
      I18n i18n,
      CrsTransformerFactory crsTransformerFactory,
      EntityRegistry entityRegistry,
      ExtensionRegistry extensionRegistry,
      TileMatrixSetLimitsGenerator limitsGenerator,
      FeaturesCoreProviders providers,
      TilesProviders tilesProviders,
      TileMatrixSetRepository tileMatrixSetRepository,
      FeaturesQuery featuresQuery) {
    this.i18n = i18n;
    this.crsTransformerFactory = crsTransformerFactory;
    this.entityRegistry = entityRegistry;
    this.extensionRegistry = extensionRegistry;
    this.limitsGenerator = limitsGenerator;
    this.providers = providers;
    this.tilesProviders = tilesProviders;
    this.tileMatrixSetRepository = tileMatrixSetRepository;
    this.featuresQuery = featuresQuery;

    this.queryHandlers =
        ImmutableMap.<Query, QueryHandler<? extends QueryInput>>builder()
            .put(
                Query.TILE_SETS,
                QueryHandler.with(QueryInputTileSets.class, this::getTileSetsResponse))
            .put(
                Query.TILE_SET,
                QueryHandler.with(QueryInputTileSet.class, this::getTileSetResponse))
            .put(Query.TILE, QueryHandler.with(QueryInputTile.class, this::getTileResponse))
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
                                    collectionId,
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

  private Response getTileResponse(QueryInputTile queryInput, ApiRequestContext requestContext) {
    TileProvider tileProvider =
        tilesProviders.getTileProviderOrThrow(
            requestContext.getApi().getData(),
            queryInput
                .getCollectionId()
                .flatMap(id -> requestContext.getApi().getData().getCollectionData(id)));

    TileQuery tileQuery = getTileQuery(queryInput, requestContext, tileProvider);

    TileResult result = tileProvider.getTile(tileQuery);

    if (!result.isAvailable()) {
      if (result.isOutsideLimits()) {
        throw result.getError().map(NotFoundException::new).orElseGet(NotFoundException::new);
      } else {
        throw result
            .getError()
            .map(IllegalStateException::new)
            .orElseGet(IllegalStateException::new);
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
                    tileQuery.getLayer(),
                    tileQuery.getTileMatrixSet().getId(),
                    tileQuery.getLevel(),
                    tileQuery.getRow(),
                    tileQuery.getCol(),
                    queryInput.getOutputFormat().getMediaType().fileExtension())))
        .entity(result.getContent().get())
        .build();
  }

  private TileQuery getTileQuery(
      QueryInputTile queryInput, ApiRequestContext requestContext, TileProvider tileProvider) {
    OgcApiDataV2 apiData = requestContext.getApi().getData();
    Optional<FeatureTypeConfigurationOgcApi> collectionData =
        queryInput.getCollectionId().flatMap(apiData::getCollectionData);
    // TODO: get layer name from cfg
    String layer = queryInput.getCollectionId().orElse(DATASET_TILES);
    TileFormatExtension outputFormat = queryInput.getOutputFormat();

    ImmutableTileQuery.Builder tileQueryBuilder =
        ImmutableTileQuery.builder()
            .from(queryInput)
            .layer(layer)
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

    // TODO: TilesProviders along the line of FeaturesCoreProviders
    Map<String, FeatureSchema> queryables =
        collectionData
            .flatMap(cd -> cd.getExtension(QueryablesConfiguration.class))
            .map(cfg -> cfg.getQueryables(apiData, collectionData.get(), providers))
            .orElse(ImmutableMap.of());
    Optional<TileGenerationSchema> generationSchema =
        tileProvider.supportsGeneration()
            ? Optional.of(tileProvider.generator().getGenerationSchema(layer, queryables))
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
}

/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSetLimits;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Handle responses under '/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}'.
 */
@Component
@Provides
@Instantiate
public class EndpointTileSingleCollection extends EndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointTileSingleCollection.class);

    private static final List<String> TAGS = ImmutableList.of("Access single-layer tiles");

    private final FeaturesCoreProviders providers;
    private final TilesQueriesHandler queryHandler;
    private final CrsTransformerFactory crsTransformerFactory;
    private final TileMatrixSetLimitsGenerator limitsGenerator;
    private final TilesCache cache;

    EndpointTileSingleCollection(@Requires FeaturesCoreProviders providers,
                                 @Requires ExtensionRegistry extensionRegistry,
                                 @Requires TilesQueriesHandler queryHandler,
                                 @Requires CrsTransformerFactory crsTransformerFactory,
                                 @Requires TileMatrixSetLimitsGenerator limitsGenerator, 
                                 @Requires TilesCache cache) {
        super(extensionRegistry);
        this.providers = providers;
        this.queryHandler = queryHandler;
        this.crsTransformerFactory = crsTransformerFactory;
        this.limitsGenerator = limitsGenerator;
        this.cache = cache;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(TileFormatExtension.class);
        return formats;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        // currently no vector tiles support for WFS backends
        if (providers.getFeatureProvider(apiData).getData().getFeatureProviderType().equals("WFS"))
            return false;

        Optional<TilesConfiguration> extension = apiData.getExtension(TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::isEnabled)
                .filter(TilesConfiguration::getSingleCollectionEnabled)
                .isPresent();
    }

    @Override
    public ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                    .apiEntrypoint("collections")
                    .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_TILE_COLLECTION);
            final String subSubPath = "/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}";
            final String path = "/collections/{collectionId}" + subSubPath;
            final HttpMethods method = HttpMethods.GET;
            final List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            final Optional<OgcApiPathParameter> optCollectionIdParam = pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
            if (!optCollectionIdParam.isPresent()) {
                LOGGER.error("Path parameter 'collectionId' missing for resource at path '" + path + "'. The GET method will not be available.");
            } else {
                final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
                boolean explode = collectionIdParam.getExplodeInOpenApi();
                final Set<String> collectionIds = (explode) ?
                        collectionIdParam.getValues(apiData) :
                        ImmutableSet.of("{collectionId}");
                for (String collectionId : collectionIds) {
                    if (explode)
                        queryParameters = getQueryParameters(extensionRegistry, apiData, path, collectionId);
                    String operationSummary = "fetch a tile of the collection '"+collectionId+"'";
                    Optional<String> operationDescription = Optional.of("The tile in the requested tiling scheme ('{tileMatrixSetId}'), " +
                            "on the requested zoom level ('{tileMatrix}'), with the requested grid coordinates ('{tileRow}', '{tileCol}') is returned. " +
                            "The tile has a single layer with all selected features in the bounding box of the tile with the requested properties.");
                    String resourcePath = path.replace("{collectionId}", collectionId);
                    ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
                            .path(resourcePath)
                            .pathParameters(pathParameters);
                    ApiOperation operation = addOperation(apiData, HttpMethods.GET, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, TAGS);
                    if (operation != null)
                        resourceBuilder.putOperations(method.name(), operation);
                    definitionBuilder.putResources(resourcePath, resourceBuilder.build());
                }
            }

            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
    }

    @Path("/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
    @GET
    public Response getTile(@Auth Optional< User > optionalUser, @Context OgcApi api, @PathParam("collectionId") String collectionId,
                            @PathParam("tileMatrixSetId") String tileMatrixSetId, @PathParam("tileMatrix") String tileMatrix,
                            @PathParam("tileRow") String tileRow, @PathParam("tileCol") String tileCol,
                            @Context UriInfo uriInfo, @Context ApiRequestContext requestContext)
            throws CrsTransformationException, IOException, NotFoundException {

        OgcApiDataV2 apiData = api.getData();
        checkAuthorization(apiData, optionalUser);
        FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData);
        ensureFeatureProviderSupportsQueries(featureProvider);

        String definitionPath = "/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}";
        checkPathParameter(extensionRegistry, apiData, definitionPath, "collectionId", collectionId);
        checkPathParameter(extensionRegistry, apiData, definitionPath, "tileMatrixSetId", tileMatrixSetId);
        checkPathParameter(extensionRegistry, apiData, definitionPath, "tileMatrix", tileMatrix);
        checkPathParameter(extensionRegistry, apiData, definitionPath, "tileRow", tileRow);
        checkPathParameter(extensionRegistry, apiData, definitionPath, "tileCol", tileCol);
        final List<OgcApiQueryParameter> allowedParameters = getQueryParameters(extensionRegistry, api.getData(), definitionPath, collectionId);

        // check, if the cache can be used (no query parameters except f)
        Map<String, String> queryParams = toFlatMap(uriInfo.getQueryParameters());
        boolean useCache = queryParams.isEmpty() || (queryParams.size()==1 && queryParams.containsKey("f"));

        int row;
        int col;
        int level;
        try {
            level = Integer.parseInt(tileMatrix);
            row = Integer.parseInt(tileRow);
            col = Integer.parseInt(tileCol);
        } catch (NumberFormatException e) {
            throw new ServerErrorException("Could not convert tile coordinates that have been validated to integers", 500);
        }

        FeatureTypeConfigurationOgcApi featureType = requestContext.getApi().getData().getCollections().get(collectionId);
        TilesConfiguration tilesConfiguration = featureType.getExtension(TilesConfiguration.class).get();

        MinMax zoomLevels = tilesConfiguration.getZoomLevels().get(tileMatrixSetId);
        if (zoomLevels.getMax() < level || zoomLevels.getMin() > level)
            throw new NotFoundException("The requested tile is outside the zoom levels for this tile set.");

        TileMatrixSet tileMatrixSet = extensionRegistry.getExtensionsForType(TileMatrixSet.class).stream()
                .filter(tms -> tms.getId().equals(tileMatrixSetId))
                .findAny()
                .orElseThrow(() -> new NotFoundException("Unknown tile matrix set: " + tileMatrixSetId));

        TileMatrixSetLimits tileLimits = limitsGenerator.getCollectionTileMatrixSetLimits(apiData, collectionId, tileMatrixSet, zoomLevels, crsTransformerFactory)
                .stream()
                .filter(limits -> limits.getTileMatrix().equals(tileMatrix))
                .findAny()
                .orElse(null);

        if (tileLimits!=null) {
            if (tileLimits.getMaxTileCol()<col || tileLimits.getMinTileCol()>col ||
                    tileLimits.getMaxTileRow()<row || tileLimits.getMinTileRow()>row)
                // return 404, if outside the range
                throw new NotFoundException("The requested tile is outside of the limits for this zoom level and tile set.");
        }

        String path = definitionPath.replace("{collectionId}", collectionId)
                .replace("{tileMatrixSetId}", tileMatrixSetId)
                .replace("{tileMatrix}", tileMatrix)
                .replace("{tileRow}", tileRow)
                .replace("{tileCol}", tileCol);
        TileFormatExtension outputFormat = api.getOutputFormat(TileFormatExtension.class, requestContext.getMediaType(), path, Optional.of(collectionId))
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        Tile tile = new ImmutableTile.Builder()
                .collectionIds(ImmutableList.of(collectionId))
                .tileMatrixSet(tileMatrixSet)
                .tileLevel(level)
                .tileRow(row)
                .tileCol(col)
                .api(api)
                .temporary(!useCache)
                .featureProvider(featureProvider)
                .outputFormat(outputFormat)
                .build();

        // if cache can be used and the tile is cached for the requested format, return the cache
        if (useCache) {
            // get the tile from the cache and return it
            java.nio.file.Path tileFile = cache.getFile(tile);
            if (Files.exists(tileFile)) {
                TilesQueriesHandler.QueryInputTileFile queryInput = new ImmutableQueryInputTileFile.Builder()
                        .from(getGenericQueryInput(api.getData()))
                        .tile(tile)
                        .tileFile(tileFile)
                        .build();

                return queryHandler.handle(TilesQueriesHandler.Query.TILE_FILE, queryInput, requestContext);
            }
        }

        // don't store the tile in the cache if it is outside the range
        MinMax cacheMinMax = tilesConfiguration.getZoomLevelsCache()
                                               .get(tileMatrixSetId);
        Tile finalTile = Objects.isNull(cacheMinMax) || (level <= cacheMinMax.getMax() && level >= cacheMinMax.getMin()) ?
                tile :
                new ImmutableTile.Builder()
                        .from(tile)
                        .temporary(true)
                        .build();

        // first execute the information that is passed as processing parameters (e.g., "properties")
        Map<String, Object> processingParameters = new HashMap<>();
        for (OgcApiQueryParameter parameter : allowedParameters) {
            processingParameters = parameter.transformContext(null, processingParameters, queryParams, api.getData());
        }

        FeatureQuery query = outputFormat.getQuery(tile, allowedParameters, queryParams, tilesConfiguration, requestContext.getUriCustomizer());

        FeaturesCoreConfiguration coreConfiguration = featureType.getExtension(FeaturesCoreConfiguration.class).get();

        // TODO add caching information
        TilesQueriesHandler.QueryInputTileSingleLayer queryInput = new ImmutableQueryInputTileSingleLayer.Builder()
                .from(getGenericQueryInput(api.getData()))
                .tile(finalTile)
                .query(query)
                .processingParameters(processingParameters)
                .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
                .build();

        return queryHandler.handle(TilesQueriesHandler.Query.SINGLE_LAYER_TILE, queryInput, requestContext);
    }

    private void ensureFeatureProviderSupportsQueries(FeatureProvider2 featureProvider) {
        if (!featureProvider.supportsQueries()) {
            throw new IllegalStateException("Feature provider does not support queries.");
        }
    }
}

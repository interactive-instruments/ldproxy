/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.gltf.domain.Format3dTilesSubtree;
import de.ii.ogcapi.features.gltf.domain.Format3dTilesTileset;
import de.ii.ogcapi.features.gltf.domain.ImmutableAssetMetadata;
import de.ii.ogcapi.features.gltf.domain.ImmutableBoundingVolume3dTiles;
import de.ii.ogcapi.features.gltf.domain.ImmutableContent3dTiles;
import de.ii.ogcapi.features.gltf.domain.ImmutableImplicitTiling3dTiles;
import de.ii.ogcapi.features.gltf.domain.ImmutableTile3dTiles;
import de.ii.ogcapi.features.gltf.domain.ImmutableTileset3dTiles;
import de.ii.ogcapi.features.gltf.domain.QueriesHandler3dTiles;
import de.ii.ogcapi.features.gltf.domain.Tileset3dTiles;
import de.ii.ogcapi.features.gltf.domain._3dTilesConfiguration;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.DefaultLinksGenerator;
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
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.web.domain.ETag;
import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@AutoBind
public class QueriesHandler3dTilesImpl implements QueriesHandler3dTiles {

  private final I18n i18n;
  private final FeaturesCoreQueriesHandler queriesHandlerFeatures;
  private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;

  @Inject
  public QueriesHandler3dTilesImpl(I18n i18n, FeaturesCoreQueriesHandler queriesHandlerFeatures) {
    this.i18n = i18n;
    this.queriesHandlerFeatures = queriesHandlerFeatures;
    this.queryHandlers =
        ImmutableMap.of(
            Query.TILESET,
            QueryHandler.with(QueryInputTileset.class, this::getTilesetResponse),
            Query.SUBTREE,
            QueryHandler.with(QueryInputSubtree.class, this::getSubtreeResponse));
  }

  @Override
  public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
    return queryHandlers;
  }

  public static void checkCollectionId(OgcApiDataV2 apiData, String collectionId) {
    if (!apiData.isCollectionEnabled(collectionId)) {
      throw new NotFoundException(
          MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));
    }
  }

  private Response getTilesetResponse(
      QueryInputTileset queryInput, ApiRequestContext requestContext) {

    final OgcApi api = requestContext.getApi();
    final OgcApiDataV2 apiData = api.getData();
    final String collectionId = queryInput.getCollectionId();

    if (!apiData.isCollectionEnabled(collectionId)) {
      throw new NotFoundException(
          MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));
    }
    FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);

    Format3dTilesTileset outputFormat =
        api.getOutputFormat(
                Format3dTilesTileset.class,
                requestContext.getMediaType(),
                "/collections/" + collectionId + "/3dtiles",
                Optional.of(collectionId))
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported for this resource.",
                            requestContext.getMediaType())));

    checkCollectionId(api.getData(), collectionId);
    List<ApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

    List<Link> links =
        new DefaultLinksGenerator()
            .generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                alternateMediaTypes,
                i18n,
                requestContext.getLanguage());

    BoundingBox bbox = api.getSpatialExtent(collectionId).orElseThrow();

    _3dTilesConfiguration cfg =
        api.getData()
            .getCollectionData(collectionId)
            .flatMap(c -> c.getExtension(_3dTilesConfiguration.class))
            .orElseThrow();

    Tileset3dTiles tileset =
        ImmutableTileset3dTiles.builder()
            .asset(ImmutableAssetMetadata.builder().version("1.1").build())
            .geometricError(5000)
            .root(
                ImmutableTile3dTiles.builder()
                    .boundingVolume(
                        ImmutableBoundingVolume3dTiles.builder()
                            .region(
                                ImmutableList.of(
                                    Helper.degToRad(bbox.getXmin()),
                                    Helper.degToRad(bbox.getYmin()),
                                    Helper.degToRad(bbox.getXmax()),
                                    Helper.degToRad(bbox.getYmax()),
                                    Objects.requireNonNull(bbox.getZmin()),
                                    Objects.requireNonNull(bbox.getZmax())))
                            .build())
                    .geometricError(1024)
                    .refine("REPLACE")
                    .content(
                        ImmutableContent3dTiles.builder().uri("content/{level}/{x}/{y}").build())
                    .implicitTiling(
                        ImmutableImplicitTiling3dTiles.builder()
                            .subdivisionScheme("QUADTREE")
                            .availableLevels(Objects.requireNonNull(cfg.getAvailableLevels()))
                            .subtreeLevels(Objects.requireNonNull(cfg.getSubtreeLevels()))
                            .subtrees(
                                ImmutableContent3dTiles.builder()
                                    .uri("subtrees/{level}/{x}/{y}")
                                    .build())
                            .build())
                    .build())
            .build();

    Date lastModified = getLastModified(queryInput);
    EntityTag etag =
        !outputFormat.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
                || apiData
                    .getExtension(HtmlConfiguration.class, collectionId)
                    .map(HtmlConfiguration::getSendEtags)
                    .orElse(false)
            ? ETag.from(tileset, Tileset3dTiles.FUNNEL, outputFormat.getMediaType().label())
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
                    "%s.tileset.%s", collectionId, outputFormat.getMediaType().fileExtension())))
        .entity(outputFormat.getEntity(tileset, links, collectionId, api, requestContext))
        .build();
  }

  private Response getSubtreeResponse(
      QueryInputSubtree queryInput, ApiRequestContext requestContext) {

    final OgcApi api = requestContext.getApi();
    final OgcApiDataV2 apiData = api.getData();
    final String collectionId = queryInput.getCollectionId();

    if (!apiData.isCollectionEnabled(collectionId)) {
      throw new NotFoundException(
          MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));
    }

    Format3dTilesSubtree outputFormat =
        api.getOutputFormat(
                Format3dTilesSubtree.class,
                requestContext.getMediaType(),
                String.format(
                    "/collections/%s/3dtiles/subtree/%d/%d/%d",
                    collectionId, queryInput.getLevel(), queryInput.getX(), queryInput.getY()),
                Optional.of(collectionId))
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported for this resource.",
                            requestContext.getMediaType())));

    checkCollectionId(api.getData(), collectionId);
    List<ApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

    List<Link> links =
        new DefaultLinksGenerator()
            .generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                alternateMediaTypes,
                i18n,
                requestContext.getLanguage());

    ByteArrayOutputStream bufferTilesAvailability = new ByteArrayOutputStream();
    ByteArrayOutputStream bufferContentAvailability = new ByteArrayOutputStream();
    ByteArrayOutputStream bufferChildSubtreeAvailability = new ByteArrayOutputStream();

    int level = queryInput.getLevel();
    long x = queryInput.getX();
    long y = queryInput.getY();

    // subtree_level = 0
    // sub:
    //   computeBbox
    //   query data
    //   check, if there is at least one feature in the tile
    //   tile available (or not)
    //   content available = tile available and level >= MIN_CONTENT_LEVEL
    // subtree_level++
    // while subtree_level < SUBTREE_LEVELS && level+subtree_level < AVAILABLE_LEVELS
    //   i = 0 .. MortonCode.size(level+subtree_level)-1
    //     coord = MortonCode.decode(i)
    //     for level+subtree_level, coord.x, coord.y:
    //       sub
    // level+subtree_level < AVAILABLE_LEVELS
    //   i = 0 .. MortonCode.size(level+subtree_level)-1
    //     coord = MortonCode.decode(i)
    //     for level+subtree_level, coord.x, coord.y:
    //       computeBbox
    //       query data
    //       check, if there is at least one feature in the tile
    //       child subtree available (or not)

    BoundingBox bbox = Helper.computeBbox(api, collectionId, level, x, y);
    boolean hasData = Helper.hasData(queriesHandlerFeatures, queryInput, bbox);
    byte[] result = (hasData ? "1" : "0").getBytes(); // TODO

    Date lastModified = getLastModified(queryInput);
    EntityTag etag =
        !outputFormat.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
                || apiData
                    .getExtension(HtmlConfiguration.class, collectionId)
                    .map(HtmlConfiguration::getSendEtags)
                    .orElse(false)
            ? ETag.from(result)
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
                    "%s.tileset.%s", collectionId, outputFormat.getMediaType().fileExtension())))
        .entity(outputFormat.getEntity(result, links, collectionId, api, requestContext))
        .build();
  }
}

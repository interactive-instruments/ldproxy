/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.app;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.Query;
import de.ii.ogcapi.features.core.domain.ImmutableQueryInputFeatures;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext.Builder;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.tiles3d.domain.TileResourceDescriptor;
import de.ii.ogcapi.tiles3d.domain.Tiles3dConfiguration;
import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.Cql.Format;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.core.Response;

public final class Tiles3dContentUtil {

  private Tiles3dContentUtil() {}

  public static Response getContent(
      FeatureProvider2 provider,
      FeaturesCoreQueriesHandler queriesHandlerFeatures,
      Cql cql,
      Tiles3dConfiguration cfg,
      TileResourceDescriptor r,
      FeatureQuery featureQuery,
      URICustomizer uriCustomizer,
      Optional<QueryInput> queryInputGeneric)
      throws URISyntaxException {

    String contentFilterString = "true";
    FeatureQuery query =
        cfg.getContentFilters().isEmpty()
            ? featureQuery
            : getFinalQuery(
                cfg.getContentFilters()
                    .get(r.getLevel() - Objects.requireNonNull(cfg.getFirstLevelWithContent())),
                featureQuery,
                cql);

    FeaturesCoreQueriesHandler.QueryInputFeatures queryInput =
        getQueryInputFeatures(query, provider, r, queryInputGeneric);

    ApiRequestContext requestContextGltf =
        getFeaturesRequestContext(uriCustomizer, cfg, r, contentFilterString);

    return queriesHandlerFeatures.handle(Query.FEATURES, queryInput, requestContextGltf);
  }

  private static FeaturesCoreQueriesHandler.QueryInputFeatures getQueryInputFeatures(
      FeatureQuery query,
      FeatureProvider2 provider,
      TileResourceDescriptor r,
      Optional<QueryInput> queryInputGeneric) {
    FeaturesCoreQueriesHandler.QueryInputFeatures queryInput;
    ImmutableQueryInputFeatures.Builder builder = new ImmutableQueryInputFeatures.Builder();
    queryInputGeneric.ifPresent(builder::from);
    queryInput =
        builder
            .collectionId(r.getCollectionId())
            .query(query)
            .featureProvider(provider)
            .defaultCrs(OgcCrs.CRS84h)
            .defaultPageSize(Optional.of(TileResourceDescriptor.MAX_FEATURES_PER_TILE))
            .showsFeatureSelfLink(false)
            .sendResponseAsStream(false)
            .build();
    return queryInput;
  }

  private static ApiRequestContext getFeaturesRequestContext(
      URICustomizer uriCustomizer,
      Tiles3dConfiguration cfg,
      TileResourceDescriptor r,
      String contentFilterString)
      throws URISyntaxException {
    return new Builder()
        .api(r.getApi())
        .requestUri(
            uriCustomizer
                .copy()
                .removeLastPathSegments(2)
                .ensureLastPathSegment("items")
                .clearParameters()
                .addParameter("f", Format3dTilesContentGltfBinary.MEDIA_TYPE.parameter())
                .addParameter("bbox", r.getBboxString())
                .addParameter("filter", contentFilterString)
                .addParameter("clampToEllipsoid", String.valueOf(cfg.shouldClampToEllipsoid()))
                .build())
        .externalUri(uriCustomizer.copy().removeLastPathSegments(4).clearParameters().build())
        .mediaType(Format3dTilesContentGltfBinary.MEDIA_TYPE)
        .alternateMediaTypes(ImmutableList.of())
        .build();
  }

  private static FeatureQuery getFinalQuery(String filter, FeatureQuery query, Cql cql) {
    return ImmutableFeatureQuery.builder()
        .from(query)
        .filter(And.of(query.getFilter().orElseThrow(), cql.read(filter, Format.TEXT)))
        .build();
  }
}

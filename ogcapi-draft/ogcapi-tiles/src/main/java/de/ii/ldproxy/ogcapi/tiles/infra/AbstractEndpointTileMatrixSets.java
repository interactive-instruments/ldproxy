/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.infra;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.Endpoint;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.ImmutableQueryInputTileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.ImmutableQueryInputTileMatrixSets;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetsQueriesHandler;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Optional;

public abstract class AbstractEndpointTileMatrixSets extends Endpoint {

    private final TileMatrixSetsQueriesHandler queryHandler;
    private final TileMatrixSetRepository tileMatrixSetRepository;

    public AbstractEndpointTileMatrixSets(ExtensionRegistry extensionRegistry, TileMatrixSetsQueriesHandler queryHandler,
                                          TileMatrixSetRepository tileMatrixSetRepository) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
        this.tileMatrixSetRepository = tileMatrixSetRepository;
    }

    public Response getTileMatrixSets(OgcApi api, ApiRequestContext requestContext) {

        if (!isEnabledForApi(api.getData()))
            throw new NotFoundException("Tile matrix sets are not available in this API.");

        ImmutableSet<TileMatrixSet> tmsSet = getPathParameters(extensionRegistry, api.getData(), "/tileMatrixSets/{tileMatrixSetId}").stream()
                .filter(param -> param.getName().equalsIgnoreCase("tileMatrixSetId"))
                .findFirst()
                .map(param -> param.getValues(api.getData())
                        .stream()
                        .map(tileMatrixSetRepository::get)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(ImmutableSet.toImmutableSet()))
                .orElse(ImmutableSet.of());

        TileMatrixSetsQueriesHandler.QueryInputTileMatrixSets queryInput = new ImmutableQueryInputTileMatrixSets.Builder()
                .from(getGenericQueryInput(api.getData()))
                .tileMatrixSets(tmsSet)
                .build();

        return queryHandler.handle(TileMatrixSetsQueriesHandler.Query.TILE_MATRIX_SETS, queryInput, requestContext);
    }

    public Response getTileMatrixSet(String tileMatrixSetId, OgcApi api, ApiRequestContext requestContext) {

        checkPathParameter(extensionRegistry, api.getData(), "/tileMatrixSets/{tileMatrixSetId}", "tileMatrixSetId", tileMatrixSetId);

        TileMatrixSetsQueriesHandler.QueryInputTileMatrixSet queryInput = new ImmutableQueryInputTileMatrixSet.Builder()
                .from(getGenericQueryInput(api.getData()))
                .tileMatrixSetId(tileMatrixSetId)
                .build();

        return queryHandler.handle(TileMatrixSetsQueriesHandler.Query.TILE_MATRIX_SET, queryInput, requestContext);
    }
}

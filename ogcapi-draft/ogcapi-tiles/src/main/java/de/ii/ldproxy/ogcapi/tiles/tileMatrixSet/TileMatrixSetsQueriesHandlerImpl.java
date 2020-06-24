/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.tileMatrixSet;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.tiles.VectorTilesLinkGenerator;
import de.ii.xtraplatform.codelists.CodelistRegistry;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

@Component
@Instantiate
@Provides
public class TileMatrixSetsQueriesHandlerImpl implements TileMatrixSetsQueriesHandler {

    private final I18n i18n;
    private final Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> queryHandlers;
    private final OgcApiExtensionRegistry extensionRegistry;

    public TileMatrixSetsQueriesHandlerImpl(@Requires I18n i18n,
                                            @Requires Dropwizard dropwizard,
                                            @Requires OgcApiExtensionRegistry extensionRegistry) {
        this.i18n = i18n;
        this.extensionRegistry = extensionRegistry;

        this.queryHandlers = ImmutableMap.of(
                Query.TILE_MATRIX_SETS,
                OgcApiQueryHandler.with(OgcApiQueryInputTileMatrixSets.class, this::getTileMatrixSetsResponse),
                Query.TILE_MATRIX_SET,
                OgcApiQueryHandler.with(OgcApiQueryInputTileMatrixSet.class, this::getTileMatrixSetResponse)
        );
    }

    @Override
    public Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    private Response getTileMatrixSetsResponse(OgcApiQueryInputTileMatrixSets queryInput, OgcApiRequestContext requestContext) {
        OgcApiApi api = requestContext.getApi();
        String path = "/tileMatrixSets";

        TileMatrixSetsFormatExtension outputFormat = api.getOutputFormat(TileMatrixSetsFormatExtension.class, requestContext.getMediaType(), path)
                .orElseThrow(NotAcceptableException::new);

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        List<OgcApiLink> links = new TileMatrixSetsLinksGenerator().generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                queryInput.getIncludeHomeLink(),
                true,
                i18n,
                requestContext.getLanguage());

        TileMatrixSets tileMatrixSets = ImmutableTileMatrixSets.builder()
                .tileMatrixSets(
                        queryInput.getTileMatrixSets()
                                .stream()
                                .map(tileMatrixSet -> ImmutableTileMatrixSetLinks.builder()
                                        .id(tileMatrixSet.getId())
                                        .title(tileMatrixSet.getTileMatrixSetData().getTitle())
                                        .links(vectorTilesLinkGenerator.generateTileMatrixSetsLinks(
                                                requestContext.getUriCustomizer(),
                                                tileMatrixSet.getId(),
                                                i18n,
                                                requestContext.getLanguage()))
                                        .build())
                                .collect(Collectors.toList()))
                .links(links)
                .build();

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(outputFormat.getTileMatrixSetsEntity(tileMatrixSets, api, requestContext))
                .build();
    }

    private Response getTileMatrixSetResponse(OgcApiQueryInputTileMatrixSet queryInput, OgcApiRequestContext requestContext) {
        OgcApiApi api = requestContext.getApi();
        String tileMatrixSetId = queryInput.getTileMatrixSetId();
        String path = "/tileMatrixSets/"+tileMatrixSetId;

        TileMatrixSetsFormatExtension outputFormat = api.getOutputFormat(TileMatrixSetsFormatExtension.class, requestContext.getMediaType(), path)
                .orElseThrow(NotAcceptableException::new);

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        List<OgcApiLink> links = new TileMatrixSetsLinksGenerator().generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                queryInput.getIncludeHomeLink(),
                false,
                i18n,
                requestContext.getLanguage());

        TileMatrixSet tileMatrixSet = extensionRegistry.getExtensionsForType(TileMatrixSet.class).stream()
                .filter(tms -> tms.getId().equals(tileMatrixSetId))
                .findAny()
                .orElseThrow(() -> new NotFoundException("Unknown tile matrix set: " + tileMatrixSetId));

        TileMatrixSetData tileMatrixSetData = ImmutableTileMatrixSetData.builder()
                .from(tileMatrixSet.getTileMatrixSetData())
                .links(links)
                .build();

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(outputFormat.getTileMatrixSetEntity(tileMatrixSetData, api, requestContext))
                .build();
    }
}

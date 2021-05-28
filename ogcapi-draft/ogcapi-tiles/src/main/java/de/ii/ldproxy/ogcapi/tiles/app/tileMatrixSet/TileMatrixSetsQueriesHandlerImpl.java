/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app.tileMatrixSet;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.tiles.app.VectorTilesLinkGenerator;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.ImmutableTileMatrixSetData;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.ImmutableTileMatrixSetLinks;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.ImmutableTileMatrixSets;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetData;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSets;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetsFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetsLinksGenerator;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetsQueriesHandler;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Instantiate
@Provides
public class TileMatrixSetsQueriesHandlerImpl implements TileMatrixSetsQueriesHandler {

    private final I18n i18n;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
    private final ExtensionRegistry extensionRegistry;

    public TileMatrixSetsQueriesHandlerImpl(@Requires I18n i18n,
                                            @Requires ExtensionRegistry extensionRegistry) {
        this.i18n = i18n;
        this.extensionRegistry = extensionRegistry;

        this.queryHandlers = ImmutableMap.of(
                Query.TILE_MATRIX_SETS,
                QueryHandler.with(QueryInputTileMatrixSets.class, this::getTileMatrixSetsResponse),
                Query.TILE_MATRIX_SET,
                QueryHandler.with(QueryInputTileMatrixSet.class, this::getTileMatrixSetResponse)
        );
    }

    @Override
    public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    private Response getTileMatrixSetsResponse(QueryInputTileMatrixSets queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        String path = "/tileMatrixSets";

        TileMatrixSetsFormatExtension outputFormat = api.getOutputFormat(TileMatrixSetsFormatExtension.class, requestContext.getMediaType(), path, Optional.empty())
                                                        .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        List<Link> links = new TileMatrixSetsLinksGenerator().generateLinks(requestContext.getUriCustomizer(),
                                                                            requestContext.getMediaType(),
                                                                            requestContext.getAlternateMediaTypes(),
                                                                            true,
                                                                            i18n,
                                                                            requestContext.getLanguage());

        TileMatrixSets tileMatrixSets = ImmutableTileMatrixSets.builder()
                                                               .tileMatrixSets(queryInput.getTileMatrixSets()
                                                                                         .stream()
                                                                                         .map(tileMatrixSet -> ImmutableTileMatrixSetLinks.builder()
                                                                                                                                          .id(tileMatrixSet.getId())
                                                                                                                                          .title(tileMatrixSet.getTileMatrixSetData().getTitle())
                                                                                                                                          .tileMatrixSetURI(tileMatrixSet.getURI().map(URI::toString))
                                                                                                                                          .links(vectorTilesLinkGenerator.generateTileMatrixSetsLinks(requestContext.getUriCustomizer(),
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

    private Response getTileMatrixSetResponse(QueryInputTileMatrixSet queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        String tileMatrixSetId = queryInput.getTileMatrixSetId();
        String path = "/tileMatrixSets/"+tileMatrixSetId;

        TileMatrixSetsFormatExtension outputFormat = api.getOutputFormat(TileMatrixSetsFormatExtension.class, requestContext.getMediaType(), path, Optional.empty())
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        List<Link> links = new TileMatrixSetsLinksGenerator().generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
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

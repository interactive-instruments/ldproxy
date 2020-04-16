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
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * fetch tiling schemes / tile matrix sets that have been configured for an API
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointTileMatrixSets implements OgcApiEndpointExtension, ConformanceClass {

    // TODO convert to query handler approach, create format extensions, etc.

    @Requires
    I18n i18n;

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointTileMatrixSets.class);

    private final OgcApiExtensionRegistry extensionRegistry;

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("tileMatrixSets")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/?(?:\\w+)?$")
            .build();

    private final VectorTilesCache cache;
    private final VectorTileMapGenerator vectorTileMapGenerator = new VectorTileMapGenerator();

    Wfs3EndpointTileMatrixSets(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                               @Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
        String dataDirectory = bundleContext.getProperty(DATA_DIR_KEY);
        cache = new VectorTilesCache(dataDirectory);
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 apiData, String subPath) {
        return ImmutableSet.of(
                new ImmutableOgcApiMediaType.Builder()
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build(),
                new ImmutableOgcApiMediaType.Builder()
                        .type(MediaType.TEXT_HTML_TYPE)
                        .build());
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/tmxs");
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    private Map<String, MinMax> getTileMatrixSetZoomLevels(OgcApiApiDataV2 data) {
        TilesConfiguration tilesConfiguration = getExtensionConfiguration(data, TilesConfiguration.class).get();
        return tilesConfiguration.getZoomLevels();
    }

    private void checkTileMatrixSet(OgcApiApiDataV2 data, String tileMatrixSetId) {
        Set<String> tileMatrixSets = getTileMatrixSetZoomLevels(data).keySet();
        if (!tileMatrixSets.contains(tileMatrixSetId)) {
            throw new NotFoundException("Unknown tile matrix set: " + tileMatrixSetId);
        }
    }

    /**
     * retrieve all available tile matrix sets
     *
     * @return all tile matrix sets in a json array or an HTML view
     */
    @Path("/")
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_HTML})
    public Response getTileMatrixSets(@Context OgcApiApi api, @Context OgcApiRequestContext requestContext) {

        checkTilesParameterDataset(vectorTileMapGenerator.getEnabledMap(api.getData()));

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        List<OgcApiLink> links = new TileMatrixSetsLinksGenerator().generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                false, // TODO get from configuration
                true,
                i18n,
                requestContext.getLanguage());

        Map<String, MinMax> tileMatrixSetZoomLevels = getTileMatrixSetZoomLevels(api.getData());

        TileMatrixSets tileMatrixSets = ImmutableTileMatrixSets.builder()
                .tileMatrixSets(
                    tileMatrixSetZoomLevels
                        .keySet()
                        .stream()
                        .map(tileMatrixSetId -> ImmutableTileMatrixSetLinks.builder()
                            .id(tileMatrixSetId)
                            .title(deriveTitle(tileMatrixSetId))
                            .links(vectorTilesLinkGenerator.generateTileMatrixSetsLinks(
                                    requestContext.getUriCustomizer(),
                                    tileMatrixSetId.toString(),
                                    i18n,
                                    requestContext.getLanguage()))
                                .build())
                        .collect(Collectors.toList()))
                .links(links)
                .build();

        if (requestContext.getMediaType().matches(MediaType.TEXT_HTML_TYPE)) {
            Optional<TileMatrixSetsFormatExtension> outputFormatHtml = api.getOutputFormat(TileMatrixSetsFormatExtension.class, requestContext.getMediaType(), "/tileMatrixSets");
            if (outputFormatHtml.isPresent())
                return outputFormatHtml.get().getTileMatrixSetsResponse(tileMatrixSets, api, requestContext);

            throw new NotAcceptableException();
        }

        return Response.ok(tileMatrixSets)
                       .build();
    }

    private Optional<String> deriveTitle(String tileMatrixSetId) {
        TileMatrixSet tileMatrixSet = null;
        for (OgcApiContentExtension contentExtension : extensionRegistry.getExtensionsForType(OgcApiContentExtension.class)) {
            if (contentExtension instanceof TileMatrixSet && ((TileMatrixSet) contentExtension).getId().equals(tileMatrixSetId)) {
                tileMatrixSet = (TileMatrixSet) contentExtension;
                break;
            }
        }
        if (Objects.isNull(tileMatrixSet)) {
            throw new NotFoundException("Unknown tile matrix set: " + tileMatrixSetId);
        }

        TileMatrixSetData tileMatrixSetData = tileMatrixSet.getTileMatrixSetData();
        return tileMatrixSetData.getTitle();
    }

    /**
     * retrieve one specific tile matrix set by id
     *
     * @param tileMatrixSetId   the local identifier of a specific tile matrix set
     * @return the tiling scheme in a json file
     */
    @Path("/{tileMatrixSetId}")
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_HTML})
    public Response getTileMatrixSet(@PathParam("tileMatrixSetId") String tileMatrixSetId,
                                     @Context OgcApiApi api,
                                     @Context OgcApiRequestContext requestContext) {

        checkTilesParameterDataset(vectorTileMapGenerator.getEnabledMap(api.getData()));
        checkTileMatrixSet(api.getData(), tileMatrixSetId);

        TileMatrixSet tileMatrixSet = null;
        for (OgcApiContentExtension contentExtension : extensionRegistry.getExtensionsForType(OgcApiContentExtension.class)) {
            if (contentExtension instanceof TileMatrixSet && ((TileMatrixSet) contentExtension).getId().equals(tileMatrixSetId)) {
                tileMatrixSet = (TileMatrixSet) contentExtension;
                break;
            }
        }
        if (Objects.isNull(tileMatrixSet)) {
            throw new NotFoundException("Unknown tile matrix set: " + tileMatrixSetId);
        }

        TileMatrixSetData jsonTileMatrixSet = tileMatrixSet.getTileMatrixSetData();

        List<OgcApiLink> links = new TileMatrixSetsLinksGenerator().generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                false, // TODO get from configuration
                true,
                i18n,
                requestContext.getLanguage());

        TileMatrixSetData tileMatrixSetData = ImmutableTileMatrixSetData.builder()
                .from(jsonTileMatrixSet)
                .links(links)
                .build();

        if (requestContext.getMediaType().matches(MediaType.TEXT_HTML_TYPE)) {
            Optional<TileMatrixSetsFormatExtension> outputFormatHtml = api.getOutputFormat(TileMatrixSetsFormatExtension.class, requestContext.getMediaType(), "/tileMatrixSets/"+tileMatrixSetId);
            if (outputFormatHtml.isPresent()) {
                return outputFormatHtml.get().getTileMatrixSetResponse(tileMatrixSetData, api, requestContext);
            }

            throw new NotAcceptableException();
        }

        return Response.ok(tileMatrixSetData)
                       .build();
    }

    /**
     * checks if the tiles parameter is enabled in the dataset. If the tiles parameter is disabled in all collections, it throws a 404.
     *
     * @param enabledMap    a map with all collections and the boolean if the tiles support is enabled or not
     */
    private void checkTilesParameterDataset(Map<String, Boolean> enabledMap) {

        if (!Objects.isNull(enabledMap)) {
            for (String collectionId : enabledMap.keySet()) {
                if (enabledMap.get(collectionId))
                    return;
            }
        }
        throw new NotFoundException();
    }

}

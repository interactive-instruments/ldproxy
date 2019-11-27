/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
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
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
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
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData apiData, String subPath) {
        return ImmutableSet.of(
                new ImmutableOgcApiMediaType.Builder()
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build(),
                new ImmutableOgcApiMediaType.Builder()
                        .type(MediaType.TEXT_HTML_TYPE)
                        .build());
    }

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/tmxs";
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    /**
     * retrieve all available tile matrix sets
     *
     * @return all tile matrix sets in a json array or an HTML view
     */
    @Path("/")
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_HTML})
    public Response getTileMatrixSets(@Context OgcApiDataset api, @Context OgcApiRequestContext requestContext) {

        Wfs3EndpointTiles.checkTilesParameterDataset(vectorTileMapGenerator.getEnabledMap(api.getData()));

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        List<OgcApiLink> links = new TileMatrixSetsLinksGenerator().generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                false, // TODO get from configuration
                true,
                i18n,
                requestContext.getLanguage());

        TileMatrixSets tileMatrixSets = ImmutableTileMatrixSets.builder()
                .tileMatrixSets(
                    cache.getTileMatrixSetIds()
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
        TileMatrixSetData tileMatrixSet = getTileMatrixSetFromStore(tileMatrixSetId);
        return tileMatrixSet.getTitle();
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
                                     @Context OgcApiDataset api,
                                     @Context OgcApiRequestContext requestContext) {

        Wfs3EndpointTiles.checkTilesParameterDataset(vectorTileMapGenerator.getEnabledMap(api.getData()));

        File file = cache.getTileMatrixSet(tileMatrixSetId);

        TileMatrixSetData jsonTileMatrixSet = getTileMatrixSetFromStore(tileMatrixSetId);

        List<OgcApiLink> links = new TileMatrixSetsLinksGenerator().generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                false, // TODO get from configuration
                true,
                i18n,
                requestContext.getLanguage());

        TileMatrixSetData tileMatrixSet = ImmutableTileMatrixSetData.builder()
                .from(jsonTileMatrixSet)
                .links(links)
                .build();

        if (requestContext.getMediaType().matches(MediaType.TEXT_HTML_TYPE)) {
            Optional<TileMatrixSetsFormatExtension> outputFormatHtml = api.getOutputFormat(TileMatrixSetsFormatExtension.class, requestContext.getMediaType(), "/tileMatrixSets/"+tileMatrixSetId);
            if (outputFormatHtml.isPresent()) {
                return outputFormatHtml.get().getTileMatrixSetResponse(tileMatrixSet, api, requestContext);
            }

            throw new NotAcceptableException();
        }

        return Response.ok(tileMatrixSet)
                       .build();
    }

    private  TileMatrixSetData getTileMatrixSetFromStore(String tileMatrixSetId) {
        File file = cache.getTileMatrixSet(tileMatrixSetId);

        try {
            final byte[] content = java.nio.file.Files.readAllBytes(file.toPath());

            // prepare Jackson mapper for deserialization
            final ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new Jdk8Module());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                // parse input
                TileMatrixSetData tileMatrixSet = mapper.readValue(content, TileMatrixSetData.class);

                return tileMatrixSet;
            } catch (IOException e) {
                LOGGER.error("Tile Matrix Set file in store is invalid: "+file.getAbsolutePath());
                LOGGER.error("Reason: "+e.getMessage());
            }
        } catch (IOException e) {
            LOGGER.error("Tile Matrix Set could not be read: "+tileMatrixSetId);
            LOGGER.error("Reason: "+e.getMessage());
        }

        throw new InternalServerErrorException();
    }
}

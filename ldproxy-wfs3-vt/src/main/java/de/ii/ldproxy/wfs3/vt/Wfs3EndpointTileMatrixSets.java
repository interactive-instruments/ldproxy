/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * fetch tiling schemes / tile matrix sets that have been configured for this service
 *
 * @author portele
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointTileMatrixSets implements OgcApiEndpointExtension, ConformanceClass {

    @Requires
    I18n i18n;

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("tileMatrixSets")
            .addMethods(OgcApiContext.HttpMethods.GET)
            .subPathPattern("^/?(?:\\w+)?$")
            .build();

    private final VectorTilesCache cache;
    private final VectorTileMapGenerator vectorTileMapGenerator = new VectorTileMapGenerator();

    Wfs3EndpointTileMatrixSets(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) {
        String dataDirectory = bundleContext.getProperty(DATA_DIR_KEY);
        cache = new VectorTilesCache(dataDirectory);

        // TODO: populate tiling scheme registry with default tiling scheme(s)
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset, String subPath) {
        // TODO: generalize
        return ImmutableSet.of(
                new ImmutableOgcApiMediaType.Builder()
                        .type(MediaType.APPLICATION_JSON_TYPE)
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
     * retrieve all available tiling schemes
     *
     * @return all tile matrix sets in a json array
     */
    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTilingSchemes(@Context OgcApiDataset service, @Context OgcApiRequestContext wfs3Request) {

        Wfs3EndpointTiles.checkTilesParameterDataset(vectorTileMapGenerator.getEnabledMap(service.getData()));

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
        List<Map<String, Object>> wfs3LinksList = new ArrayList<>();

        for (Object tileMatrixSetId : cache.getTileMatrixSetIds()
                                          .toArray()) {
            Map<String, Object> wfs3LinksMap = new HashMap<>();
            wfs3LinksMap.put("id", tileMatrixSetId);
            wfs3LinksMap.put("title", "Google Maps Compatible for the World");
            wfs3LinksMap.put("links", vectorTilesLinkGenerator.generateTileMatrixSetsLinks(wfs3Request.getUriCustomizer(), tileMatrixSetId.toString(), i18n, wfs3Request.getLanguage()));
            wfs3LinksList.add(wfs3LinksMap);
        }

        return Response.ok(ImmutableMap.of("tileMatrixSetLinks", wfs3LinksList))
                       .build();
    }

    /**
     * retrieve one specific tile matrix set by id
     *
     * @param tileMatrixSetId   the local identifier of a specific tile matrix set
     * @return the tiling scheme in a json file
     */
    @Path("/{tileMatrixSetId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTilingScheme(@PathParam("tileMatrixSetId") String tileMatrixSetId,
                                    @Context OgcApiDataset service) {

        Wfs3EndpointTiles.checkTilesParameterDataset(vectorTileMapGenerator.getEnabledMap(service.getData()));


        File file = cache.getTileMatrixSet(tileMatrixSetId);

        return Response.ok(file, MediaType.APPLICATION_JSON)
                       .build();
    }
}

/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.List;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * fetch tiling schemes / tile matrix sets that have been configured for this service
 *
 * @author portele
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointTilingSchemes implements Wfs3EndpointExtension {

    private final VectorTilesCache cache;

    Wfs3EndpointTilingSchemes(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) {
        String dataDirectory = bundleContext.getProperty(DATA_DIR_KEY);
        cache = new VectorTilesCache(dataDirectory);

        // TODO: populate tiling scheme registry with default tiling scheme(s)
    }

    @Override
    public String getPath() {
        return "tilingSchemes";
    }

    @Override
    public String getSubPathRegex() {
        return "^\\/?.*$";
    }

    @Override
    public List<String> getMethods() {
        return ImmutableList.of("GET");
    }

    @Override
    public boolean matches(String firstPathSegment, String method, String subPath) {
        return Wfs3EndpointExtension.super.matches(firstPathSegment, method, subPath);
    }

    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTilingSchemes() {

        return Response.ok(ImmutableMap.of("tilingSchemes", cache.getTilingSchemeIds().toArray() ))
                       .build();
    }

    @Path("/{tilingSchemeId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTilingScheme(@PathParam("tilingSchemeId") String tilingSchemeId) {

        File file = cache.getTilingScheme(tilingSchemeId);

        return Response.ok(file, MediaType.APPLICATION_JSON).build();
    }
}

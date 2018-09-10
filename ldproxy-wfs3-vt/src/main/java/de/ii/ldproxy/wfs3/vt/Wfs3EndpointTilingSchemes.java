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
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;

import javax.ws.rs.Consumes;
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
 * TODO: this is just a placeholder.
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointTilingSchemes implements Wfs3EndpointExtension {

    static final String TILING_SCHEMES_DIR_NAME = "tilingSchemes";

    // in dev env this would be build/data/tilingSchemes
    private final File tilingSchemesDirectory;

    Wfs3EndpointTilingSchemes(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) {
        this.tilingSchemesDirectory = new File(new File(bundleContext.getProperty(DATA_DIR_KEY)), TILING_SCHEMES_DIR_NAME);

        if (!tilingSchemesDirectory.exists()) {
            tilingSchemesDirectory.mkdirs();
        }
    }

    @Override
    public String getPath() {
        return "tilingSchemes";
    }

    @Override
    public List<String> getMethods() {
        return ImmutableList.of("GET");
    }


    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTilingSchemes() {

        //TODO read from tilingSchemesDirectory

        return Response.ok(ImmutableMap.of("tilingSchemes", ImmutableList.of()))
                       .build();
    }

    @Path("/{id}")
    @GET
    @Consumes(Wfs3MediaTypes.GEO_JSON)
    public Response getTilingScheme(@PathParam("id") String id) {

        //TODO read from tilingSchemesDirectory

        return Response.ok(ImmutableMap.of()).build();
    }
}

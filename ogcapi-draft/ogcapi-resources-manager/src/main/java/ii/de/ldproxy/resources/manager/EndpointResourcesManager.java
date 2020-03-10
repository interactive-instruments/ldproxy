/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package ii.de.ldproxy.resources.manager;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext.HttpMethods;
import de.ii.ldproxy.wfs3.styles.StylesConfiguration;
import de.ii.xtraplatform.auth.api.User;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * creates, updates and deletes a resource from the service
 */
@Component
@Provides
@Instantiate
public class EndpointResourcesManager implements OgcApiEndpointExtension, ConformanceClass {

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("resources")
            .addMethods(HttpMethods.PUT, HttpMethods.DELETE)
            .subPathPattern("^/?[^/]+$")
            .build();

    private final File resourcesStore;
    private final OgcApiExtensionRegistry extensionRegistry;

    public EndpointResourcesManager(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                                 @Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
        this.resourcesStore = new File(bundleContext.getProperty(DATA_DIR_KEY) + File.separator + "resources");
        if (!resourcesStore.exists()) {
            resourcesStore.mkdirs();
        }
    }

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/t15/opf-styles-1/1.0/conf/manage-resources";
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 dataset, String subPath) {
        if (subPath.matches("^/?[^/]+$"))
            return ImmutableSet.of(
                    new ImmutableOgcApiMediaType.Builder()
                            .type(MediaType.WILDCARD_TYPE)
                            .build());

        throw new ServerErrorException("Invalid sub path: " + subPath, 500);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<StylesConfiguration> stylesExtension = getExtensionConfiguration(apiData, StylesConfiguration.class);

        if (stylesExtension.isPresent() &&
                stylesExtension.get()
                        .getResourceManagerEnabled()) {
            return true;
        }
        return false;
    }

    /**
     * create or update a resource
     *
     * @param resourceId the local identifier of a specific style
     * @return empty response (204)
     */
    @Path("/{resourceId}")
    @PUT
    @Consumes(MediaType.WILDCARD)
    public Response putStyle(@Auth Optional<User> optionalUser, @PathParam("resourceId") String resourceId,
                             @Context OgcApiApi dataset, @Context OgcApiRequestContext ogcApiRequest,
                             @Context HttpServletRequest request, byte[] requestBody) {

        checkAuthorization(dataset.getData(), optionalUser);

        final String datasetId = dataset.getId();
        File apiDir = new File(resourcesStore + File.separator + datasetId);
        if (!apiDir.exists()) {
            apiDir.mkdirs();
        }

        File resourceFile = new File(apiDir + File.separator + resourceId);

        try {
            Files.write(resourceFile.toPath(), requestBody);
        } catch (IOException e) {
            throw new ServerErrorException("could not PUT resource: "+resourceId, 500);
        }

        return Response.noContent()
                       .build();
    }

    /**
     * deletes a resource
     *
     * @param resourceId the local identifier of a specific style
     * @return empty response (204)
     */
    @Path("/{resourceId}")
    @DELETE
    public Response deleteResource(@Auth Optional<User> optionalUser, @PathParam("resourceId") String resourceId,
                                @Context OgcApiApi dataset) {

        checkAuthorization(dataset.getData(), optionalUser);

        final String datasetId = dataset.getId();
        File apiDir = new File(resourcesStore + File.separator + datasetId);
        if (!apiDir.exists()) {
            apiDir.mkdirs();
        }

        File resourceFile = new File(apiDir + File.separator + resourceId);
        if (resourceFile.exists())
            resourceFile.delete();

        return Response.noContent()
                       .build();
    }

}

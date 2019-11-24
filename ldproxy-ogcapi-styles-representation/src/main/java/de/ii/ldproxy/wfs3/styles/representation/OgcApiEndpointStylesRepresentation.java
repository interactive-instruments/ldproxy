/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles.representation;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.wfs3.styles.StylesConfiguration;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
import de.ii.xtraplatform.server.CoreServerConfig;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class OgcApiEndpointStylesRepresentation implements OgcApiEndpointExtension {

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("styles")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/?\\w+/map$")
            .build();
    private static final ImmutableSet<OgcApiMediaType> API_MEDIA_TYPES = ImmutableSet.of(
            new ImmutableOgcApiMediaType.Builder()
                    .type(MediaType.TEXT_HTML_TYPE)
                    .build()
    );

    @Requires
    private KeyValueStore keyValueStore;
    @Requires
    private CoreServerConfig coreServerConfig;

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset, String subPath) {
        if (subPath.matches("^/?\\w+/map$"))
            return API_MEDIA_TYPES;

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        Optional<StylesConfiguration> stylesExtension = apiData.getExtension(StylesConfiguration.class);

        if (!stylesExtension.isPresent() || !stylesExtension.get()
                                                            .getMapsEnabled()) {
            throw new NotFoundException();
        }

        return true;
    }

    /**
     * creates a StyleView with the style.mustache template.
     * This view is a Openlayers Client, which represents a style of a wfs in a map.
     *
     * @param service     the service
     * @param styleId     the style which has to be represented in the client
     * @param wfs3Request the request
     * @return
     */
    @Path("/{styleId}/map")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getStyles(@Context OgcApiDataset service, @PathParam("styleId") String styleId, @Context OgcApiRequestContext wfs3Request) {

        Optional<StylesConfiguration> stylesExtension = getExtensionConfiguration(service.getData(), StylesConfiguration.class);
        if (!stylesExtension.isPresent() || !stylesExtension.get()
                                                            .getMapsEnabled()) {
            throw new NotFoundException();
        }

        String prefix = coreServerConfig.getExternalUrl();

        String styleUri = prefix + "/" + service.getData()
                                                .getId() + "/" + "styles" + "/" + styleId + "?f=mbs";

        StyleView styleView = new StyleView(styleUri, service, styleId);

        return Response.ok()
                       .entity(styleView)
                       .build();

    }
}

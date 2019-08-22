/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles.representation;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiContext;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiEndpointExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.wfs3.styles.StylesConfiguration;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
import de.ii.xtraplatform.server.CoreServerConfig;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class Wfs3EndpointStylesRepresentation implements OgcApiEndpointExtension {

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("maps")
            .build();
    private static final ImmutableSet<OgcApiMediaType> API_MEDIA_TYPES = ImmutableSet.of(
            new ImmutableOgcApiMediaType.Builder()
                    .main(MediaType.TEXT_HTML_TYPE)
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
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset) {
        return API_MEDIA_TYPES;
    }

    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData serviceData) {
        Optional<StylesConfiguration> stylesExtension = serviceData.getExtension(StylesConfiguration.class);

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
    @Path("/{styleId}")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getStyles(@Context OgcApiDataset service, @PathParam("styleId") String styleId, @Context OgcApiRequestContext wfs3Request) {

        Optional<StylesConfiguration> stylesExtension = getExtensionConfiguration(service.getData(), StylesConfiguration.class);
        if (!stylesExtension.isPresent() || !stylesExtension.get()
                                                            .getMapsEnabled()) {
            throw new NotFoundException();
        }

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                                                 .getChildStore(service.getId());
        List<String> styles = stylesStore.getKeys();
        // TODO EndpointStyles.getStyleDocument(stylesStore, styleId, "mbs");

        String prefix = coreServerConfig.getExternalUrl();

        String styleUri = prefix + "/" + service.getData()
                                                .getId() + "/" + "styles" + "/" + styleId;

        StyleView styleView = new StyleView(styleUri, service.getData()
                                                             .getId());

        return Response.ok()
                       .entity(styleView)
                       .build();

    }
}

/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.application.ImmutableOgcApiQueryInputCollections;
import de.ii.ldproxy.ogcapi.application.OgcApiQueriesHandlerCollections;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.auth.api.User;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class OgcApiEndpointCollections implements OgcApiEndpointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiEndpointCollections.class);
    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .addMethods(OgcApiContext.HttpMethods.GET)
            .subPathPattern("^/?$")
            .build();

    private final OgcApiExtensionRegistry extensionRegistry;
    //TODO
    @Requires
    private OgcApiQueriesHandlerCollections queryHandler;

    public OgcApiEndpointCollections(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset, String subPath) {
        if (subPath.matches("^/?$"))
            return extensionRegistry.getExtensionsForType(CollectionsFormatExtension.class)
                                    .stream()
                                    .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(dataset))
                                    .map(CollectionsFormatExtension::getMediaType)
                                    .collect(ImmutableSet.toImmutableSet());

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }


    @GET
    public Response getCollections(@Auth Optional<User> optionalUser, @Context OgcApiDataset api,
                                   @Context OgcApiRequestContext requestContext) {
        checkAuthorization(api.getData(), optionalUser);

        OgcApiQueriesHandlerCollections.OgcApiQueryInputCollections queryInput = new ImmutableOgcApiQueryInputCollections.Builder().build();

        return queryHandler.handle(OgcApiQueriesHandlerCollections.Query.COLLECTIONS, queryInput, requestContext);
    }
}

/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.application.ImmutableOgcApiQueryInputLandingPage;
import de.ii.ldproxy.ogcapi.application.OgcApiQueriesHandlerCommon;
import de.ii.ldproxy.ogcapi.application.OgcApiQueriesHandlerCommon.Query;
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
public class OgcApiEndpointLandingPage implements OgcApiEndpointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiEndpointLandingPage.class);
    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/?$")
            .build();

    private final OgcApiExtensionRegistry extensionRegistry;

    @Requires
    private OgcApiQueriesHandlerCommon queryHandler;

    public OgcApiEndpointLandingPage(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 dataset, String subPath) {
        if (subPath.matches("^/?$"))
            return extensionRegistry.getExtensionsForType(CommonFormatExtension.class)
                                    .stream()
                                    .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(dataset))
                                    .map(CommonFormatExtension::getMediaType)
                                    .collect(ImmutableSet.toImmutableSet());

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }

    @GET
    public Response getLandingPage(@Auth Optional<User> optionalUser, @Context OgcApiApi api,
                                   @Context OgcApiRequestContext requestContext) {
        checkAuthorization(api.getData(), optionalUser);

        boolean includeLinkHeader = getExtensionConfiguration(api.getData(), OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeLinkHeader)
                .orElse(false);

        OgcApiQueriesHandlerCommon.OgcApiQueryInputLandingPage queryInput = new ImmutableOgcApiQueryInputLandingPage.Builder()
                .includeLinkHeader(includeLinkHeader)
                .build();

        return queryHandler.handle(Query.LANDING_PAGE, queryInput, requestContext);
    }
}

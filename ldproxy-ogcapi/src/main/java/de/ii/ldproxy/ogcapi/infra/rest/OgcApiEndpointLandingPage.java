/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.application.ImmutableOgcApiQueryInputLandingPage;
import de.ii.ldproxy.ogcapi.application.OgcApiQueriesHandlerCommon;
import de.ii.ldproxy.ogcapi.application.OgcApiQueriesHandlerCommon.CommonQuery;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiEndpointExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.CommonFormatExtension;
import de.ii.xtraplatform.auth.api.User;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class OgcApiEndpointLandingPage implements OgcApiEndpointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiEndpointLandingPage.class);
    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("")
            .addMethods(OgcApiContext.HttpMethods.GET)
            .subPathPattern("^/?$")
            .build();

    private final OgcApiExtensionRegistry extensionRegistry;
    //TODO
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
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset, String subPath) {
        if (subPath.matches("^/?$"))
            return extensionRegistry.getExtensionsForType(CommonFormatExtension.class)
                                    .stream()
                                    .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(dataset))
                                    .map(CommonFormatExtension::getMediaType)
                                    .collect(ImmutableSet.toImmutableSet());

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }


    @GET
    public Response getLandingPage(@Auth Optional<User> optionalUser, @Context OgcApiDataset service,
                                   @Context OgcApiRequestContext requestContext) {
        checkAuthorization(service, optionalUser);

        OgcApiQueriesHandlerCommon.OgcApiQueryInputLandingPage queryInputDataset = new ImmutableOgcApiQueryInputLandingPage.Builder().build();

        return queryHandler.handle(CommonQuery.LANDING_PAGE, queryInputDataset, requestContext);
    }

    private void checkAuthorization(OgcApiDataset service, Optional<User> optionalUser) {
        if (service.getData()
                   .getSecured() && !optionalUser.isPresent()) {
            throw new NotAuthorizedException("Bearer realm=\"ldproxy\"");
            //throw new ClientErrorException(Response.Status.UNAUTHORIZED);
        }
    }
}

/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.collect.ImmutableList;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Component
@Provides
@Instantiate
public class OgcApiEndpointLandingPage extends OgcApiEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiEndpointLandingPage.class);
    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/?$")
            .build();
    private static final List<String> TAGS = ImmutableList.of("Capabilities");

    @Requires
    private OgcApiQueriesHandlerCommon queryHandler;

    public OgcApiEndpointLandingPage(@Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiCommonConfiguration.class);
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(CommonFormatExtension.class);
        return formats;
    }

    /*
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
     */

    @Override
    public OgcApiEndpointDefinition getDefinition(OgcApiApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableOgcApiEndpointDefinition.Builder definitionBuilder = new ImmutableOgcApiEndpointDefinition.Builder()
                    .apiEntrypoint("")
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_LANDING_PAGE);
            Set<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, "/");
            String operationSummary = "landing page";
            Optional<String> operationDescription = Optional.of("The landing page provides links to the API definition " +
                    "(link relations `service-desc` and `service-doc`), the Conformance declaration (path `/conformance`, " +
                    "link relation `conformance`), and other resources in the API.");
            String path = "/";
            ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                    .path(path);
            OgcApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilder.putOperations("GET", operation);
            definitionBuilder.putResources(path, resourceBuilder.build());
            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
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

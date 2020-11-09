/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.common.app.ImmutableQueryInputLandingPage;
import de.ii.ldproxy.ogcapi.common.app.QueriesHandlerCommon;
import de.ii.ldproxy.ogcapi.common.app.QueriesHandlerCommon.Query;
import de.ii.ldproxy.ogcapi.common.domain.CommonConfiguration;
import de.ii.ldproxy.ogcapi.common.domain.CommonFormatExtension;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.auth.domain.User;
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


@Component
@Provides
@Instantiate
public class EndpointLandingPage extends Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointLandingPage.class);
    private static final List<String> TAGS = ImmutableList.of("Capabilities");

    @Requires
    private QueriesHandlerCommon queryHandler;

    public EndpointLandingPage(@Requires ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CommonConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(CommonFormatExtension.class);
        return formats;
    }

    @Override
    public ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        int apiDataHash = apiData.hashCode();
        if (!apiDefinitions.containsKey(apiDataHash)) {
            ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                    .apiEntrypoint("")
                    .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_LANDING_PAGE);
            List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, "/");
            String operationSummary = "landing page";
            Optional<String> operationDescription = Optional.of("The landing page provides links to the API definition " +
                    "(link relations `service-desc` and `service-doc`), the Conformance declaration (path `/conformance`, " +
                    "link relation `conformance`), and other resources in the API.");
            String path = "/";
            ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                    .path(path);
            ApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilder.putOperations("GET", operation);
            definitionBuilder.putResources(path, resourceBuilder.build());
            apiDefinitions.put(apiDataHash, definitionBuilder.build());
        }

        return apiDefinitions.get(apiDataHash);
    }

    @GET
    public Response getLandingPage(@Auth Optional<User> optionalUser, @Context OgcApi api,
                                   @Context ApiRequestContext requestContext) {
        checkAuthorization(api.getData(), optionalUser);

        boolean includeLinkHeader = api.getData().getExtension(FoundationConfiguration.class)
                .map(FoundationConfiguration::getIncludeLinkHeader)
                .orElse(false);
        List<Link> additionalLinks = api.getData().getExtension(CommonConfiguration.class)
                                        .map(CommonConfiguration::getAdditionalLinks)
                                        .orElse(ImmutableList.of());

        QueriesHandlerCommon.QueryInputLandingPage queryInput = new ImmutableQueryInputLandingPage.Builder()
                .includeLinkHeader(includeLinkHeader)
                .additionalLinks(additionalLinks)
                .build();

        return queryHandler.handle(Query.LANDING_PAGE, queryInput, requestContext);
    }
}

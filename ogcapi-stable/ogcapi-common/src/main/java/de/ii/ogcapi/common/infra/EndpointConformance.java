/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.common.app.ImmutableQueryInputConformance;
import de.ii.ogcapi.common.app.QueriesHandlerCommonImpl;
import de.ii.ogcapi.common.app.QueriesHandlerCommonImpl.Query;
import de.ii.ogcapi.common.domain.CommonConfiguration;
import de.ii.ogcapi.common.domain.CommonFormatExtension;
import de.ii.ogcapi.common.domain.QueriesHandlerCommon;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.auth.domain.User;
import io.dropwizard.auth.Auth;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Conformance Declaration
 * @path /{apiId}/conformance
 * @formats {@link de.ii.ogcapi.common.domain.CommonFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointConformance extends Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointConformance.class);

    private static final List<String> TAGS = ImmutableList.of("Capabilities");

    private final QueriesHandlerCommon queryHandler;

    @Inject
    public EndpointConformance(ExtensionRegistry extensionRegistry,
                               QueriesHandlerCommon queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
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
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("conformance")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_CONFORMANCE);
        List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, "/conformance");
        String operationSummary = "conformance declaration";
        Optional<String> operationDescription = Optional.of("The URIs of all conformance classes supported by the server. " +
                "This information is provided to support 'generic' clients that want to access multiple " +
                "OGC API implementations - and not 'just' a specific API. For clients accessing only a single " +
                "API, this information is in general not relevant and the OpenAPI definition details the " +
                "required information about the API.");
        String path = "/conformance";
        ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                .path(path);
        ApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
        if (operation!=null)
            resourceBuilder.putOperations("GET", operation);
        definitionBuilder.putResources(path, resourceBuilder.build());

        return definitionBuilder.build();
    }

    @GET
    public Response getConformanceClasses(@Auth Optional<User> optionalUser, @Context OgcApi api,
                                          @Context ApiRequestContext requestContext) {

        QueriesHandlerCommonImpl.QueryInputConformance queryInput = new ImmutableQueryInputConformance.Builder()
                .from(getGenericQueryInput(api.getData()))
                .build();

        return queryHandler.handle(Query.CONFORMANCE_DECLARATION, queryInput, requestContext);
    }
}

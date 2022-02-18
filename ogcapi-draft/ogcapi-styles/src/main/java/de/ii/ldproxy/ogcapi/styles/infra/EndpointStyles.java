/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.foundation.domain.*;
import de.ii.ldproxy.ogcapi.styles.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

/**
 * fetch list of styles
 */
@Component
@Provides
@Instantiate
public class EndpointStyles extends Endpoint implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStyles.class);

    private static final List<String> TAGS = ImmutableList.of("Discover and fetch styles");

    private final QueriesHandlerStyles queryHandler;

    public EndpointStyles(@Requires ExtensionRegistry extensionRegistry,
                          @Requires QueriesHandlerStyles queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    @Override
    public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-styles-1/0.0/conf/core");
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(StylesFormatExtension.class);
        return formats;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("styles")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_STYLES);
        List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, "/styles");
        String operationSummary = "lists the available styles";
        Optional<String> operationDescription = Optional.of("This operation fetches the set of styles available. " +
                "For each style the id, a title, links to the stylesheet of the style in each supported encoding, " +
                "and the link to the metadata is provided.");
        String path = "/styles";
        ImmutableOgcApiResourceSet.Builder resourceBuilderSet = new ImmutableOgcApiResourceSet.Builder()
                .path(path)
                .subResourceType("Style");
        ApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
        if (operation!=null)
            resourceBuilderSet.putOperations("GET", operation);
        definitionBuilder.putResources(path, resourceBuilderSet.build());

        return definitionBuilder.build();
    }

    /**
     * fetch all available styles for the service
     *
     * @return all styles in a JSON styles object or an HTML page
     */
    @Path("/")
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_HTML})
    public Response getStyles(@Context OgcApi api, @Context ApiRequestContext requestContext) {
        QueriesHandlerStyles.QueryInputStyles queryInput = new ImmutableQueryInputStyles.Builder()
                .from(getGenericQueryInput(api.getData()))
                .build();

        return queryHandler.handle(QueriesHandlerStyles.Query.STYLES, queryInput, requestContext);
    }
}

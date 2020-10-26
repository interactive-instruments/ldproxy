/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.app.ImmutableQueryInputCollections;
import de.ii.ldproxy.ogcapi.collections.app.QueriesHandlerCollections;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsConfiguration;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsFormatExtension;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.Endpoint;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.FoundationConfiguration;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiResourceSet;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
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
public class EndpointCollections extends Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointCollections.class);
    private static final List<String> TAGS = ImmutableList.of("Discover data collections");

    @Requires
    private QueriesHandlerCollections queryHandler;

    public EndpointCollections(@Requires ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CollectionsConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(CollectionsFormatExtension.class);
        return formats;
    }

    @Override
    public ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        int apiDataHash = apiData.hashCode();
        if (!apiDefinitions.containsKey(apiDataHash)) {
            ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                    .apiEntrypoint("collections")
                    .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_COLLECTIONS);
            List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, "/collections");
            String operationSummary = "feature collections in the dataset '"+apiData.getLabel()+"'";
            Optional<String> operationDescription = Optional.of("The dataset is organized in feature collections. " +
                    "This resource provides information about and access to the feature collections.\n" +
                    "The response contains the list of collections. For each collection, a link to the items in the " +
                    "collection (path `/collections/{collectionId}/items`, link relation `items`) as well as key " +
                    "information about the collection.\n" +
                    "This information includes:\n\n" +
                    "* A local identifier for the collection that is unique for the dataset;\n" +
                    "* A list of coordinate reference systems (CRS) in which geometries may be returned by the server. " +
                    "The first CRS is the default coordinate reference system (the default is always WGS 84 with " +
                    "axis order longitude/latitude);\n" +
                    "* An optional title and description for the collection;\n" +
                    "* An optional extent that can be used to provide an indication of the spatial and temporal extent " +
                    "of the collection - typically derived from the data;\n" +
                    "* An optional indicator about the type of the items in the collection (the default value, " +
                    "if the indicator is not provided, is 'feature').");
            String path = "/collections";
            ImmutableOgcApiResourceSet.Builder resourceBuilder = new ImmutableOgcApiResourceSet.Builder()
                    .path(path)
                    .subResourceType("Collection");
            ApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilder.putOperations("GET", operation);
            definitionBuilder.putResources(path, resourceBuilder.build());

            apiDefinitions.put(apiDataHash, definitionBuilder.build());
        }

        return apiDefinitions.get(apiDataHash);
    }

    @GET
    public Response getCollections(@Auth Optional<User> optionalUser, @Context OgcApi api,
                                   @Context ApiRequestContext requestContext) {
        checkAuthorization(api.getData(), optionalUser);

        boolean includeLinkHeader = api.getData().getExtension(FoundationConfiguration.class)
                .map(FoundationConfiguration::getIncludeLinkHeader)
                .orElse(false);
        List<Link> additionalLinks = api.getData().getExtension(CollectionsConfiguration.class)
                                        .map(CollectionsConfiguration::getAdditionalLinks)
                                        .orElse(ImmutableList.of());

        QueriesHandlerCollections.QueryInputCollections queryInput = new ImmutableQueryInputCollections.Builder()
                .includeLinkHeader(includeLinkHeader)
                .additionalLinks(additionalLinks)
                .build();

        return queryHandler.handle(QueriesHandlerCollections.Query.COLLECTIONS, queryInput, requestContext);
    }
}

/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.application.ImmutableOgcApiQueryInputCollections;
import de.ii.ldproxy.ogcapi.collections.application.OgcApiQueriesHandlerCollections;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsFormatExtension;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiCollectionsConfiguration;
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

@Component
@Provides
@Instantiate
public class OgcApiEndpointCollections extends OgcApiEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiEndpointCollections.class);
    private static final List<String> TAGS = ImmutableList.of("Discover data collections");

    @Requires
    private OgcApiQueriesHandlerCollections queryHandler;

    public OgcApiEndpointCollections(@Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiCollectionsConfiguration.class);
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(CollectionsFormatExtension.class);
        return formats;
    }

    @Override
    public OgcApiEndpointDefinition getDefinition(OgcApiApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableOgcApiEndpointDefinition.Builder definitionBuilder = new ImmutableOgcApiEndpointDefinition.Builder()
                    .apiEntrypoint("collections")
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_COLLECTIONS);
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
            OgcApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilder.putOperations("GET", operation);
            definitionBuilder.putResources(path, resourceBuilder.build());

            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
    }

    @GET
    public Response getCollections(@Auth Optional<User> optionalUser, @Context OgcApiApi api,
                                   @Context OgcApiRequestContext requestContext) {
        checkAuthorization(api.getData(), optionalUser);

        boolean includeHomeLink = api.getData().getExtension(OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeHomeLink)
                .orElse(false);
        boolean includeLinkHeader = api.getData().getExtension(OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeLinkHeader)
                .orElse(false);

        OgcApiQueriesHandlerCollections.OgcApiQueryInputCollections queryInput = new ImmutableOgcApiQueryInputCollections.Builder()
                .includeHomeLink(includeHomeLink)
                .includeLinkHeader(includeLinkHeader)
                .build();

        return queryHandler.handle(OgcApiQueriesHandlerCollections.Query.COLLECTIONS, queryInput, requestContext);
    }
}

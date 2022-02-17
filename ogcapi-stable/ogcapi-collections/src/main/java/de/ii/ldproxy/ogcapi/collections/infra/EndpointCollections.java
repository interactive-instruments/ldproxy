/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.app.ImmutableQueryInputCollections;
import de.ii.ldproxy.ogcapi.collections.domain.QueriesHandlerCollections;
import de.ii.ldproxy.ogcapi.collections.app.QueriesHandlerCollectionsImpl;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsConfiguration;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsFormatExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.foundation.domain.Endpoint;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.FoundationValidator;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiResourceSet;
import de.ii.ldproxy.ogcapi.foundation.domain.Link;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
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

@Singleton
@AutoBind
public class EndpointCollections extends Endpoint implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointCollections.class);
    private static final List<String> TAGS = ImmutableList.of("Discover data collections");

    private final QueriesHandlerCollections queryHandler;

    @Inject
    public EndpointCollections(ExtensionRegistry extensionRegistry,
                               QueriesHandlerCollections queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    @Override
    public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-common-2/0.0/conf/collections");
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CollectionsConfiguration.class;
    }

    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, MODE apiValidation) {
        ValidationResult result = super.onStartup(apiData, apiValidation);

        if (apiValidation== MODE.NONE)
            return result;

        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
                .from(result)
                .mode(apiValidation);

        Optional<CollectionsConfiguration> config = apiData.getExtension(CollectionsConfiguration.class);
        if (config.isPresent()) {
            builder = FoundationValidator.validateLinks(builder, config.get().getAdditionalLinks(), "/collections");
        }

        return builder.build();
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(CollectionsFormatExtension.class);
        return formats;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
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
                "* A title and description for the collection;\n" +
                "* An indication of the spatial and temporal extent of the data in the collection;\n" +
                "* A list of coordinate reference systems (CRS) in which geometries may be returned by the server. " +
                "The first CRS is the default coordinate reference system (the default is always WGS 84 with " +
                "axis order longitude/latitude);\n" +
                "* The CRS in which the spatial geometries are stored in the data source (if data is requested in " +
                "this CRS, the geometries are returned without any coordinate conversion);\n" +
                "* An indicator about the type of the items in the collection (the default value is 'feature').");
        String path = "/collections";
        ImmutableOgcApiResourceSet.Builder resourceBuilder = new ImmutableOgcApiResourceSet.Builder()
                .path(path)
                .subResourceType("Collection");
        ApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
        if (operation!=null)
            resourceBuilder.putOperations("GET", operation);
        definitionBuilder.putResources(path, resourceBuilder.build());

        return definitionBuilder.build();
    }

    @GET
    public Response getCollections(@Auth Optional<User> optionalUser, @Context OgcApi api,
                                   @Context ApiRequestContext requestContext) {

        List<Link> additionalLinks = api.getData().getExtension(CollectionsConfiguration.class)
                                        .map(CollectionsConfiguration::getAdditionalLinks)
                                        .orElse(ImmutableList.of());

        QueriesHandlerCollectionsImpl.QueryInputCollections queryInput = new ImmutableQueryInputCollections.Builder()
                .from(getGenericQueryInput(api.getData()))
                .additionalLinks(additionalLinks)
                .build();

        return queryHandler.handle(QueriesHandlerCollectionsImpl.Query.COLLECTIONS, queryInput, requestContext);
    }
}

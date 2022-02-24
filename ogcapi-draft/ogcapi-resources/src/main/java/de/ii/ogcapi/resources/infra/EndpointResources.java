/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceSet;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ogcapi.resources.domain.ImmutableQueryInputResources;
import de.ii.ogcapi.resources.domain.QueriesHandlerResources;
import de.ii.ogcapi.resources.domain.ResourcesConfiguration;
import de.ii.ogcapi.resources.domain.ResourcesFormatExtension;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * fetch list of resources available in an API
 */
@Singleton
@AutoBind
public class EndpointResources extends Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointResources.class);

    private static final List<String> TAGS = ImmutableList.of("Discover and fetch other resources");

    private final I18n i18n;
    private final QueriesHandlerResources queryHandler;

    @Inject
    public EndpointResources(ExtensionRegistry extensionRegistry,
                             I18n i18n,
                             QueriesHandlerResources queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
        this.i18n = i18n;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<ResourcesConfiguration> resourcesExtension = apiData.getExtension(ResourcesConfiguration.class);
        Optional<StylesConfiguration> stylesExtension = apiData.getExtension(StylesConfiguration.class);

        if ((resourcesExtension.isPresent() && resourcesExtension.get()
                                                                 .isEnabled()) ||
                (stylesExtension.isPresent() && stylesExtension.get()
                                                               .getResourcesEnabled())) {
            return true;
        }
        return false;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ResourcesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(ResourcesFormatExtension.class);
        return formats;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("resources")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_RESOURCES);
        List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, "/resources");
        String operationSummary = "information about the available file resources";
        Optional<String> operationDescription = Optional.of("This operation fetches the set of file resources that have been " +
                "created and that may be used by reference, for example, in stylesheets. For each resource the id and " +
                "a link to the resource is provided.");
        String path = "/resources";
        ImmutableOgcApiResourceSet.Builder resourceBuilderSet = new ImmutableOgcApiResourceSet.Builder()
                .path(path)
                .subResourceType("File Resource");
        ApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
        if (operation!=null)
            resourceBuilderSet.putOperations("GET", operation);
        definitionBuilder.putResources(path, resourceBuilderSet.build());

        return definitionBuilder.build();
    }

    /**
     * fetch all available resources
     *
     * @return all resources in a JSON resources object
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_HTML})
    public Response getResources(@Context OgcApi api, @Context ApiRequestContext requestContext) {
        OgcApiDataV2 apiData = api.getData();
        QueriesHandlerResources.QueryInputResources queryInput = ImmutableQueryInputResources.builder()
                                                                                             .from(getGenericQueryInput(api.getData()))
                                                                                             .build();

        return queryHandler.handle(QueriesHandlerResources.Query.RESOURCES, queryInput, requestContext);
    }
}

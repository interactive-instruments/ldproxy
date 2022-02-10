/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.styles.domain.QueriesHandlerStyles;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ldproxy.resources.app.ImmutableResource;
import de.ii.ldproxy.resources.app.ImmutableResources;
import de.ii.ldproxy.resources.app.Resources;
import de.ii.ldproxy.resources.app.ResourcesLinkGenerator;
import de.ii.ldproxy.resources.domain.ImmutableQueryInputResource;
import de.ii.ldproxy.resources.domain.ImmutableQueryInputResources;
import de.ii.ldproxy.resources.domain.QueriesHandlerResources;
import de.ii.ldproxy.resources.domain.ResourcesConfiguration;
import de.ii.ldproxy.resources.domain.ResourcesFormatExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.API_RESOURCES_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

/**
 * fetch list of resources available in an API
 */
@Component
@Provides
@Instantiate
public class EndpointResources extends Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointResources.class);

    private static final List<String> TAGS = ImmutableList.of("Discover and fetch other resources");

    private final I18n i18n;
    private final QueriesHandlerResources queryHandler;

    public EndpointResources(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                             @Requires ExtensionRegistry extensionRegistry,
                             @Requires I18n i18n,
                             @Requires QueriesHandlerResources queryHandler) {
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
    @Path("/")
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

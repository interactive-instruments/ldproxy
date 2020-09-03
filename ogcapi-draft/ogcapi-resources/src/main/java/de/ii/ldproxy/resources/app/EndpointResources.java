/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
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
 * fetch list of styles or a style for the service
 */
@Component
@Provides
@Instantiate
public class EndpointResources extends Endpoint implements ConformanceClass {

    @Requires
    I18n i18n;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointResources.class);

    private static final List<String> TAGS = ImmutableList.of("Discover and fetch styles");

    private final java.nio.file.Path resourcesStore;

    public EndpointResources(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext, @Requires ExtensionRegistry extensionRegistry) throws IOException {
        super(extensionRegistry);
        this.resourcesStore = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), API_RESOURCES_DIR)
                                   .resolve("resources");
        if (Files.notExists(resourcesStore)) {
            if (Files.notExists(resourcesStore.getParent())) {
                Files.createDirectory(resourcesStore.getParent());
            }
            Files.createDirectory(resourcesStore);
        }
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-styles-1/0.0/conf/resources");
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<StylesConfiguration> stylesExtension = apiData.getExtension(StylesConfiguration.class);

        if (stylesExtension.isPresent() && stylesExtension.get()
                                                          .getResourcesEnabled()) {
            return true;
        }
        return false;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(ResourcesFormatExtension.class);
        return formats;
    }

    @Override
    public ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
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

            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
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
        final ResourcesLinkGenerator resourcesLinkGenerator = new ResourcesLinkGenerator();

        final String apiId = api.getId();
        File apiDir = new File(resourcesStore + File.separator + apiId);
        if (!apiDir.exists()) {
            apiDir.mkdirs();
        }

        Resources resources = ImmutableResources.builder()
            .resources(
                Arrays.stream(apiDir.listFiles())
                .filter(file -> !file.isHidden())
                .map(File::getName)
                .sorted()
                .map(filename -> ImmutableResource.builder()
                        .id(filename)
                        .link(resourcesLinkGenerator.generateResourceLink(requestContext.getUriCustomizer(), filename))
                        .build())
                .collect(Collectors.toList()))
            .links(new DefaultLinksGenerator()
                    .generateLinks(requestContext.getUriCustomizer(),
                            requestContext.getMediaType(),
                            requestContext.getAlternateMediaTypes(),
                            i18n,
                            requestContext.getLanguage()))
            .build();

        return getFormats().stream()
                .filter(format -> requestContext.getMediaType().matches(format.getMediaType().type()))
                .findAny()
                .map(ResourcesFormatExtension.class::cast)
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated.", requestContext.getMediaType().type())))
                .getResourcesResponse(resources, api, requestContext);
    }
}

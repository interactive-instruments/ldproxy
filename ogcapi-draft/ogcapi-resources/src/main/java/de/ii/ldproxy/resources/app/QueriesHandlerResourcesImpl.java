/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources.app;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadataFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleRepository;
import de.ii.ldproxy.ogcapi.styles.domain.StylesheetContent;
import de.ii.ldproxy.resources.domain.QueriesHandlerResources;
import de.ii.ldproxy.resources.domain.ResourceFormatExtension;
import de.ii.ldproxy.resources.domain.ResourcesFormatExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.API_RESOURCES_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

@Component
@Instantiate
@Provides
public class QueriesHandlerResourcesImpl implements QueriesHandlerResources {

    private final I18n i18n;
    private final ExtensionRegistry extensionRegistry;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
    private final java.nio.file.Path resourcesStore;

    public QueriesHandlerResourcesImpl(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                                       @Requires ExtensionRegistry extensionRegistry,
                                       @Requires I18n i18n) {
        this.extensionRegistry = extensionRegistry;
        this.i18n = i18n;
        this.resourcesStore = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), API_RESOURCES_DIR)
                                   .resolve("resources");
        if (!resourcesStore.toFile().exists()) {
            resourcesStore.toFile().mkdirs();
        }
        this.queryHandlers = ImmutableMap.of(
                Query.RESOURCES, QueryHandler.with(QueryInputResources.class, this::getResourcesResponse),
                Query.RESOURCE, QueryHandler.with(QueryInputResource.class, this::getResourceResponse)
        );
    }

    @Override
    public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    private Response getResourcesResponse(QueryInputResources queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();

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

        ResourcesFormatExtension format = extensionRegistry.getExtensionsForType(ResourcesFormatExtension.class)
                                                           .stream()
                                                           .filter(f -> requestContext.getMediaType().matches(f.getMediaType().type()))
                                                           .findAny()
                                                           .map(ResourcesFormatExtension.class::cast)
                                                           .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated.", requestContext.getMediaType().type())));

        Date lastModified = Arrays.stream(apiDir.listFiles())
                                  .filter(file -> !file.isHidden())
                                  .map(File::lastModified)
                                  .max(Comparator.naturalOrder())
                                  .map(Instant::ofEpochMilli)
                                  .map(Date::from)
                                  .orElse(Date.from(Instant.now()));
        EntityTag etag = getEtag(resources, Resources.FUNNEL, format);
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? resources.getLinks() : null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null), null)
                .entity(format.getResourcesEntity(resources, apiData, requestContext))
                .build();
    }

    private Response getResourceResponse(QueryInputResource queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        String resourceId = queryInput.getResourceId();

        final String apiId = api.getId();
        final java.nio.file.Path resourceFile = resourcesStore.resolve(apiId).resolve(resourceId);

        if (Files.notExists(resourceFile)) {
            throw new NotFoundException(MessageFormat.format("The file ''{0}'' does not exist.", resourceId));
        }

        final ResourceFormatExtension format = extensionRegistry.getExtensionsForType(ResourceFormatExtension.class)
                                                                .stream()
                                                                .filter(f -> requestContext.getMediaType().matches(f.getMediaType().type()))
                                                                .findAny()
                                                                .map(ResourceFormatExtension.class::cast)
                                                                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated.", requestContext.getMediaType().type())));
        ;
        final byte[] resource;
        try {
            resource = Files.readAllBytes(resourceFile);
        } catch (IOException e) {
            throw new ServerErrorException("resource could not be read: "+resourceId, 500);
        }

        // TODO: URLConnection content-type guessing doesn't seem to work well, maybe try Apache Tika
        String contentType = URLConnection.guessContentTypeFromName(resourceId);
        if (contentType==null) {
            try {
                contentType = URLConnection.guessContentTypeFromStream(ByteSource.wrap(resource).openStream());
            } catch (IOException e) {
                // nothing we can do here, just take the default
            }
        }
        if (contentType==null || contentType.isEmpty())
            contentType = "application/octet-stream";

        Date lastModified = getLastModified(resourceFile.toFile());
        EntityTag etag = getEtag(resource);
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(api, requestContext, null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null), null)
                .entity(format.getResourceEntity(resource, resourceId, apiData, requestContext))
                .type(contentType)
                .header("Content-Disposition", "inline; filename=\""+resourceId+"\"")
                .build();
    }
}

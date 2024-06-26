/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.HeaderCaching;
import de.ii.ogcapi.foundation.domain.HeaderContentDisposition;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.resources.domain.QueriesHandlerResources;
import de.ii.ogcapi.resources.domain.ResourceFormatExtension;
import de.ii.ogcapi.resources.domain.ResourcesFormatExtension;
import de.ii.xtraplatform.base.domain.ETag;
import de.ii.xtraplatform.base.domain.resiliency.AbstractVolatileComposed;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.blobs.domain.Blob;
import de.ii.xtraplatform.blobs.domain.ResourceStore;
import de.ii.xtraplatform.web.domain.LastModified;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@AutoBind
public class QueriesHandlerResourcesImpl extends AbstractVolatileComposed
    implements QueriesHandlerResources {

  private final I18n i18n;
  private final ExtensionRegistry extensionRegistry;
  private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
  private final ResourceStore resourcesStore;

  @Inject
  public QueriesHandlerResourcesImpl(
      ExtensionRegistry extensionRegistry,
      I18n i18n,
      ResourceStore blobStore,
      VolatileRegistry volatileRegistry) {
    super(QueriesHandlerResources.class.getSimpleName(), volatileRegistry, true);
    this.extensionRegistry = extensionRegistry;
    this.i18n = i18n;
    this.resourcesStore = blobStore.with(ResourcesBuildingBlock.STORE_RESOURCE_TYPE);
    this.queryHandlers =
        ImmutableMap.of(
            Query.RESOURCES,
                QueryHandler.with(QueryInputResources.class, this::getResourcesResponse),
            Query.RESOURCE, QueryHandler.with(QueryInputResource.class, this::getResourceResponse));

    onVolatileStart();

    addSubcomponent(resourcesStore);

    onVolatileStarted();
  }

  @Override
  public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
    return queryHandlers;
  }

  private Response getResourcesResponse(
      QueryInputResources queryInput, ApiRequestContext requestContext) {
    OgcApi api = requestContext.getApi();
    OgcApiDataV2 apiData = api.getData();
    ResourcesLinkGenerator resourcesLinkGenerator = new ResourcesLinkGenerator();
    long maxLastModified = 0L;
    ImmutableResources.Builder resourcesBuilder =
        ImmutableResources.builder().links(getLinks(requestContext, i18n));

    ResourcesFormatExtension format =
        extensionRegistry.getExtensionsForType(ResourcesFormatExtension.class).stream()
            .filter(f -> requestContext.getMediaType().matches(f.getMediaType().type()))
            .findAny()
            .map(ResourcesFormatExtension.class::cast)
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type {0} cannot be generated.",
                            requestContext.getMediaType().type())));

    try (Stream<Path> fileStream =
        resourcesStore.walk(
            Path.of(api.getId()),
            1,
            (path, attributes) -> attributes.isValue() && !attributes.isHidden())) {
      List<Path> files = fileStream.sorted().collect(Collectors.toList());

      for (Path file : files) {
        long lastModified = resourcesStore.lastModified(file);
        String filename = file.getFileName().toString();

        if (lastModified > maxLastModified) {
          maxLastModified = lastModified;
        }

        resourcesBuilder.addResources(
            ImmutableResource.builder()
                .id(filename)
                .link(
                    resourcesLinkGenerator.generateResourceLink(
                        requestContext.getUriCustomizer(), filename))
                .build());
      }
    } catch (IOException e) {
      throw new ServerErrorException("resources could not be read", 500);
    }

    Resources resources = resourcesBuilder.build();
    Date lastModified = LastModified.from(maxLastModified);
    EntityTag etag =
        !format.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
                || apiData
                    .getExtension(HtmlConfiguration.class)
                    .map(HtmlConfiguration::getSendEtags)
                    .orElse(false)
            ? ETag.from(resources, Resources.FUNNEL, format.getMediaType().label())
            : null;
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) return response.build();

    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader() ? resources.getLinks() : null,
            HeaderCaching.of(lastModified, etag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format("resources.%s", format.getMediaType().fileExtension())))
        .entity(format.getResourcesEntity(resources, apiData, requestContext))
        .build();
  }

  private Response getResourceResponse(
      QueryInputResource queryInput, ApiRequestContext requestContext) {
    OgcApi api = requestContext.getApi();
    OgcApiDataV2 apiData = api.getData();
    String resourceId = queryInput.getResourceId();

    final String apiId = api.getId();
    final java.nio.file.Path resourcePath = Path.of(apiId).resolve(resourceId);

    final ResourceFormatExtension format =
        extensionRegistry.getExtensionsForType(ResourceFormatExtension.class).stream()
            .filter(f -> requestContext.getMediaType().matches(f.getMediaType().type()))
            .findAny()
            .map(ResourceFormatExtension.class::cast)
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type {0} cannot be generated.",
                            requestContext.getMediaType().type())));

    try {
      Optional<Blob> resourceBlob = resourcesStore.get(resourcePath);

      if (resourceBlob.isEmpty()) {
        throw new NotFoundException(
            MessageFormat.format("The resource ''{0}'' does not exist.", resourceId));
      }

      Blob blob = resourceBlob.get();
      // TODO
      Date lastModified = LastModified.from(blob.lastModified());
      EntityTag eTag = blob.eTag();
      Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, eTag);
      if (Objects.nonNull(response)) return response.build();

      return prepareSuccessResponse(
              requestContext,
              null,
              HeaderCaching.of(lastModified, eTag, queryInput),
              null,
              HeaderContentDisposition.of(resourceId))
          .entity(format.getResourceEntity(blob.content(), resourceId, apiData, requestContext))
          .type(blob.contentType())
          .build();

    } catch (IOException e) {
      throw new ServerErrorException("resource could not be read: " + resourceId, 500);
    }
  }
}

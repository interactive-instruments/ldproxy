/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.app;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.DefaultLinksGenerator;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.routes.domain.ImmutableRouteDefinition;
import de.ii.ogcapi.routes.domain.ImmutableRoutes;
import de.ii.ogcapi.routes.domain.Route;
import de.ii.ogcapi.routes.domain.RouteDefinition;
import de.ii.ogcapi.routes.domain.RouteDefinitionFormatExtension;
import de.ii.ogcapi.routes.domain.RouteFormatExtension;
import de.ii.ogcapi.routes.domain.RouteRepository;
import de.ii.ogcapi.routes.domain.Routes;
import de.ii.ogcapi.routes.domain.RoutesFormatExtension;
import de.ii.ogcapi.routes.domain.RoutesLinksGenerator;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.store.domain.BlobStore;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.web.domain.LastModified;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class RouteRepositoryFiles implements RouteRepository, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(RouteRepositoryFiles.class);

  private final ExtensionRegistry extensionRegistry;
  private final BlobStore routesStore;
  private final I18n i18n;
  private final DefaultLinksGenerator defaultLinkGenerator;
  private final RoutesLinksGenerator routesLinkGenerator;
  private final ObjectMapper mapper;

  @Inject
  public RouteRepositoryFiles(BlobStore blobStore, ExtensionRegistry extensionRegistry, I18n i18n) {
    this.routesStore = blobStore.with(RoutingBuildingBlock.STORE_RESOURCE_TYPE);
    this.i18n = i18n;
    this.extensionRegistry = extensionRegistry;
    this.defaultLinkGenerator = new DefaultLinksGenerator();
    this.routesLinkGenerator = new RoutesLinksGenerator();
    this.mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new GuavaModule());
    mapper.configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public Stream<RoutesFormatExtension> getRoutesFormatStream(OgcApiDataV2 apiData) {
    return extensionRegistry.getExtensionsForType(RoutesFormatExtension.class).stream()
        .filter(format -> format.isEnabledForApi(apiData));
  }

  @Override
  public Stream<RouteFormatExtension> getRouteFormatStream(OgcApiDataV2 apiData) {
    return extensionRegistry.getExtensionsForType(RouteFormatExtension.class).stream()
        .filter(format -> format.isEnabledForApi(apiData));
  }

  @Override
  public Stream<RouteDefinitionFormatExtension> getRouteDefinitionFormatStream(
      OgcApiDataV2 apiData) {
    return extensionRegistry.getExtensionsForType(RouteDefinitionFormatExtension.class).stream()
        .filter(format -> format.isEnabledForApi(apiData));
  }

  private List<String> getRouteIds(OgcApiDataV2 apiData) {
    Set<String> formatExt =
        getRouteFormatStream(apiData)
            .map(RouteFormatExtension::getFileExtension)
            .collect(Collectors.toUnmodifiableSet());
    Path parent = getPathRoutes(apiData);

    try (Stream<Path> fileStream =
        routesStore.walk(
            parent,
            1,
            (path, attributes) ->
                attributes.isValue()
                    && !attributes.isHidden()
                    && !path.getFileName().toString().contains(".definition")
                    && formatExt.contains(
                        com.google.common.io.Files.getFileExtension(
                            path.getFileName().toString())))) {
      return fileStream
          .map(
              path ->
                  com.google.common.io.Files.getNameWithoutExtension(path.getFileName().toString()))
          .distinct()
          .sorted()
          .collect(ImmutableList.toImmutableList());
    } catch (IOException e) {
      LogContext.error(LOGGER, e, "Could not parse routes");
    }
    return ImmutableList.of();
  }

  @Override
  public Routes getRoutes(OgcApiDataV2 apiData, ApiRequestContext requestContext) {
    List<String> routeIds = getRouteIds(apiData);
    final RoutesLinksGenerator routesLinkGenerator = new RoutesLinksGenerator();
    ImmutableRoutes.Builder builder =
        new ImmutableRoutes.Builder()
            .addAllLinks(
                defaultLinkGenerator.generateLinks(
                    requestContext.getUriCustomizer(),
                    requestContext.getMediaType(),
                    requestContext.getAlternateMediaTypes(),
                    i18n,
                    requestContext.getLanguage()));
    routeIds.forEach(
        routeId ->
            builder.addLinks(
                routesLinkGenerator.generateRouteLink(
                    routeId,
                    getRouteName(apiData, routeId, requestContext.getLanguage()),
                    requestContext.getUriCustomizer(),
                    i18n,
                    requestContext.getLanguage())));
    return builder.build();
  }

  @Override
  public boolean routeExists(OgcApiDataV2 apiData, String routeId) {
    return getRouteFormatStream(apiData)
        .anyMatch(format -> exists(getPathRoute(apiData, routeId, format)));
  }

  private String getRouteName(OgcApiDataV2 apiData, String routeId, Optional<Locale> language) {
    return getRouteDefinition(apiData, routeId)
        .getInputs()
        .getName()
        .orElse(i18n.get("routeLinkFallback", language));
  }

  @Override
  public RouteDefinition getRouteDefinition(OgcApiDataV2 apiData, String routeId) {
    if (!routeExists(apiData, routeId)) {
      throw new NotFoundException(
          MessageFormat.format("The route ''{0}'' does not exist in this API.", routeId));
    }

    try {
      return mapper.readValue(
          routesStore.get(getPathDefinition(apiData, routeId)).get(), RouteDefinition.class);
    } catch (IOException e) {
      throw new InternalServerErrorException(
          MessageFormat.format(
              "Route definition file in route store is invalid for route ''{0}'' in API ''{1}''.",
              routeId, apiData.getId()),
          e);
    }
  }

  @Override
  public ImmutableValidationResult.Builder validate(
      ImmutableValidationResult.Builder builder, OgcApiDataV2 apiData) {
    List<String> routeIds = getRouteIds(apiData);
    Set<String> formatExt =
        getRouteFormatStream(apiData)
            .map(RouteFormatExtension::getFileExtension)
            .collect(Collectors.toUnmodifiableSet());

    routeIds.forEach(
        routeId -> {
          formatExt.forEach(
              ext -> {
                if (!exists(getPathRoute(apiData, routeId, ext)))
                  builder.addStrictErrors(
                      "Route Repository: Route '{}' is mssing in the format with extension '{}'.",
                      routeId,
                      ext);
              });
          if (!exists(getPathDefinition(apiData, routeId)))
            builder.addStrictErrors(
                "Route Repository: The definition of route '{}' is not available.", routeId);
        });

    return builder;
  }

  @Override
  public Route getRoute(OgcApiDataV2 apiData, String routeId, RouteFormatExtension format) {
    if (!routeExists(apiData, routeId)) {
      throw new NotFoundException(
          MessageFormat.format("The route ''{0}'' does not exist in this API.", routeId));
    }

    try {
      return mapper.readValue(
          routesStore.get(getPathRoute(apiData, routeId, format)).get(), Route.class);
    } catch (IOException e) {
      throw new InternalServerErrorException(
          MessageFormat.format(
              "Route file in route store is invalid for route ''{0}'' in API ''{1}''.",
              routeId, apiData.getId()),
          e);
    }
  }

  @Override
  public Date getLastModified(OgcApiDataV2 apiData, String routeId) {
    Path definitionFile = getPathDefinition(apiData, routeId);
    if (exists(definitionFile)) {
      try {
        return LastModified.from(routesStore.lastModified(definitionFile));
      } catch (IOException e) {
        // continue
      }
    }

    return null;
  }

  @Override
  public void writeRouteAndDefinition(
      OgcApiDataV2 apiData,
      String routeId,
      RouteFormatExtension format,
      byte[] route,
      RouteDefinition routeDefinition,
      List<Link> routeDefinitionLinks)
      throws IOException {
    Path routePath = getPathRoute(apiData, routeId, format);
    Path definitionPath = getPathDefinition(apiData, routeId);
    try {
      routesStore.put(routePath, new ByteArrayInputStream(route));
    } catch (IOException e) {
      routesStore.delete(routePath);
      throw e;
    }
    try {
      byte[] definition =
          this.mapper.writeValueAsBytes(
              new ImmutableRouteDefinition.Builder()
                  .from(routeDefinition)
                  .links(routeDefinitionLinks)
                  .build());
      routesStore.put(definitionPath, new ByteArrayInputStream(definition));
    } catch (IOException e) {
      routesStore.delete(routePath);
      routesStore.delete(definitionPath);
      throw e;
    }
  }

  @Override
  public void deleteRoute(OgcApiDataV2 apiData, String routeId) throws IOException {
    for (RouteFormatExtension format :
        getRouteFormatStream(apiData).collect(Collectors.toUnmodifiableList())) {
      routesStore.delete(getPathRoute(apiData, routeId, format));
    }
    routesStore.delete(getPathDefinition(apiData, routeId));
  }

  private Path getPathRoutes(OgcApiDataV2 apiData) {
    return Path.of(apiData.getId());
  }

  private Path getPathRoute(OgcApiDataV2 apiData, String routeId, RouteFormatExtension format) {
    return getPathRoute(apiData, routeId, format.getFileExtension());
  }

  private Path getPathRoute(OgcApiDataV2 apiData, String routeId, String ext) {
    return getPathRoutes(apiData).resolve(String.format("%s.%s", routeId, ext));
  }

  private Path getPathDefinition(OgcApiDataV2 apiData, String routeId) {
    return getPathRoutes(apiData).resolve(String.format("%s.definition.json", routeId));
  }

  private boolean exists(Path path) {
    try {
      return routesStore.has(path);
    } catch (IOException e) {
      return false;
    }
  }
}

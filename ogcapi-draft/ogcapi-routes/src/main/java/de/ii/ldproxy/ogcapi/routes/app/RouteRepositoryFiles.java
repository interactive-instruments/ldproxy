/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.app;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRouteDefinition;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRoutes;
import de.ii.ldproxy.ogcapi.routes.domain.Route;
import de.ii.ldproxy.ogcapi.routes.domain.RouteDefinition;
import de.ii.ldproxy.ogcapi.routes.domain.RouteDefinitionFormatExtension;
import de.ii.ldproxy.ogcapi.routes.domain.RouteFormatExtension;
import de.ii.ldproxy.ogcapi.routes.domain.RouteRepository;
import de.ii.ldproxy.ogcapi.routes.domain.Routes;
import de.ii.ldproxy.ogcapi.routes.domain.RoutesFormatExtension;
import de.ii.ldproxy.ogcapi.routes.domain.RoutesLinksGenerator;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.API_RESOURCES_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

@Component
@Provides
@Instantiate
public class RouteRepositoryFiles implements RouteRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteRepositoryFiles.class);

    private final ExtensionRegistry extensionRegistry;
    private final Path routesStore;
    private final I18n i18n;
    private final DefaultLinksGenerator defaultLinkGenerator;
    private final RoutesLinksGenerator routesLinkGenerator;
    private final ObjectMapper mapper;

    public RouteRepositoryFiles(@Context BundleContext bundleContext,
                                @Requires ExtensionRegistry extensionRegistry,
                                @Requires I18n i18n) throws IOException {
        this.routesStore = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), API_RESOURCES_DIR)
                                .resolve("routes");
        this.i18n = i18n;
        this.extensionRegistry = extensionRegistry;
        this.defaultLinkGenerator = new DefaultLinksGenerator();
        this.routesLinkGenerator = new RoutesLinksGenerator();
        Files.createDirectories(routesStore);
        this.mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new GuavaModule());
        mapper.configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public Stream<RoutesFormatExtension> getRoutesFormatStream(OgcApiDataV2 apiData) {
        return extensionRegistry.getExtensionsForType(RoutesFormatExtension.class)
            .stream()
            .filter(format -> format.isEnabledForApi(apiData));
    }

    @Override
    public Stream<RouteFormatExtension> getRouteFormatStream(OgcApiDataV2 apiData) {
        return extensionRegistry.getExtensionsForType(RouteFormatExtension.class)
            .stream()
            .filter(format -> format.isEnabledForApi(apiData));
    }

    @Override
    public Stream<RouteDefinitionFormatExtension> getRouteDefinitionFormatStream(OgcApiDataV2 apiData) {
        return extensionRegistry.getExtensionsForType(RouteDefinitionFormatExtension.class)
            .stream()
            .filter(format -> format.isEnabledForApi(apiData));
    }

    private List<String> getRouteIds(OgcApiDataV2 apiData) {
        File dir = getPathRoutes(apiData).toFile();
        if (!dir.exists())
            dir.getParentFile().mkdirs();
        Set<String> formatExt = getRouteFormatStream(apiData)
            .map(RouteFormatExtension::getFileExtension)
            .collect(Collectors.toUnmodifiableSet());
        return Arrays.stream(Objects.requireNonNullElse(dir.listFiles(), ImmutableList.of().toArray(File[]::new)))
            .filter(file -> !file.isHidden())
            .filter(file -> !file.getName().contains(".definition"))
            .filter(file -> formatExt.contains(com.google.common.io.Files.getFileExtension(file.getName())))
            .map(file -> com.google.common.io.Files.getNameWithoutExtension(file.getName()))
            .distinct()
            .sorted()
            .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Routes getRoutes(OgcApiDataV2 apiData, ApiRequestContext requestContext) {
        List<String> routeIds = getRouteIds(apiData);
        final RoutesLinksGenerator routesLinkGenerator = new RoutesLinksGenerator();
        ImmutableRoutes.Builder builder = new ImmutableRoutes.Builder()
            .addAllLinks(defaultLinkGenerator.generateLinks(requestContext.getUriCustomizer(),
                                                            requestContext.getMediaType(),
                                                            requestContext.getAlternateMediaTypes(),
                                                            i18n,
                                                            requestContext.getLanguage()));
        routeIds.forEach(routeId -> builder.addLinks(routesLinkGenerator.generateRouteLink(routeId, getRouteName(apiData, routeId, requestContext.getLanguage()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage())));
        return builder.build();
    }

    @Override
    public boolean routeExists(OgcApiDataV2 apiData, String routeId) {
        return getRouteFormatStream(apiData).anyMatch(format -> getPathRoute(apiData, routeId, format).toFile().exists());
    }

    private String getRouteName(OgcApiDataV2 apiData, String routeId, Optional<Locale> language) {
        return getRouteDefinition(apiData, routeId).getInputs().getName().orElse(i18n.get("routeLinkFallback", language));
    }

    @Override
    public RouteDefinition getRouteDefinition(OgcApiDataV2 apiData, String routeId) {
        if (!routeExists(apiData, routeId)) {
            throw new NotFoundException(MessageFormat.format("The route ''{0}'' does not exist in this API.", routeId));
        }

        try {
            return mapper.readValue(getPathDefinition(apiData, routeId).toFile(), RouteDefinition.class);
        } catch (IOException e) {
            throw new InternalServerErrorException(MessageFormat.format("Route definition file in route store is invalid for route ''{0}'' in API ''{1}''.", routeId, apiData.getId()), e);
        }
    }

    @Override
    public ImmutableValidationResult.Builder validate(ImmutableValidationResult.Builder builder, OgcApiDataV2 apiData) {
        File dir = getPathRoutes(apiData).toFile();
        if (!dir.exists())
            return builder;
        Set<String> formatExt = getRouteFormatStream(apiData)
            .map(RouteFormatExtension::getFileExtension)
            .collect(Collectors.toUnmodifiableSet());
        List<String> routeIds = Arrays.stream(Objects.requireNonNullElse(dir.listFiles(), ImmutableList.of().toArray(File[]::new)))
            .filter(file -> !file.isHidden())
            .filter(file -> !file.getName().contains(".definition"))
            .map(file -> com.google.common.io.Files.getNameWithoutExtension(file.getName()))
            .distinct()
            .sorted()
            .collect(Collectors.toUnmodifiableList());

        routeIds.forEach(routeId -> {
            formatExt.forEach(ext -> {
                if (!Files.exists(getPathRoutes(apiData).resolve(routeId+"."+ext)))
                    builder.addStrictErrors("Route Repository: Route '{}' is mssing in the format with extension '{}'.", routeId, ext);
            });
            if (!Files.exists(getPathRoutes(apiData).resolve(routeId+".definition.json")))
                builder.addStrictErrors("Route Repository: The definition of route '{}' is not available.", routeId);
        });

        return builder;
    }

    @Override
    public Route getRoute(OgcApiDataV2 apiData, String routeId, RouteFormatExtension format) {
        if (!routeExists(apiData, routeId)) {
            throw new NotFoundException(MessageFormat.format("The route ''{0}'' does not exist in this API.", routeId));
        }

        try {
            return mapper.readValue(getPathRoute(apiData, routeId, format).toFile(), Route.class);
        } catch (IOException e) {
            throw new InternalServerErrorException(MessageFormat.format("Route file in route store is invalid for route ''{0}'' in API ''{1}''.", routeId, apiData.getId()), e);
        }
    }

    @Override
    public Date getLastModified(OgcApiDataV2 apiData, String routeId) {
        File definitionFile = getPathDefinition(apiData, routeId).toFile();
        if (definitionFile.exists())
            return Date.from(Instant.ofEpochMilli(definitionFile.lastModified()));

        return null;
    }

    @Override
    public void writeRouteAndDefinition(OgcApiDataV2 apiData, String routeId, RouteFormatExtension format, byte[] route, RouteDefinition routeDefinition, List<Link> routeDefinitionLinks) throws IOException {
        Path routePath = getPathRoute(apiData, routeId, format);
        Path definitionPath = getPathDefinition(apiData, routeId);
        try {
            Files.write(routePath, route);
        } catch (IOException e) {
            deleteFile(routePath);
            throw e;
        }
        try {
            byte[] definition = this.mapper
                .writeValueAsBytes(new ImmutableRouteDefinition.Builder()
                                       .from(routeDefinition)
                                       .links(routeDefinitionLinks)
                                       .build());
            Files.write(definitionPath, definition);
        } catch (IOException e) {
            deleteFile(routePath);
            deleteFile(definitionPath);
            throw e;
        }
    }

    @Override
    public void deleteRoute(OgcApiDataV2 apiData, String routeId) throws IOException {
        for (RouteFormatExtension format: getRouteFormatStream(apiData).collect(Collectors.toUnmodifiableList())) {
            deleteFile(getPathRoute(apiData, routeId, format));
        }
        deleteFile(getPathDefinition(apiData, routeId));
    }

    private void deleteFile(Path path) throws IOException {
        if (Files.exists(path))
            Files.delete(path);
    }

    private Path getPathRoutes(OgcApiDataV2 apiData) {
        return routesStore.resolve(apiData.getId());
    }

    private Path getPathRoute(OgcApiDataV2 apiData, String routeId, RouteFormatExtension format) {
        Path dir = getPathRoutes(apiData);
        if (!dir.toFile().exists())
            dir.toFile().mkdirs();
        return dir.resolve(String.format("%s.%s", routeId, format.getFileExtension()));
    }

    private Path getPathDefinition(OgcApiDataV2 apiData, String routeId) {
        Path dir = getPathRoutes(apiData);
        if (!dir.toFile().exists())
            dir.toFile().mkdirs();
        return dir.resolve(String.format("%s.definition.json", routeId));
    }
}
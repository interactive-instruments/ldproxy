/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import static de.ii.ldproxy.ogcapi.foundation.domain.FoundationConfiguration.API_RESOURCES_DIR;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.I18n;
import de.ii.ldproxy.ogcapi.foundation.domain.Link;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableStyleEntry;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableStyleMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableStyles;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableStylesheetMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.StyleEntry;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadataFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleRepository;
import de.ii.ldproxy.ogcapi.styles.domain.Styles;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.StylesFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StylesLinkGenerator;
import de.ii.ldproxy.ogcapi.styles.domain.StylesheetContent;
import de.ii.ldproxy.ogcapi.styles.domain.StylesheetMetadata;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class StyleRepositoryFiles implements StyleRepository, AppLifeCycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleRepositoryFiles.class);

    private final ExtensionRegistry extensionRegistry;
    private final java.nio.file.Path stylesStore;
    private final I18n i18n;
    private final DefaultLinksGenerator defaultLinkGenerator;
    private final ObjectMapper patchMapperLenient;
    private final ObjectMapper patchMapperStrict;
    private final ObjectMapper metadataMapper;
    private final AppContext appContext;
    private final FeaturesCoreProviders providers;
    private final EntityRegistry entityRegistry;

    @Inject
    public StyleRepositoryFiles(AppContext appContext,
                                ExtensionRegistry extensionRegistry,
                                I18n i18n,
                                FeaturesCoreProviders providers,
                                EntityRegistry entityRegistry) {
        this.stylesStore = appContext.getDataDir()
            .resolve(API_RESOURCES_DIR)
            .resolve("styles");
        this.i18n = i18n;
        this.extensionRegistry = extensionRegistry;
        this.appContext = appContext;
        this.providers = providers;
        this.entityRegistry = entityRegistry;
        this.defaultLinkGenerator = new DefaultLinksGenerator();
        this.patchMapperLenient = new ObjectMapper();
        patchMapperLenient.registerModule(new Jdk8Module());
        patchMapperLenient.registerModule(new GuavaModule());
        patchMapperLenient.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        patchMapperLenient.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        this.patchMapperStrict = new ObjectMapper();
        patchMapperStrict.registerModule(new Jdk8Module());
        patchMapperStrict.registerModule(new GuavaModule());
        patchMapperStrict.configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true);
        patchMapperStrict.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        patchMapperStrict.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        this.metadataMapper = new ObjectMapper();
        metadataMapper.registerModule(new Jdk8Module());
        metadataMapper.registerModule(new GuavaModule());
        metadataMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void onStart() {
        try {
            Files.createDirectories(stylesStore);
        } catch (IOException e) {
            LOGGER.error("Could not create styles repository: " + e.getMessage());
        }
    }

    public Stream<StylesFormatExtension> getStylesFormatStream(OgcApiDataV2 apiData, Optional<String> collectionId) {
        return extensionRegistry.getExtensionsForType(StylesFormatExtension.class)
                                .stream()
                                .filter(format -> collectionId.isPresent()
                                        ? format.isEnabledForApi(apiData, collectionId.get())
                                        : format.isEnabledForApi(apiData));
    }

    public Stream<StyleFormatExtension> getStyleFormatStream(OgcApiDataV2 apiData, Optional<String> collectionId) {
        return extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                                .stream()
                                .filter(format -> collectionId.isPresent()
                                        ? format.isEnabledForApi(apiData, collectionId.get())
                                        : format.isEnabledForApi(apiData));
    }

    public Stream<StyleMetadataFormatExtension> getStyleMetadataFormatStream(OgcApiDataV2 apiData, Optional<String> collectionId) {
        return extensionRegistry.getExtensionsForType(StyleMetadataFormatExtension.class)
                                .stream()
                                .filter(format -> collectionId.isPresent()
                                        ? format.isEnabledForApi(apiData, collectionId.get())
                                        : format.isEnabledForApi(apiData));
    }

    @Override
    public List<ApiMediaType> getStylesheetMediaTypes(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId) {
        return getStyleFormatStream(apiData, collectionId)
                .filter(styleFormat -> stylesheetExists(apiData, collectionId, styleId, styleFormat))
                .map(StyleFormatExtension::getMediaType)
                .collect(Collectors.toList());
    }

    @Override
    public Date getStyleLastModified(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId) {
        return getStyleFormatStream(apiData, collectionId)
                .filter(styleFormat -> stylesheetExists(apiData, collectionId, styleId, styleFormat))
                .map(styleFormat -> getStylesheetLastModified(apiData, collectionId, styleId, styleFormat, true))
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    @Override
    public Date getStylesheetLastModified(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, StyleFormatExtension styleFormat, boolean includeDerived) {
        // a stylesheet exists, if we have a stylesheet document or if we can derive one
        File stylesheetFile = getPathStyle(apiData, collectionId, styleId, styleFormat).toFile();
        if (stylesheetFile.exists())
            return Date.from(Instant.ofEpochMilli(stylesheetFile.lastModified()));

        if (includeDerived && collectionId.isPresent() && deriveCollectionStylesEnabled(apiData, collectionId.get())) {
            stylesheetFile = getPathStyle(apiData, Optional.empty(), styleId, styleFormat).toFile();
            if (stylesheetFile.exists())
                return Date.from(Instant.ofEpochMilli(stylesheetFile.lastModified()));
        }

        return null;
    }

    @Override
    public Styles getStyles(OgcApiDataV2 apiData, Optional<String> collectionId, ApiRequestContext requestContext) {
        File dir = getPathStyles(apiData, collectionId).toFile();
        if (!dir.exists())
            dir.getParentFile().mkdirs();
        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();
        Map<String, StyleFormatExtension> formatMap = getStyleFormatStream(apiData, collectionId).filter(format -> !format.getDerived())
                                                                                                 .collect(Collectors.toUnmodifiableMap(format -> format.getFileExtension(), format -> format));
        List<StyleEntry> styleEntries = Arrays.stream(Objects.requireNonNullElse(dir.listFiles(), ImmutableList.of().toArray(File[]::new)))
                                              .filter(file -> !file.isHidden())
                                              .filter(file -> formatMap.containsKey(com.google.common.io.Files.getFileExtension(file.getName())))
                                              .map(file -> com.google.common.io.Files.getNameWithoutExtension(file.getName()))
                                              .distinct()
                                              .sorted()
                                              .map(styleId -> {
                                                  ImmutableStyleEntry.Builder builder = ImmutableStyleEntry.builder()
                                                                                                           .id(styleId)
                                                                                                           .title(getTitle(apiData, collectionId, styleId, requestContext).orElse(styleId));

                                                  Date lastModified = getStyleLastModified(apiData, collectionId, styleId);
                                                  if (Objects.nonNull(lastModified))
                                                      builder.lastModified(lastModified);

                                                  List<ApiMediaType> mediaTypes = getStylesheetMediaTypes(apiData, collectionId, styleId);
                                                  builder.links(stylesLinkGenerator.generateStyleLinks(requestContext.getUriCustomizer(),
                                                                                                       styleId,
                                                                                                       mediaTypes,
                                                                                                       i18n,
                                                                                                       requestContext.getLanguage()));
                                                  if (collectionId.isPresent()) {
                                                      List<ApiMediaType> additionalMediaTypes =
                                                              getStyleFormatStream(apiData, collectionId).filter(format -> format.canDeriveCollectionStyle()
                                                                      && !mediaTypes.contains(format.getMediaType())
                                                                      && willDeriveStylesheet(apiData, collectionId, styleId, format))
                                                                                                         .map(StyleFormatExtension::getMediaType)
                                                                                                         .collect(Collectors.toUnmodifiableList());
                                                      if (!additionalMediaTypes.isEmpty()) {
                                                          builder.addAllLinks(stylesLinkGenerator.generateStyleLinks(requestContext.getUriCustomizer(),
                                                                                                               styleId,
                                                                                                               additionalMediaTypes,
                                                                                                               i18n,
                                                                                                               requestContext.getLanguage()));
                                                      }
                                                  }
                                                  builder.addLinks(stylesLinkGenerator.generateStyleMetadataLink(requestContext.getUriCustomizer(),
                                                                                                                 styleId,
                                                                                                                 i18n,
                                                                                                                 requestContext.getLanguage()));
                                                  return builder.build();
                                              })
                                              .collect(Collectors.toList());
        Styles styles = ImmutableStyles.builder()
                                       .styles(styleEntries)
                                       .lastModified(styleEntries.stream()
                                                                 .map(StyleEntry::getLastModified)
                                                                 .map(Optional::get)
                                                                 .max(Comparator.naturalOrder()))
                                       .links(new DefaultLinksGenerator()
                                                      .generateLinks(requestContext.getUriCustomizer(),
                                                                     requestContext.getMediaType(),
                                                                     requestContext.getAlternateMediaTypes(),
                                                                     i18n,
                                                                     requestContext.getLanguage()))
                                       .build();
        return styles;
    }

    @Override
    public Styles getStyles(OgcApiDataV2 apiData, Optional<String> collectionId, ApiRequestContext requestContext, boolean includeDerived) {
        Styles styles = getStyles(apiData, collectionId, requestContext);
        if (!includeDerived || collectionId.isEmpty() || !deriveCollectionStylesEnabled(apiData, collectionId.get()))
            return styles;

        Set<String> existingStyleIds = styles.getStyles().stream().map(StyleEntry::getId).collect(Collectors.toUnmodifiableSet());
        Styles rootStyles = getStyles(apiData, Optional.empty(), requestContext);
        return ImmutableStyles.builder()
                              .from(styles)
                              .addAllStyles(rootStyles.getStyles()
                                                      .stream()
                                                      .filter(styleEntry -> !existingStyleIds.contains(styleEntry.getId()))
                                                      .map(styleEntry -> deriveCollectionStyleEntry(apiData, styleEntry, collectionId.get(), requestContext))
                                                      .filter(Optional::isPresent)
                                                      .map(Optional::get)
                                                      .collect(Collectors.toUnmodifiableList()))
                              .build();
    }

    public boolean stylesheetExists(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, StyleFormatExtension styleFormat) {
        // exclude derived stylesheets
        return stylesheetExists(apiData, collectionId, styleId, styleFormat, false);
    }

    public boolean stylesheetExists(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, StyleFormatExtension styleFormat, boolean includeDerived) {
        // a stylesheet exists, if we have a stylesheet document or if we can derive one
        return getPathStyle(apiData, collectionId, styleId, styleFormat).toFile().exists() ||
               (includeDerived && willDeriveStylesheet(apiData, collectionId, styleId, styleFormat));
    }

    private boolean willDeriveStylesheet(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, StyleFormatExtension styleFormat) {
        // for specific style encodings, we derive a style for a single feature collection from a multi-collection stylesheet, if this capability is not switched of for the collection
        try {
            return collectionId.isPresent() &&
                    deriveCollectionStylesEnabled(apiData, collectionId.get()) &&
                    getPathStyle(apiData, Optional.empty(), styleId, styleFormat).toFile().exists() &&
                    styleFormat.canDeriveCollectionStyle() &&
                    styleFormat.deriveCollectionStyle(new StylesheetContent(getPathStyle(apiData, Optional.empty(), styleId, styleFormat)), apiData, collectionId.get(), styleId).isPresent();
        } catch (IOException e) {
            LOGGER.error("Could not derive stylesheet for style ''{0}'' in collection ''{1}'' in API ''{2}'' for style encoding ''{3}''", styleId, collectionId, apiData.getId(), styleFormat.getMediaType().label());
        }
        return false;
    }

    public StylesheetContent getStylesheet(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, StyleFormatExtension styleFormat, ApiRequestContext requestContext) {
        if (!stylesheetExists(apiData, collectionId, styleId, styleFormat)) {
            boolean styleExists = Objects.requireNonNull(getPathStyle(apiData, collectionId, styleId, styleFormat).toFile().getParentFile().listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.matches("^" + styleId + "\\..*");
                }
            })).length > 0;

            if (styleExists) {
                if (collectionId.isPresent())
                    throw new NotAcceptableException(MessageFormat.format("The style ''{0}'' is not available in the requested format for collection ''{1}''.", styleId, collectionId.get()));
                throw new NotAcceptableException(MessageFormat.format("The style ''{0}'' is not available in the requested format.", styleId));
            }

            if (collectionId.isPresent())
                throw new NotFoundException(MessageFormat.format("The style ''{0}'' does not exist in this API for collection ''{1}''.", styleId, collectionId.get()));
            throw new NotFoundException(MessageFormat.format("The style ''{0}'' does not exist in this API.", styleId));
        }

        try {
            // TODO parse first?
            return new StylesheetContent(getPathStyle(apiData, collectionId, styleId, styleFormat));
        } catch (IOException e) {
            if (collectionId.isPresent())
                throw new RuntimeException(MessageFormat.format("The style ''{0}'' could not be parsed for collection ''{1}''.", styleId, collectionId.get()), e);
            throw new RuntimeException(MessageFormat.format("The style ''{0}'' could not be parsed.", styleId), e);
        }
    }

    @Override
    public StylesheetContent getStylesheet(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, StyleFormatExtension styleFormat, ApiRequestContext requestContext, boolean includeDerived) {
        if (collectionId.isEmpty() || stylesheetExists(apiData, collectionId, styleId, styleFormat))
            return getStylesheet(apiData, collectionId, styleId, styleFormat, requestContext);

        if (stylesheetExists(apiData, collectionId, styleId, styleFormat, true)) {
            Optional<StylesheetContent> stylesheet = styleFormat.deriveCollectionStyle(getStylesheet(apiData, Optional.empty(), styleId, styleFormat, requestContext), apiData, collectionId.get(), styleId);
            if (stylesheet.isPresent())
                return stylesheet.get();
        }

        throw new NotFoundException(MessageFormat.format("The style ''{0}'' does not exist in this API for collection ''{1}''.", styleId, collectionId.get()));
    }

    @Override
    public StyleMetadata getStyleMetadata(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, ApiRequestContext requestContext) {
        if (getStylesheetMediaTypes(apiData,collectionId, styleId).isEmpty()) {
            if (collectionId.isEmpty())
                throw new NotFoundException(MessageFormat.format("The style ''{0}'' does not exist in this API.", styleId));
            throw new NotFoundException(MessageFormat.format("The style ''{0}'' does not exist in this API for collection ''{1}''.", styleId, collectionId.get()));
        }

        // derive standard links (self/alternate)
        List<Link> links = defaultLinkGenerator.generateLinks(requestContext.getUriCustomizer(),
                                                              requestContext.getMediaType(),
                                                              requestContext.getAlternateMediaTypes(),
                                                              i18n,
                                                              requestContext.getLanguage());

        // Derive standard metadata from the style
        // TODO move outside of this specific repository implementation
        List<StylesheetMetadata> stylesheets = deriveStylesheetMetadata(apiData, collectionId, styleId, requestContext);
        Optional<String> title = getTitle(apiData, collectionId, styleId, requestContext);

        Optional<StyleFormatExtension> format = getStyleFormatStream(apiData, collectionId).filter(f -> f.canDeriveMetadata())
                                                                                           .filter(f -> stylesheetExists(apiData, collectionId, styleId, f, true))
                                                                                           .findAny();
        ImmutableStyleMetadata.Builder builder = ImmutableStyleMetadata.builder()
                                                                       .id(Optional.ofNullable(styleId))
                                                                       .title(title.orElse(styleId))
                                                                       .stylesheets(stylesheets);
        if (format.isPresent()) {
            builder.layers(format.get().deriveLayerMetadata(getStylesheet(apiData, collectionId, styleId, format.get(), requestContext, true), apiData, providers, entityRegistry));
        }
        StyleMetadata metadata = builder.build();

        Optional<JsonMergePatch> patch = getStyleMetadataPatch(apiData, collectionId, styleId);
        if (patch.isEmpty() && collectionId.isPresent() && deriveCollectionStylesEnabled(apiData, collectionId.get())) {
            patch = getStyleMetadataPatch(apiData, Optional.empty(), styleId);
        }
        if (patch.isPresent()) {
            try {
                metadata = metadataMapper.treeToValue(patch.get().apply(metadataMapper.valueToTree(metadata)), StyleMetadata.class);
            } catch (JsonProcessingException | JsonPatchException e) {
                if (collectionId.isPresent())
                    throw new InternalServerErrorException(MessageFormat.format("Style metadata file in styles store is invalid for style ''{0}'' in collection ''{1}'' in API ''{2}''.", styleId, collectionId.get(), apiData.getId()), e);
                throw new InternalServerErrorException(MessageFormat.format("Style metadata file in styles store is invalid for style ''{0}'' in API ''{1}''.", styleId, apiData.getId()), e);
            }
        }

        metadata = ImmutableStyleMetadata.builder()
                                         .from(metadata)
                                         .addAllLinks(links)
                                         .build();

        URICustomizer uriCustomizer = new URICustomizer(appContext.getUri().resolve("rest/services")).ensureLastPathSegments(apiData.getSubPath().toArray(String[]::new));
        String serviceUrl = uriCustomizer.toString();
        return metadata.replaceParameters(serviceUrl);
    }

    @Override
    public Optional<JsonMergePatch> getStyleMetadataPatch(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId) {
        File patchFile = getPathMetadata(apiData, collectionId, styleId).toFile();
        if (!patchFile.exists())
            return Optional.empty();

        try {
            final byte[] patchContent = Files.readAllBytes(patchFile.toPath());

            try {
                // parse patch file
                return Optional.of(patchMapperLenient.readValue(patchContent, JsonMergePatch.class));
            } catch (IOException e) {
                if (collectionId.isPresent())
                    throw new InternalServerErrorException(MessageFormat.format("Style metadata file in styles store is invalid for style ''{0}'' in collection ''{1}'' in API ''{2}''.", styleId, collectionId.get(), apiData.getId()), e);
                throw new InternalServerErrorException(MessageFormat.format("Style metadata file in styles store is invalid for style ''{0}'' in API ''{1}''.", styleId, apiData.getId()), e);
            }
        } catch (IOException e) {
            if (collectionId.isPresent())
                throw new InternalServerErrorException(MessageFormat.format("Style metadata could not be read for style ''{0}'' in collection ''{1}'' in API ''{2}''.", styleId, collectionId.get(), apiData.getId()), e);
            throw new InternalServerErrorException(MessageFormat.format("Style metadata could not be read for style ''{0}'' in API ''{1}''.", styleId, apiData.getId()), e);
        }
    }

    @Override
    public byte[] updateStyleMetadataPatch(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, byte[] additionalPatchContent, boolean strict) {

        ObjectMapper mapper = strict ? patchMapperStrict : patchMapperLenient;

        File patchFile = getPathMetadata(apiData, collectionId, styleId).toFile();
        if (!patchFile.exists())
            return additionalPatchContent;

        JsonMergePatch patch;
        try {
            // parse patch file
            patch = mapper.readValue(additionalPatchContent, JsonMergePatch.class);
            if (strict)
                mapper.readValue(additionalPatchContent, StyleMetadata.class);
        } catch (IOException e) {
            throw new IllegalArgumentException(MessageFormat.format("Style metadata patch file is invalid for style ''{0}'' in API ''{1}''.", styleId, apiData.getId()), e);
        }

        try {
            final byte[] patchContent = Files.readAllBytes(patchFile.toPath());

            try {
                // parse patch file
                StyleMetadata current = mapper.readValue(patchContent, StyleMetadata.class);
                return mapper.writeValueAsBytes(mapper.treeToValue(patch.apply(mapper.valueToTree(current)), JsonMergePatch.class));
            } catch (IOException | JsonPatchException e) {
                if (collectionId.isPresent())
                    throw new InternalServerErrorException(MessageFormat.format("Style metadata file in styles store is invalid for style ''{0}'' in collection ''{1}'' in API ''{2}''.", styleId, collectionId.get(), apiData.getId()), e);
                throw new InternalServerErrorException(MessageFormat.format("Style metadata file in styles store is invalid for style ''{0}'' in API ''{1}''.", styleId, apiData.getId()), e);
            }
        } catch (IOException e) {
            if (collectionId.isPresent())
                throw new InternalServerErrorException(MessageFormat.format("Style metadata could not be read for style ''{0}'' in collection ''{1}'' in API ''{2}''.", styleId, collectionId.get(), apiData.getId()), e);
            throw new InternalServerErrorException(MessageFormat.format("Style metadata could not be read for style ''{0}'' in API ''{1}''.", styleId, apiData.getId()), e);
        }
    }

    @Override
    public ImmutableValidationResult.Builder validate(ImmutableValidationResult.Builder builder,
                                                      OgcApiDataV2 apiData,
                                                      Optional<String> collectionId) {
        Optional<StylesConfiguration> config = (collectionId.isEmpty() ? apiData : apiData.getCollections().get(collectionId.get())).getExtension(StylesConfiguration.class);
        if (config.isPresent() && config.get().isEnabled()) {
            List<String> formatLabels = getStyleFormatStream(apiData, collectionId).map(format -> format.getMediaType().label())
                                                                                   .collect(Collectors.toUnmodifiableList());
            for (String encoding : config.get().getStyleEncodings()) {
                if (!formatLabels.contains(encoding)) {
                    builder.addStrictErrors(MessageFormat.format("The style encoding ''{0}'' is specified in the STYLES module configuration, but the format does not exist.", encoding));
                }
            }

            // check that the default stylesheet for the web map exists
            String defaultStyle = config.get().getDefaultStyle();
            if (Objects.isNull(defaultStyle)) {
                defaultStyle = (collectionId.isEmpty() ? apiData : apiData.getCollections().get(collectionId.get()))
                        .getExtension(HtmlConfiguration.class)
                        .map(HtmlConfiguration::getDefaultStyle)
                        .orElse("NONE");
            }
            if (!defaultStyle.equals("NONE")) {
                String finalDefaultStyle = defaultStyle;
                boolean exists = getStyleFormatStream(apiData, collectionId).filter(StyleFormatExtension::getAsDefault)
                                                                            .anyMatch(format -> stylesheetExists(apiData, collectionId, finalDefaultStyle, format, true));
                if (!exists) {
                    if (collectionId.isPresent())
                        builder.addWarnings(MessageFormat.format("The default style ''{0}'' specified in the HTML module configuration does not exist for collection ''{1}'' and no link to the web map will be created.", defaultStyle, collectionId.get()));
                    else
                        builder.addWarnings(MessageFormat.format("The default style ''{0}'' specified in the HTML module configuration does not exist and no link to the web map will be created.", defaultStyle));
                }
            }

            if (collectionId.isPresent()) {
                // all feature access is on the collection level
                final String style = apiData.getExtension(FeaturesHtmlConfiguration.class, collectionId.get()).map(FeaturesHtmlConfiguration::getStyle).orElse(defaultStyle);
                if (!style.equals("NONE") && !style.equals("DEFAULT")) {
                    boolean exists = getStyleFormatStream(apiData, collectionId).filter(StyleFormatExtension::getAsDefault)
                        .anyMatch(format -> stylesheetExists(apiData, collectionId, style, format, true));
                    if (!exists) {
                        builder.addWarnings(MessageFormat.format("The style ''{0}'' specified in the FEATURES_HTML module configuration does not exist for collection ''{1}''. 'NONE' will be used instead.", style, collectionId.get()));
                    }
                }
            }

            final String style = (collectionId.isEmpty()
                ? apiData.getExtension(TilesConfiguration.class)
                : apiData.getExtension(TilesConfiguration.class, collectionId.get()))
                .map(TilesConfiguration::getStyle)
                .orElse(defaultStyle);
            if (!style.equals("NONE") && !style.equals("DEFAULT")) {
                boolean exists = getStyleFormatStream(apiData, collectionId).filter(StyleFormatExtension::getAsDefault)
                    .anyMatch(format -> stylesheetExists(apiData, collectionId, style, format, true));
                if (!exists) {
                    if (collectionId.isPresent())
                        builder.addWarnings(MessageFormat.format("The style ''{0}'' specified in the TILES module configuration does not exist for collection ''{1}''. 'NONE' will be used instead.", style, collectionId.get()));
                    else
                        builder.addWarnings(MessageFormat.format("The style ''{0}'' specified in the TILES module configuration does not exist. 'NONE' will be used instead.", style));
                }
            }
        }

        return builder;
    }

    @Override
    public String getNewStyleId(OgcApiDataV2 apiData, Optional<String> collectionId) {
        Set<String> styleIds = getStyleIds(apiData, collectionId, true);
        int id = 1;
        String s;
        while(true) {
            s = String.format("%s%s%s", collectionId.orElse(""), (collectionId.isPresent() ? "_" : ""), id++);
            if (!styleIds.contains(s))
                return s;
        }
    }

    @Override
    public void writeStyleDocument(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, StyleFormatExtension format, byte[] requestBody) throws IOException {
        Files.write(getStylesheetPath(apiData, collectionId, styleId, format), requestBody);
    }

    @Override
    public void writeStyleMetadataDocument(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, byte[] requestBody) throws IOException {
        Files.write(getStyleMetadataPath(apiData, collectionId, styleId), requestBody);
    }

    @Override
    public void deleteStyle(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId) throws IOException {
        for(StyleFormatExtension format: getStyleFormatStream(apiData, collectionId).collect(Collectors.toUnmodifiableList())) {
            deleteFile(getStylesheetPath(apiData, collectionId, styleId, format));
        }
        deleteFile(getStyleMetadataPath(apiData, collectionId, styleId));
    }

    private void deleteFile(Path path) throws IOException {
        if (Files.exists(path))
            Files.delete(path);
    }

    private Path getStylesheetPath(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, StyleFormatExtension format) {
        String filename = styleId + "." + format.getFileExtension();
        return collectionId.isEmpty()
                ? stylesStore.resolve(apiData.getId()).resolve(filename)
                : stylesStore.resolve(apiData.getId()).resolve(collectionId.get()).resolve(filename);
    }

    private Path getStyleMetadataPath(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId) {
        String filename = styleId + ".metadata";
        return collectionId.isEmpty()
                ? stylesStore.resolve(apiData.getId()).resolve(filename)
                : stylesStore.resolve(apiData.getId()).resolve(collectionId.get()).resolve(filename);
    }

    private boolean deriveCollectionStylesEnabled(OgcApiDataV2 apiData, String collectionId) {
        return apiData.getCollections()
                      .get(collectionId)
                      .getExtension(StylesConfiguration.class)
                      .map(StylesConfiguration::getDeriveCollectionStyles)
                      .orElse(false);
    }

    private Optional<StyleEntry> deriveCollectionStyleEntry(OgcApiDataV2 apiData, StyleEntry rootStyleEntry, String collectionId, ApiRequestContext requestContext) {

        final String styleId = rootStyleEntry.getId();
        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

        ImmutableStyleEntry.Builder builder = ImmutableStyleEntry.builder()
                                                                 .from(rootStyleEntry)
                                                                 .links(ImmutableList.of());

        getStyleFormatStream(apiData, Optional.of(collectionId))
                .filter(StyleFormatExtension::canDeriveCollectionStyle)
                .filter(format -> stylesheetExists(apiData, Optional.empty(), styleId, format))
                .forEach(format -> {
                    StylesheetContent stylesheetContent = getStylesheet(apiData, Optional.empty(), styleId, format, requestContext);
                    Optional<StylesheetContent> derivedStylesheet = format.deriveCollectionStyle(stylesheetContent, apiData, collectionId, styleId);
                    if (derivedStylesheet.isEmpty())
                        return;

                    builder.addAllLinks(stylesLinkGenerator.generateStyleLinks(requestContext.getUriCustomizer(), styleId,
                                                                               ImmutableList.of(format.getMediaType()),
                                                                               i18n, requestContext.getLanguage()));
                });

        StyleEntry styleEntry = builder.addLinks(stylesLinkGenerator.generateStyleMetadataLink(requestContext.getUriCustomizer(), styleId, i18n, requestContext.getLanguage()))
                                       .build();

        return styleEntry.getLinks().size()>1 ? Optional.of(styleEntry) : Optional.empty();
    }

    private List<StylesheetMetadata> deriveStylesheetMetadata(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId,
                                                              ApiRequestContext requestContext) {
        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();
        ImmutableList.Builder<StylesheetMetadata> builder = new ImmutableList.Builder();
        builder.addAll(getStyleFormatStream(apiData, collectionId).filter(format -> !format.getDerived())
                                                                  .filter(format -> stylesheetExists(apiData, collectionId, styleId, format, true))
                                                                  .map(format -> ImmutableStylesheetMetadata.builder()
                                                                                                            .native_(true)
                                                                                                            .title(format.getMediaType().label())
                                                                                                            .link(stylesLinkGenerator.generateStylesheetLink(requestContext.getUriCustomizer()
                                                                                                                                                                           .copy()
                                                                                                                                                                           .removeLastPathSegments(2), styleId, format.getMediaType(), i18n, requestContext.getLanguage()))
                                                                                                            .specification(format.getSpecification())
                                                                                                            .version(format.getVersion())
                                                                                                            .build())
                                                                  .collect(Collectors.toUnmodifiableList()));
        return builder.build();

    }

    private Path getPathStyles(OgcApiDataV2 apiData, Optional<String> collectionId) {
        return collectionId.isPresent()
                ? stylesStore.resolve(apiData.getId()).resolve(collectionId.get())
                : stylesStore.resolve(apiData.getId());
    }

    private Path getPathStyle(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, StyleFormatExtension styleFormat) {
        Path dir = getPathStyles(apiData, collectionId);
        if (!dir.toFile().exists())
            dir.toFile().mkdirs();
        return dir.resolve(String.format("%s.%s", styleId, styleFormat.getFileExtension()));
    }

    private Path getPathMetadata(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId) {
        Path dir = getPathStyles(apiData, collectionId);
        if (!dir.toFile().exists())
            dir.toFile().mkdirs();
        return dir.resolve(String.format("%s.metadata", styleId));
    }

    private Optional<String> getTitle(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, ApiRequestContext requestContext) {
        return getStyleFormatStream(apiData, collectionId)
                .filter(format -> stylesheetExists(apiData, collectionId, styleId, format))
                .map(format -> format.getTitle(styleId, getStylesheet(apiData, collectionId, styleId, format, requestContext)))
                .filter(title -> !title.equals(styleId))
                .findAny();
    }

    public Set<String> getStyleIds(OgcApiDataV2 apiData, Optional<String> collectionId, boolean includeDerived) {
        ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<>();
        builder.addAll(Arrays.stream(Objects.requireNonNullElse(getPathStyles(apiData, collectionId).toFile().listFiles(), new File[0]))
                             .map(file -> {
                                 String filename = file.getName();
                                 if (filename.contains("."))
                                     filename = filename.substring(0, file.getName().lastIndexOf("."));

                                 return filename;
                             })
                             .collect(Collectors.toUnmodifiableSet()));

        if (includeDerived && collectionId.isPresent() && deriveCollectionStylesEnabled(apiData, collectionId.get())) {
            // add the styles from the parent style collection, that are used to derive a style for the collection
            builder.addAll(Arrays.stream(Objects.requireNonNullElse(getPathStyles(apiData, Optional.empty()).toFile().listFiles(), new File[0]))
                                 .map(file -> {
                                     String filename = file.getName();
                                     if (filename.contains("."))
                                         filename = filename.substring(0, file.getName().lastIndexOf("."));

                                     return filename;
                                 })
                                 .filter(styleId -> getStyleFormatStream(apiData, collectionId).anyMatch(format -> willDeriveStylesheet(apiData, collectionId, styleId, format)))
                                 .collect(Collectors.toUnmodifiableSet()));
        }

        return builder.build();
    }
}

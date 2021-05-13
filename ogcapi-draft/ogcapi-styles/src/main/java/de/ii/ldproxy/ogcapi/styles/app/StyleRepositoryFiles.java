/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableStyleEntry;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.StyleEntry;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableStyleMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableStylesheetMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableStyles;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadataFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleRepository;
import de.ii.ldproxy.ogcapi.styles.domain.StylesheetContent;
import de.ii.ldproxy.ogcapi.styles.domain.StylesheetMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.Styles;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.StylesFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StylesLinkGenerator;
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
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
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
public class StyleRepositoryFiles implements StyleRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleRepositoryFiles.class);

    private final ExtensionRegistry extensionRegistry;
    private final java.nio.file.Path stylesStore;
    private final I18n i18n;

    public StyleRepositoryFiles(@Context BundleContext bundleContext,
                                @Requires ExtensionRegistry extensionRegistry,
                                @Requires I18n i18n) throws IOException {
        this.stylesStore = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), API_RESOURCES_DIR)
                                .resolve("styles");
        this.i18n = i18n;
        this.extensionRegistry = extensionRegistry;
        java.nio.file.Files.createDirectories(stylesStore);
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
    public Styles getStyles(OgcApiDataV2 apiData, Optional<String> collectionId, ApiRequestContext requestContext) {
        File dir = getPathStyles(apiData, collectionId).toFile();
        if (!dir.exists())
            dir.getParentFile().mkdirs();
        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();
        Map<String, StyleFormatExtension> formatMap = getStyleFormatStream(apiData, collectionId).filter(format -> !format.getDerived())
                                                                                                 .collect(Collectors.toUnmodifiableMap(format -> format.getFileExtension(), format -> format));
        Styles styles = ImmutableStyles.builder()
                                       .styles(
                                               Arrays.stream(Objects.requireNonNullElse(dir.listFiles(), ImmutableList.of().toArray(File[]::new)))
                                                     .filter(file -> !file.isHidden())
                                                     .filter(file -> formatMap.containsKey(com.google.common.io.Files.getFileExtension(file.getName())))
                                                     .map(file -> com.google.common.io.Files.getNameWithoutExtension(file.getName()))
                                                     .distinct()
                                                     .sorted()
                                                     .map(styleId -> ImmutableStyleEntry.builder()
                                                                                        .id(styleId)
                                                                                        .title(getTitle(apiData, collectionId, styleId, requestContext).orElse(styleId))
                                                                                        .links(stylesLinkGenerator.generateStyleLinks(requestContext.getUriCustomizer(),
                                                                                                                                      styleId,
                                                                                                                                      getStylesheetMediaTypes(apiData,
                                                                                                                                                              collectionId,
                                                                                                                                                              styleId),
                                                                                                                                      i18n,
                                                                                                                                      requestContext.getLanguage()))
                                                                                        .addLinks(stylesLinkGenerator.generateStyleMetadataLink(requestContext.getUriCustomizer(),
                                                                                                                                                styleId,
                                                                                                                                                i18n,
                                                                                                                                                requestContext.getLanguage()))
                                                                                        .build())
                                                     .collect(Collectors.toList()))
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
                                                      .collect(Collectors.toUnmodifiableList())).build();
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
        return collectionId.isPresent() &&
                deriveCollectionStylesEnabled(apiData, collectionId.get()) &&
                getPathStyle(apiData, Optional.empty(), styleId, styleFormat).toFile().exists() &&
                styleFormat.canDeriveCollectionStyle();
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

        if (stylesheetExists(apiData, Optional.empty(), styleId, styleFormat, true)) {
            Optional<StylesheetContent> stylesheet = styleFormat.deriveCollectionStyle(getStylesheet(apiData, Optional.empty(), styleId, styleFormat, requestContext), apiData.getId(), collectionId.get(), styleId, Optional.of(requestContext.getUriCustomizer()));
            if (stylesheet.isPresent())
                return stylesheet.get();
        }

        throw new NotFoundException(MessageFormat.format("The style ''{0}'' does not exist in this API for collection ''{1}''.", styleId, collectionId.get()));
    }

    @Override
    public StyleMetadata getStyleMetadata(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, ApiRequestContext requestContext) {
        File metadataFile = getPathMetadata(apiData, collectionId, styleId).toFile();

        if (!metadataFile.exists()) {
            // TODO derive metadata from a source metadata, if the style is derived
            List<StylesheetMetadata> stylesheets = deriveStylesheetMetadata(apiData, collectionId, styleId, requestContext);
            Optional<String> title = stylesheets.stream()
                                                .map(sheet -> sheet.getTitle())
                                                .filter(Optional::isPresent)
                                                .map(Optional::get)
                                                .filter(stylesheetTitle -> !stylesheetTitle.equals(styleId))
                                                .findAny();
            ImmutableStyleMetadata.Builder metadata = ImmutableStyleMetadata.builder()
                                                                            .id(styleId)
                                                                            .title(title.orElse(styleId))
                                                                            .stylesheets(stylesheets);

            return metadata.build();
        }

        try {
            final byte[] metadataContent = Files.readAllBytes(metadataFile.toPath());

            // prepare Jackson mapper for deserialization
            final ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new Jdk8Module());
            mapper.registerModule(new GuavaModule());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                // parse input
                StyleMetadata metadata = mapper.readValue(metadataContent, StyleMetadata.class);

                return metadata.replaceParameters(requestContext.getUriCustomizer());
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
    public ImmutableValidationResult.Builder validate(ImmutableValidationResult.Builder builder,
                                                      OgcApiDataV2 apiData,
                                                      Optional<String> collectionId) {
        Optional<StylesConfiguration> config = (collectionId.isEmpty() ? apiData : apiData.getCollections().get(collectionId.get())).getExtension(StylesConfiguration.class);
        if (config.isPresent()) {
            List<String> formatLabels = getStyleFormatStream(apiData, collectionId).map(format -> format.getMediaType().label())
                                                                                   .collect(Collectors.toUnmodifiableList());
            for (String encoding : config.get().getStyleEncodings()) {
                if (!formatLabels.contains(encoding)) {
                    builder.addStrictErrors(MessageFormat.format("The style encoding ''{0}'' is specified in the STYLES module configuration, but the format does not exist.", encoding));
                }
            }

            // check that the default stylesheet for the web map exists
            String defaultStyle = config.get().getDefaultStyle();
            if (Objects.nonNull(defaultStyle)) {
                boolean exists = getStyleFormatStream(apiData, collectionId).filter(StyleFormatExtension::getAsDefault)
                                                                            .anyMatch(format -> stylesheetExists(apiData, collectionId, defaultStyle, format, true));
                if (!exists) {
                    builder.addStrictErrors(MessageFormat.format("The default style ''{0}'' specified in the STYLES module configuration does not exist.", defaultStyle));
                }
            }
        }

        return builder;
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
                    Optional<StylesheetContent> derivedStylesheet = format.deriveCollectionStyle(stylesheetContent, apiData.getId(), collectionId, styleId, Optional.of(requestContext.getUriCustomizer()));
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
                                                                                                            .title(format.getTitle(styleId, getStylesheet(apiData, collectionId, styleId, format, requestContext, true)))
                                                                                                            .link(stylesLinkGenerator.generateStylesheetLink(requestContext.getUriCustomizer(), styleId, format.getMediaType(), i18n, requestContext.getLanguage()))
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
                .map(format -> format.getTitle(styleId, getStylesheet(apiData, collectionId, styleId, format, requestContext)))
                .filter(title -> !title.equals(styleId))
                .findAny();
    }


}

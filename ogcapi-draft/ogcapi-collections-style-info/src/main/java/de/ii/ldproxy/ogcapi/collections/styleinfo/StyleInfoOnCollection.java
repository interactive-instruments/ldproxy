/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.styleinfo;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionExtension;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiCollection;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.domain.ImmutableStyleEntry;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.API_RESOURCES_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

/**
 * add style links to the collection information
 */
@Component
@Provides
@Instantiate
public class StyleInfoOnCollection implements CollectionExtension {

    @Requires
    I18n i18n;

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleInfoOnCollection.class);

    private final Path styleInfosStore;

    public StyleInfoOnCollection(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) throws IOException {
        this.styleInfosStore = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), API_RESOURCES_DIR)
                                    .resolve("style-infos");
        Files.createDirectories(styleInfosStore);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<StylesConfiguration> stylesExtension = apiData.getExtension(StylesConfiguration.class);

        if (stylesExtension.isPresent() && stylesExtension.get()
                                                          .getStyleInfosOnCollection()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        Optional<StylesConfiguration> stylesExtension = apiData.isCollectionEnabled(collectionId) ?
                apiData.getCollections()
                       .get(collectionId)
                       .getExtension(StylesConfiguration.class) :
                Optional.empty();

        if (stylesExtension.isPresent() && stylesExtension.get()
                                                          .getStyleInfosOnCollection()) {
            return true;
        }
        return false;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                                     FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                     OgcApiDataV2 apiData,
                                                     URICustomizer uriCustomizer,
                                                     boolean isNested,
                                                     ApiMediaType mediaType,
                                                     List<ApiMediaType> alternateMediaTypes,
                                                     Optional<Locale> language) {
        if (!isNested && isEnabledForApi(apiData, featureTypeConfiguration.getId())) {
            final String apiId = apiData.getId();
            final Optional<Integer> apiVersion = apiData.getApiVersion();
            final String collectionId = featureTypeConfiguration.getId();
            File apiDir = new File(styleInfosStore + File.separator + apiId);
            if (!apiDir.exists()) {
                apiDir.mkdirs();
            }

            Path collectionFile = styleInfosStore.resolve(apiId).resolve(collectionId + ".json");
            if (Files.exists(collectionFile)) {
                Optional<StyleInfo> styleInfos = getStyleInfos(collectionFile, apiId, apiVersion, collectionId, uriCustomizer.copy());
                if (styleInfos.isPresent() && styleInfos.get().getStyles().isPresent()) {
                    collection.putExtensions("styles",
                            styleInfos.get()
                                    .getStyles()
                                    .get()
                                    .stream()
                                    .map(styleInfo -> ImmutableStyleEntry.builder()
                                            .id(styleInfo.getId())
                                            .title(styleInfo.getTitle())
                                            .links(styleInfo.getLinks())
                                            .build())
                                    .collect(Collectors.toList()));
                }
                if (styleInfos.isPresent() && styleInfos.get().getDefaultStyle().isPresent()) {
                    collection.putExtensions("defaultStyle",styleInfos.get().getDefaultStyle());
                }
            }
        }

        return collection;
    }

    private Optional<StyleInfo> getStyleInfos(Path styleInfosFile, String apiId, Optional<Integer> apiVersion, String collectionId, URICustomizer uriCustomizer) {

        try {
            final byte[] content = java.nio.file.Files.readAllBytes(styleInfosFile);

            // prepare Jackson mapper for deserialization
            final ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new Jdk8Module());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                // parse input
                StyleInfo styleInfo = mapper.readValue(content, StyleInfo.class);

                return Optional.of(replaceParameters(styleInfo, apiId, apiVersion, collectionId, uriCustomizer));
            } catch (IOException e) {
                LOGGER.error("File in styleInfo store is invalid and is skipped: "+styleInfosFile.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.error("Style info could not be read: "+styleInfosFile.toAbsolutePath());
        }
        return Optional.empty();
    }

    private StyleInfo replaceParameters(StyleInfo styleInfo, String apiId, Optional<Integer> apiVersion, String collectionId, URICustomizer uriCustomizer) {

        // any template parameters in links?
        boolean templated = styleInfo.getStyles()
                                     .orElse(ImmutableList.of())
                                     .stream()
                                     .map(style -> style.getLinks())
                                     .flatMap(Collection::stream)
                                     .filter(Objects::nonNull)
                                     .anyMatch(link -> Objects.requireNonNullElse(link.getTemplated(),false) && link.getHref().matches("^.*\\{(serviceUrl|collectionId)\\}.*$"));

        if (!templated)
            return styleInfo;

        String serviceUrl = uriCustomizer.removeLastPathSegments(2)
                                         .clearParameters()
                                         .ensureNoTrailingSlash()
                                         .toString();

        return ImmutableStyleInfo.builder()
                                 .defaultStyle(styleInfo.getDefaultStyle())
                                 .styles(styleInfo.getStyles()
                                                  .orElse(ImmutableList.of())
                                                  .stream()
                                                  .map(style -> ImmutableStyleEntry.builder()
                                                                                   .id(style.getId())
                                                                                   .title(style.getTitle())
                                                                                   .description(style.getDescription())
                                                                                   .links(style.getLinks()
                                                                                               .stream()
                                                                                               .map(link -> link.getTemplated() ?
                                                                                                       new ImmutableLink.Builder()
                                                                                                                        .from(link)
                                                                                                                        .href(link.getHref()
                                                                                                                                  .replace("{serviceUrl}", serviceUrl)
                                                                                                                                  .replace("{collectionId}", collectionId))
                                                                                                                        .templated(null)
                                                                                                                        .build() : link)
                                                                                               .collect(ImmutableList.toImmutableList()))
                                                                                   .build())
                                                  .collect(ImmutableList.toImmutableList()))
                                 .build();
    }
}

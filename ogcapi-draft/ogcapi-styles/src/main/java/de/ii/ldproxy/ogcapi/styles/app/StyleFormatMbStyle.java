/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.foundation.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.SchemaGenerator;
import de.ii.ldproxy.ogcapi.foundation.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableMbStyleStylesheet;
import de.ii.ldproxy.ogcapi.styles.domain.MbStyleLayer;
import de.ii.ldproxy.ogcapi.styles.domain.MbStyleStylesheet;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleLayer;
import de.ii.ldproxy.ogcapi.styles.domain.StylesheetContent;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import io.swagger.v3.oas.models.media.Schema;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class StyleFormatMbStyle implements ConformanceClass, StyleFormatExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleFormatMbStyle.class);

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "vnd.mapbox.style+json"))
            .label("Mapbox")
            .parameter("mbs")
            .fileExtension("json")
            .build();

    private final AppContext appContext;
    private final Schema schemaStyle;
    public final static String SCHEMA_REF_STYLE = "#/components/schemas/MbStyleStylesheet";

    @Inject
    public StyleFormatMbStyle(AppContext appContext, SchemaGenerator schemaGenerator) {
        this.appContext = appContext;
        this.schemaStyle = schemaGenerator.getSchema(MbStyleStylesheet.class);
    }

    @Override
    public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-styles-1/0.0/conf/mapbox-styles");
    }

    @Override
    public boolean canSupportTransactions() {
        return true;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(schemaStyle)
                .schemaRef(SCHEMA_REF_STYLE)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public ApiMediaTypeContent getRequestContent(OgcApiDataV2 apiData, String path, HttpMethods method) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(schemaStyle)
                .schemaRef(SCHEMA_REF_STYLE)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public String getFileExtension() {
        return "mbs";
    }

    @Override
    public String getSpecification() {
        return "https://docs.mapbox.com/mapbox-gl-js/style-spec/";
    }

    @Override
    public String getVersion() {
        return "8";
    }

    @Override
    public boolean getAsDefault() { return true; }

    @Override
    public String getTitle(String styleId, StylesheetContent stylesheetContent) {
        Optional<MbStyleStylesheet> optionalStylesheet = parse(stylesheetContent, "", false, false);
        return optionalStylesheet.isPresent() ? optionalStylesheet.get().getName().orElse(styleId) : styleId;
    }

    @Override
    public Object getStyleEntity(StylesheetContent stylesheetContent, OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, ApiRequestContext requestContext) {
        URICustomizer uriCustomizer = new URICustomizer(appContext.getUri().resolve("rest/services")).ensureLastPathSegments(apiData.getSubPath().toArray(String[]::new));
        String serviceUrl = uriCustomizer.toString();
        return parse(stylesheetContent, serviceUrl, true, false);
    }

    @Override
    public boolean canDeriveCollectionStyle() {
        return true;
    }

    @Override
    public Optional<StylesheetContent> deriveCollectionStyle(StylesheetContent stylesheetContent, OgcApiDataV2 apiData, String collectionId, String styleId) {
        URICustomizer uriCustomizer = new URICustomizer(appContext.getUri().resolve("rest/services")).ensureLastPathSegments(apiData.getSubPath().toArray(String[]::new));
        String serviceUrl = uriCustomizer.toString();
        Optional<MbStyleStylesheet> mbStyleOriginal = StyleFormatMbStyle.parse(stylesheetContent, serviceUrl, false, false);
        if (mbStyleOriginal.isEmpty() ||
            mbStyleOriginal.get().getLayers()
                           .stream()
                           .noneMatch(layer -> layer.getSource().isPresent() &&
                                   layer.getSource().get().equals(apiData.getId()) &&
                                   layer.getSource().isPresent() &&
                                   layer.getSourceLayer().get().equals(collectionId)))
            return Optional.empty();

        MbStyleStylesheet mbStyleDerived = ImmutableMbStyleStylesheet.builder()
                                                                     .from(mbStyleOriginal.get())
                                                                     .layers(mbStyleOriginal.get().getLayers()
                                                                                            .stream()
                                                                                            .filter(layer -> layer.getSource().isEmpty() || !layer.getSource().get().equals(apiData.getId()) || (layer.getSourceLayer().isEmpty() || layer.getSourceLayer().get().equals(collectionId)))
                                                                                            .collect(Collectors.toUnmodifiableList()))
                                                                     .build();

        String descriptor = String.format("%s/%s/%s", apiData.getId(), collectionId, styleId);
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new Jdk8Module());
            return Optional.of(new StylesheetContent(mapper.writeValueAsBytes(mbStyleDerived), descriptor, true));
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Could not derive style %s. Reason: %s", descriptor, e.getMessage()));
            return Optional.empty();
        }
    }

    @Override
    public boolean canDeriveMetadata() { return true; }

    @Override
    public List<StyleLayer> deriveLayerMetadata(StylesheetContent stylesheetContent,
        OgcApiDataV2 apiData,
        FeaturesCoreProviders providers,
        EntityRegistry entityRegistry) {
        URICustomizer uriCustomizer = new URICustomizer(appContext.getUri().resolve("rest/services")).ensureLastPathSegments(apiData.getSubPath().toArray(String[]::new));
        String serviceUrl = uriCustomizer.toString();
        Optional<MbStyleStylesheet> mbStyle = StyleFormatMbStyle.parse(stylesheetContent, serviceUrl, false, false);
        if (mbStyle.isEmpty())
            return ImmutableList.of();

        return mbStyle.get().getLayerMetadata(apiData, providers, entityRegistry);
    }

    @Override
    public Optional<String> analyze(StylesheetContent stylesheetContent, boolean strict) {
        MbStyleStylesheet stylesheet = parse(stylesheetContent, "", true, strict).get();

        // TODO add more checks
        if (strict) {
            if (stylesheet.getLayers().isEmpty()) {
                throw new IllegalArgumentException("The Mapbox Style document has no layers.");
            }
            if (stylesheet.getVersion() != 8) {
                throw new IllegalArgumentException("The Mapbox Style document does not have version '8'. Found: " + stylesheet.getVersion());
            }
            if (stylesheet.getSources().isEmpty()) {
                throw new IllegalArgumentException("The Mapbox Style document has no sources.");
            }
            List<String> ids = new ArrayList<>();
            List<String> types = ImmutableList.of("fill", "line", "symbol", "circle", "heatmap", "fill-extrusion", "raster", "hillshade", "background");
            for (MbStyleLayer layer : stylesheet.getLayers()) {
                String id = layer.getId();
                if (Objects.isNull(id))
                    throw new IllegalArgumentException("A layer in the Mapbox Style document has no id.");

                String type = layer.getType();
                if (Objects.isNull(type))
                    throw new IllegalArgumentException(String.format("Layer '%s' in the Mapbox Style document has no type.", id));

                if (ids.contains(id)) {
                    throw new IllegalArgumentException(String.format("Multiple layers in the Mapbox Style document have id '%s'.", id));
                }
                ids.add(id);

                if (!types.contains(type)) {
                    throw new IllegalArgumentException(String.format("Layer '%s' in the Mapbox Style document has an invalid type: '%s'", id, type));
                }
            }
        }

        Optional<String> styleIdCandidate = stylesheet.getName();
        if (styleIdCandidate.isPresent()) {
            Pattern styleNamePattern = Pattern.compile("[^\\w\\-]", Pattern.CASE_INSENSITIVE);
            Matcher styleNameMatcher = styleNamePattern.matcher(styleIdCandidate.get());
            if (styleIdCandidate.get().contains(" ") || styleNameMatcher.find())
                return Optional.empty();
        }

        return styleIdCandidate;
    }

    static Optional<MbStyleStylesheet> parse(StylesheetContent stylesheetContent, String serviceUrl, boolean throwOnError, boolean strict) {
        final byte[] content = stylesheetContent.getContent();

        // prepare Jackson mapper for deserialization
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new GuavaModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, strict);
        mapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
        MbStyleStylesheet parsedContent;
        try {
            // parse input
            parsedContent = mapper.readValue(content, MbStyleStylesheet.class);
        } catch (IOException e) {
            if (stylesheetContent.getInStore()) {
                // this is an invalid style already in the store: server error
                if (throwOnError)
                    throw new RuntimeException("The content of a stylesheet is invalid: " + stylesheetContent.getDescriptor() + ".", e);
            } else {
                // a style provided by a client: client error
                if (throwOnError)
                    throw new IllegalArgumentException("The content of the stylesheet is invalid.", e);
            }
            LOGGER.error("The content of a stylesheet ''{}'' is invalid: {}", stylesheetContent.getDescriptor(), e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace:", e);
            }
            return Optional.empty();
        }

        return Optional.of(parsedContent.replaceParameters(serviceUrl));
    }
}

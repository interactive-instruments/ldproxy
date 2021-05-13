/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.SchemaGenerator;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableMbStyleStylesheet;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableMbStyleVectorSource;
import de.ii.ldproxy.ogcapi.styles.domain.MbStyleSource;
import de.ii.ldproxy.ogcapi.styles.domain.MbStyleStylesheet;
import de.ii.ldproxy.ogcapi.styles.domain.MbStyleVectorSource;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StylesheetContent;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class StyleFormatMbStyle implements ConformanceClass, StyleFormatExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleFormatMbStyle.class);

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "vnd.mapbox.style+json"))
            .label("Mapbox")
            .parameter("mbs")
            .build();

    private final Schema schemaStyle;
    public final static String SCHEMA_REF_STYLE = "#/components/schemas/MbStyleStylesheet";

    public StyleFormatMbStyle(@Requires SchemaGenerator schemaGenerator) {
        schemaStyle = schemaGenerator.getSchema(MbStyleStylesheet.class);
    }

    @Override
    public List<String> getConformanceClassUris() {
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

    static MbStyleStylesheet parse(StylesheetContent stylesheetContent, Optional<URICustomizer> uriCustomizer) {
        final byte[] content = stylesheetContent.getContent();

        // prepare Jackson mapper for deserialization
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MbStyleStylesheet parsedContent;
        try {
            // parse input
            parsedContent = mapper.readValue(content, MbStyleStylesheet.class);
        } catch (IOException e) {
            throw new RuntimeException("StylesheetContent in the styles store is invalid: " + stylesheetContent.getDescriptor() + ".", e);
        }

        return replaceParameters(parsedContent, uriCustomizer);
    }

    @Override
    public String getTitle(String styleId, StylesheetContent stylesheetContent) {
        return parse(stylesheetContent, Optional.empty()).getName().orElse(styleId);
    }

    @Override
    public Object getStyleEntity(StylesheetContent stylesheetContent, OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, ApiRequestContext requestContext) {
        return parse(stylesheetContent, Optional.of(requestContext.getUriCustomizer()));
    }

    @Override
    public boolean canDeriveCollectionStyle() {
        return true;
    }

    @Override
    public Optional<StylesheetContent> deriveCollectionStyle(StylesheetContent stylesheetContent, String apiId, String collectionId, String styleId, Optional<URICustomizer> uriCustomizer) {
        MbStyleStylesheet mbStyleOriginal = StyleFormatMbStyle.parse(stylesheetContent, uriCustomizer);
        MbStyleStylesheet mbStyleDerived = ImmutableMbStyleStylesheet.builder()
                                                                     .from(mbStyleOriginal)
                                                                     .layers(mbStyleOriginal.getLayers()
                                                                                            .stream()
                                                                                            .filter(layer -> layer.getSource().isEmpty() || !layer.getSource().get().equals(apiId) || (layer.getSourceLayer().isEmpty() || layer.getSourceLayer().get().equals(collectionId)))
                                                                                            .collect(Collectors.toUnmodifiableList()))
                                                                     .build();

        String descriptor = String.format("%s/%s/%s", apiId, collectionId, styleId);
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new Jdk8Module());
            return Optional.of(new StylesheetContent(mapper.writeValueAsBytes(mbStyleDerived), descriptor));
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Could not derive style %s. Reason: %s", descriptor, e.getMessage()));
            return Optional.empty();
        }
    }

    private static MbStyleStylesheet replaceParameters(MbStyleStylesheet stylesheet, Optional<URICustomizer> uriCustomizer) {
        if (uriCustomizer.isEmpty())
            return stylesheet;

        // any template parameters in links?
        boolean templated = stylesheet.getSprite()
                                      .orElse("")
                                      .matches("^.*\\{serviceUrl\\}.*$") ||
                            stylesheet.getGlyphs()
                                      .orElse("")
                                      .matches("^.*\\{serviceUrl\\}.*$") ||
                            stylesheet.getSources()
                                      .values()
                                      .stream()
                                      .filter(source -> source instanceof MbStyleVectorSource)
                                      .anyMatch(source -> ((MbStyleVectorSource) source).getTiles()
                                                                                        .orElse(ImmutableList.of())
                                                                                        .stream()
                                                                                        .anyMatch(tilesUri -> tilesUri.matches("^.*\\{serviceUrl\\}.*$")) ||
                                                        ((MbStyleVectorSource) source).getUrl()
                                                                                      .orElse("")
                                                                                      .matches("^.*\\{serviceUrl\\}.*$"));
        if (!templated)
            return stylesheet;

        String serviceUrl = uriCustomizer.get()
                                         .copy()
                                         .removeLastPathSegments(2)
                                         .clearParameters()
                                         .ensureNoTrailingSlash()
                                         .toString();

        return ImmutableMbStyleStylesheet.builder()
                                         .from(stylesheet)
                                         .sprite(stylesheet.getSprite().isPresent() ?
                                                 Optional.of(stylesheet.getSprite().get().replace("{serviceUrl}", serviceUrl)) :
                                                 Optional.empty())
                                         .glyphs(stylesheet.getGlyphs().isPresent() ?
                                                         Optional.of(stylesheet.getGlyphs().get().replace("{serviceUrl}", serviceUrl)) :
                                                         Optional.empty())
                                         .sources(stylesheet.getSources()
                                                            .entrySet()
                                                            .stream()
                                                            .collect(Collectors.toMap(entry -> entry.getKey(),
                                                                                      entry -> {
                                                                                          MbStyleSource source = entry.getValue();
                                                                                          return (source instanceof MbStyleVectorSource) ?
                                                                                                  ImmutableMbStyleVectorSource.builder()
                                                                                                                              .from((MbStyleVectorSource)source)
                                                                                                                              .url(((MbStyleVectorSource)source).getUrl()
                                                                                                                                                                .map(url -> url.replace("{serviceUrl}", serviceUrl)))
                                                                                                                              .tiles(((MbStyleVectorSource)source).getTiles()
                                                                                                                                                                  .orElse(ImmutableList.of())
                                                                                                                                                                  .stream()
                                                                                                                                                                  .map(tile -> tile.replace("{serviceUrl}", serviceUrl))
                                                                                                                                                                  .collect(Collectors.toList()))
                                                                                                                              .build() :
                                                                                                  source;
                                                                                      })))
                                         .build();
    }
}

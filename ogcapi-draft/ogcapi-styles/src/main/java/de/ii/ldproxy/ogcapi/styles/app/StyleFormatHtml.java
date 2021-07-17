/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ArrayListMultimap;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionDynamicMetadataRegistry;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.SchemaGenerator;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableMbStyleStylesheet;
import de.ii.ldproxy.ogcapi.styles.domain.MbStyleStylesheet;
import de.ii.ldproxy.ogcapi.styles.domain.MbStyleVectorSource;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.StylesheetContent;
import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import de.ii.xtraplatform.store.domain.legacy.KeyValueStore;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class StyleFormatHtml implements StyleFormatExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleFormatHtml.class);
    public static final String MEDIA_TYPE_STRING = "application/vnd.mapbox.style+json" ;
    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .label("HTML")
            .parameter("html")
            .build();

    private final Schema schemaStyle;
    private final SchemaGenerator schemaGenerator;
    private final KeyValueStore keyValueStore;
    private final XtraPlatform xtraPlatform;
    private final CollectionDynamicMetadataRegistry metadataRegistry;
    public final static String SCHEMA_REF_STYLE = "#/components/schemas/htmlSchema";

    public StyleFormatHtml(@Requires SchemaGenerator schemaGenerator,
                           @Requires KeyValueStore keyValueStore,
                           @Requires XtraPlatform xtraPlatform,
                           @Requires CollectionDynamicMetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
        schemaStyle = new StringSchema().example("<html>...</html>");
        this.schemaGenerator = schemaGenerator;
        this.keyValueStore = keyValueStore;
        this.xtraPlatform = xtraPlatform;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(StylesConfiguration.class)
                      .filter(config -> config.getStyleEncodings().contains(this.getMediaType().label()) &&
                                        config.getStyleEncodings().contains(StyleFormatMbStyle.MEDIA_TYPE.label()))
                      .isPresent();
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
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
    public boolean getDerived() {
        return true;
    }

    @Override
    public boolean canDeriveCollectionStyle() {
        return true;
    }

    // TODO centralize
    @Override
    public Optional<StylesheetContent> deriveCollectionStyle(StylesheetContent stylesheetContent, OgcApiDataV2 apiData, String collectionId, String styleId) {
        URICustomizer uriCustomizer = new URICustomizer(xtraPlatform.getServicesUri()).ensureLastPathSegments(apiData.getSubPath().toArray(String[]::new));
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
    public Object getStyleEntity(StylesheetContent stylesheetContent, OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, ApiRequestContext requestContext) {

        URICustomizer uriCustomizer = new URICustomizer(xtraPlatform.getServicesUri()).ensureLastPathSegments(apiData.getSubPath().toArray(String[]::new));
        String serviceUrl = uriCustomizer.toString();

        if (collectionId.isPresent())
            uriCustomizer.ensureLastPathSegments("collections", collectionId.get());
        uriCustomizer.ensureLastPathSegments("styles", styleId)
                .addParameter("f", "mbs");
        String styleUrl = uriCustomizer.toString();

        boolean popup = apiData.getExtension(StylesConfiguration.class).map(StylesConfiguration::getWebmapWithPopup).orElse(true);

        boolean layerControl = apiData.getExtension(StylesConfiguration.class).map(StylesConfiguration::getWebmapWithLayerControl).orElse(false);
        boolean allLayers = apiData.getExtension(StylesConfiguration.class).map(StylesConfiguration::getLayerControlAllLayers).orElse(false);
        ArrayListMultimap<String,String> layerMap = ArrayListMultimap.create();
        if (layerControl) {
            MbStyleStylesheet mbStyle = StyleFormatMbStyle.parse(stylesheetContent, serviceUrl, true, false).get();
            if (allLayers) {
                Map<String, FeatureTypeConfigurationOgcApi> collectionData = apiData.getCollections();
                mbStyle.getLayers()
                       .stream()
                       .forEach(styleLayer -> {
                           if (styleLayer.getSourceLayer().isPresent()) {
                               layerMap.put(collectionData.containsKey(styleLayer.getSourceLayer().get()) ?
                                                    collectionData.get(styleLayer.getSourceLayer().get()).getLabel() :
                                                    styleLayer.getSourceLayer().get(),
                                            styleLayer.getId());
                           } else {
                               layerMap.put(styleLayer.getId(), styleLayer.getId());
                           }
                       });
            } else {
                Set<String> vectorSources = mbStyle.getSources()
                                                   .entrySet()
                                                   .stream()
                                                   .filter(source -> source.getValue() instanceof MbStyleVectorSource)
                                                   .map(Map.Entry::getKey)
                                                   .collect(Collectors.toUnmodifiableSet());
                Map<String, FeatureTypeConfigurationOgcApi> collectionData = apiData.getCollections();
                mbStyle.getLayers()
                       .stream()
                       .filter(styleLayer -> styleLayer.getSource().isPresent() && vectorSources.contains(styleLayer.getSource().get()) && styleLayer.getSourceLayer().isPresent() && collectionData.containsKey(styleLayer.getSourceLayer().get()))
                       .forEach(styleLayer -> layerMap.put(collectionData.containsKey(styleLayer.getSourceLayer().get()) ?
                                                                   collectionData.get(styleLayer.getSourceLayer().get()).getLabel() :
                                                                   styleLayer.getSourceLayer().get(),
                                                           styleLayer.getId()));
            }
        }

        return new StyleView(styleUrl, apiData, metadataRegistry.getSpatialExtent(apiData.getId()).orElse(null), styleId, popup, layerControl, layerMap.asMap());
    }
}

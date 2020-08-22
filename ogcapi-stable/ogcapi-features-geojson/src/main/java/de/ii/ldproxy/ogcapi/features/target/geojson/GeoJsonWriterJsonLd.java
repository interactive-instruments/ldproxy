/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.target.geojson;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.features.target.geojson.GeoJsonConfiguration.JsonLdOptions;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.stringtemplates.domain.StringTemplateFilters;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoJsonWriterJsonLd implements GeoJsonWriter {

    @Override
    public GeoJsonWriterJsonLd create() {
        return new GeoJsonWriterJsonLd();
    }

    @Override
    public int getSortPriority() {
        return 5;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext,
                        Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (transformationContext.isFeatureCollection()) {
            Optional<JsonLdOptions> jsonLdOptions = transformationContext.getApiData()
                                                                         .getCollections()
                                                                         .get(transformationContext.getCollectionId())
                                                                         .getExtension(GeoJsonConfiguration.class)
                                                                         .flatMap(GeoJsonConfiguration::getJsonLd);

            if (jsonLdOptions.isPresent()) {
                writeContextAndJsonLdType(transformationContext, jsonLdOptions.get()
                                                                              .getContext(), ImmutableList.of("geojson:FeatureCollection"));
            }
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext,
                               Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        Optional<JsonLdOptions> jsonLdOptions = transformationContext.getApiData()
                                                                     .getCollections()
                                                                     .get(transformationContext.getCollectionId())
                                                                     .getExtension(GeoJsonConfiguration.class)
                                                                     .flatMap(GeoJsonConfiguration::getJsonLd);

        if (jsonLdOptions.isPresent()) {
            List<String> types = jsonLdOptions.map(JsonLdOptions::getTypes)
                                              .orElse(ImmutableList.of("geojson:Feature"));

            writeContextAndJsonLdType(transformationContext, jsonLdOptions.get()
                                                                          .getContext(), types, !transformationContext.isFeatureCollection());
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onProperty(FeatureTransformationContextGeoJson transformationContext,
                           Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (transformationContext.getState()
                                 .getCurrentFeatureProperty()
                                 .isPresent()
                || transformationContext.getState()
                                        .getCurrentValue()
                                        .isPresent()) {

            final FeatureProperty currentFeatureProperty = transformationContext.getState()
                                                                                .getCurrentFeatureProperty()
                                                                                .get();
            String currentValue = transformationContext.getState()
                                                       .getCurrentValue()
                                                       .get();

            if (currentFeatureProperty.isId()) {

                Optional<String> jsonLdId = transformationContext.getApiData()
                                                                 .getCollections()
                                                                 .get(transformationContext.getCollectionId())
                                                                 .getExtension(GeoJsonConfiguration.class)
                                                                 .flatMap(GeoJsonConfiguration::getJsonLd)
                                                                 .flatMap(GeoJsonConfiguration.JsonLdOptions::getIdTemplate)
                                                                 .map(idTemplate -> {
                                                                     String currentUri = StringTemplateFilters.applyTemplate(idTemplate, currentValue, isHtml -> {
                                                                     }, "featureId");
                                                                     currentUri = StringTemplateFilters.applyTemplate(currentUri, transformationContext.getServiceUrl(), isHtml -> {
                                                                     }, "serviceUrl");
                                                                     currentUri = StringTemplateFilters.applyTemplate(currentUri, transformationContext.getCollectionId(), isHtml -> {
                                                                     }, "collectionId");
                                                                     return currentUri;
                                                                 });


                if (jsonLdId.isPresent()) {
                    transformationContext.getJson()
                                         .writeStringField("@id", jsonLdId.get());
                }
            }
        }

        next.accept(transformationContext);
    }

    private void writeContextAndJsonLdType(FeatureTransformationContextGeoJson transformationContext,
                                           String ldContext,
                                           List<String> types) throws IOException {
        writeContextAndJsonLdType(transformationContext, ldContext, types, true);
    }

    private void writeContextAndJsonLdType(FeatureTransformationContextGeoJson transformationContext,
                                           String ldContext,
                                           List<String> types,
                                           boolean writeContext) throws IOException {

        if (writeContext)
            transformationContext.getJson()
                                 .writeStringField("@context",
                                         ldContext.replace("{{serviceUrl}}", transformationContext.getServiceUrl())
                                                  .replace("{{collectionId}}", transformationContext.getCollectionId()));

        if (types.size() == 1) {
            // write @type
            transformationContext.getJson()
                                 .writeStringField("@type", types.get(0));
        } else if (types.size() > 1) {
            // write @type as array
            transformationContext.getJson()
                                 .writeArrayFieldStart("@type");

            for (String type : types) {
                transformationContext.getJson()
                                     .writeString(type);
            }

            transformationContext.getJson()
                                 .writeEndArray();
        }
    }
}

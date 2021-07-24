/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojsonld.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.geojsonld.domain.GeoJsonLdConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.stringtemplates.domain.StringTemplateFilters;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class GeoJsonWriterJsonLd implements GeoJsonWriter {

    List<String> currentTypes;

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
            Optional<GeoJsonLdConfiguration> jsonLdOptions = transformationContext.getApiData()
                                                                                  .getCollections()
                                                                                  .get(transformationContext.getCollectionId())
                                                                                  .getExtension(GeoJsonLdConfiguration.class);

            if (jsonLdOptions.isPresent() && jsonLdOptions.get().isEnabled()) {
                writeContext(transformationContext, jsonLdOptions.get()
                                                                 .getContext());
                writeJsonLdType(transformationContext, ImmutableList.of("geojson:FeatureCollection"));
            }
        }

        currentTypes = ImmutableList.of();

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext,
                               Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        Optional<GeoJsonLdConfiguration> jsonLdOptions = transformationContext.getApiData()
                                                                     .getCollections()
                                                                     .get(transformationContext.getCollectionId())
                                                                     .getExtension(GeoJsonLdConfiguration.class);

        if (jsonLdOptions.isPresent() && jsonLdOptions.get().isEnabled()) {
            if (!transformationContext.isFeatureCollection()) {
                writeContext(transformationContext, jsonLdOptions.get()
                                                                 .getContext());
            }

            currentTypes = jsonLdOptions.map(GeoJsonLdConfiguration::getTypes)
                                        .orElse(ImmutableList.of("geojson:Feature"));

            if (currentTypes.stream().noneMatch(type -> type.contains("{{type}}"))) {
                writeJsonLdType(transformationContext, currentTypes);
                currentTypes = ImmutableList.of();
            }
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onFeatureEnd(FeatureTransformationContextGeoJson transformationContext,
                             Consumer<FeatureTransformationContextGeoJson> next) throws IOException {

        if (!currentTypes.isEmpty()) {
            writeJsonLdType(transformationContext, currentTypes);
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
                && transformationContext.getState()
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
                                                                 .getExtension(GeoJsonLdConfiguration.class)
                                                                 .filter(GeoJsonLdConfiguration::isEnabled)
                                                                 .flatMap(GeoJsonLdConfiguration::getIdTemplate)
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

            if (currentFeatureProperty.isType() && !currentTypes.isEmpty()) {
                currentTypes = currentTypes.stream()
                                           .map(type -> type.replace("{{type}}", currentValue))
                                           .collect(Collectors.toUnmodifiableList());
            }
        }

        next.accept(transformationContext);
    }

    private void writeContext(FeatureTransformationContextGeoJson transformationContext,
                              String ldContext) throws IOException {
        if (Objects.nonNull(ldContext))
            transformationContext.getJson()
                                 .writeStringField("@context",
                                         ldContext.replace("{{serviceUrl}}", transformationContext.getServiceUrl())
                                                  .replace("{{collectionId}}", transformationContext.getCollectionId()));
    }

    private void writeJsonLdType(FeatureTransformationContextGeoJson transformationContext,
                                 List<String> types) throws IOException {

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

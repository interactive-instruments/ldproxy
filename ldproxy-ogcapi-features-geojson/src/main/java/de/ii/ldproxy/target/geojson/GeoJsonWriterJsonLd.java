/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableList;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
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
        return 25;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (transformationContext.isFeatureCollection()) {
            Optional<GeoJsonPropertyMapping> mappingWithLdContext = transformationContext
                    .getApiData()
                    .getFeatureProvider()
                    .getMappings()
                    .get(transformationContext.getCollectionId())
                    .getMappings()
                    .values()
                    .stream()
                    .map(sourcePathMapping -> sourcePathMapping.getMappingForType("application/geo+json"))
                    .filter(targetMapping -> targetMapping instanceof GeoJsonPropertyMapping)
                    .map(targetMapping -> (GeoJsonPropertyMapping) targetMapping)
                    .filter(targetMapping -> Objects.nonNull(targetMapping.getLdContext()))
                    .findFirst();

            if (mappingWithLdContext.isPresent()) {
                writeContextAndJsonLdType(transformationContext,
                        next,
                        mappingWithLdContext.get().getLdContext(),
                        ImmutableList.of("geojson:FeatureCollection"));
            }
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (!transformationContext.isFeatureCollection()) {
            Optional<GeoJsonPropertyMapping> mappingWithLdContext = transformationContext.getState()
                    .getCurrentMapping()
                    .filter(targetMapping -> targetMapping instanceof GeoJsonPropertyMapping)
                    .map(targetMapping -> (GeoJsonPropertyMapping) targetMapping)
                    .filter(targetMapping -> Objects.nonNull(targetMapping.getLdContext()));

            if (mappingWithLdContext.isPresent()) {
                writeContextAndJsonLdType(transformationContext,
                        next,
                        mappingWithLdContext.get().getLdContext(),
                        Optional.ofNullable(mappingWithLdContext.get().getLdType()).orElse(ImmutableList.of()));
            }
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    private void writeContextAndJsonLdType(FeatureTransformationContextGeoJson transformationContext,
                                           Consumer<FeatureTransformationContextGeoJson> next, String ldContext, List<String> types) throws IOException {

        transformationContext.getJson()
                             .writeStringField("@context",
                                     ldContext.replace("{{serviceUrl}}", transformationContext.getServiceUrl())
                                             .replace("{{collectionId}}", transformationContext.getCollectionId()));

        if (types.isEmpty()) {
            // do not write @type
        } else if (types.size()==1) {
            transformationContext.getJson()
                    .writeStringField("@type", types.get(0));
        } else {
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

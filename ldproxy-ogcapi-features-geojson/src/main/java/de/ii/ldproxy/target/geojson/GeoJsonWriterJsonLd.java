/**
 * Copyright 2019 interactive instruments GmbH
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
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext,
                               Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        Optional<GeoJsonPropertyMapping> mappingWithLdContext = transformationContext.getState()
                                                                                     .getCurrentMapping()
                                                                                     .filter(targetMapping -> targetMapping instanceof GeoJsonPropertyMapping)
                                                                                     .map(targetMapping -> (GeoJsonPropertyMapping) targetMapping)
                                                                                     .filter(targetMapping -> Objects.nonNull(targetMapping.getLdContext()));

        if (mappingWithLdContext.isPresent()) {
            transformationContext.getJson()
                                 .writeStringField("@context", mappingWithLdContext.get()
                                                                                   .getLdContext()
                                                                                   .replace("{{serviceUrl}}", transformationContext.getServiceUrl())
                                                                                   .replace("{{collectionId}}", transformationContext.getCollectionId()));

            transformationContext.getJson()
                                 .writeArrayFieldStart("@type");

            for (String type : Optional.ofNullable(mappingWithLdContext.get()
                                                                       .getLdType())
                                       .orElse(ImmutableList.of())) {
                transformationContext.getJson()
                                     .writeString(type);
            }

            transformationContext.getJson()
                                 .writeEndArray();
        }

        // next chain for extensions
        next.accept(transformationContext);
    }
}

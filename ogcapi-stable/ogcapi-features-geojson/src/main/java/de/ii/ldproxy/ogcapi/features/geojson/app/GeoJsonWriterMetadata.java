/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.fasterxml.jackson.core.JsonGenerator;
import de.ii.ldproxy.ogcapi.domain.Link;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.function.Consumer;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoJsonWriterMetadata implements GeoJsonWriter {

    @Override
    public GeoJsonWriterMetadata create() {
        return new GeoJsonWriterMetadata();
    }

    private boolean linksWritten;

    @Override
    public int getSortPriority() {
        return 20;
    }

    private void reset() {
        this.linksWritten = false;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        reset();

        if (transformationContext.isFeatureCollection()) {
            // extendable
            // TODO move isLastPage up to Wfs3Service
            // next chain for links? would allow to defer output until onEnd
            OptionalLong numberReturned = transformationContext.getState()
                                                               .getNumberReturned();
            OptionalLong numberMatched = transformationContext.getState()
                                                              .getNumberMatched();
            boolean isLastPage = numberReturned.orElse(0) < transformationContext.getLimit();

            this.writeLinksIfAny(transformationContext.getJson(), transformationContext.getLinks(), isLastPage);

            if (numberReturned.isPresent()) {
                transformationContext.getJson()
                                     .writeNumberField("numberReturned", numberReturned.getAsLong());
            }
            if (numberMatched.isPresent()) {
                transformationContext.getJson()
                                     .writeNumberField("numberMatched", numberMatched.getAsLong());
            }
            transformationContext.getJson()
                                 .writeStringField("timeStamp", Instant.now()
                                                                       .truncatedTo(ChronoUnit.SECONDS)
                                                                       .toString());
        }

        next.accept(transformationContext);
    }

    @Override
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (!transformationContext.isFeatureCollection()) {
            this.writeLinksIfAny(transformationContext.getJson(), transformationContext.getLinks(), false);
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onFeatureEnd(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        // next chain for extensions
        next.accept(transformationContext);

        if (!transformationContext.isFeatureCollection()) {
            this.writeLinksIfAny(transformationContext.getJson(), transformationContext.getLinks(), false);
        }
    }

    private void writeLinksIfAny(JsonGenerator json, List<Link> links, boolean isLastPage) throws IOException {
        if (!linksWritten && !links.isEmpty()) {
            json.writeFieldName("links");
            json.writeStartArray();

            for (Link link : links) {
                if (!(isLastPage && Objects.equals(link.getRel(), "next"))) {
                    json.writeStartObject();
                    json.writeStringField("href", link.getHref());
                    if (Objects.nonNull(link.getRel()))
                        json.writeStringField("rel", link.getRel());
                    if (Objects.nonNull(link.getType()))
                        json.writeStringField("type", link.getType());
                    if (Objects.nonNull(link.getTitle()))
                        json.writeStringField("title", link.getTitle());
                    json.writeEndObject();
                }
            }

            json.writeEndArray();

            this.linksWritten = true;
        }
    }
}

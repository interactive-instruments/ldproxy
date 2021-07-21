/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.fasterxml.jackson.core.JsonGenerator;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
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

    @Override
    public int getSortPriority() {
        return 20;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {

        if (transformationContext.isFeatureCollection()) {
            OptionalLong numberReturned = transformationContext.getState()
                                                               .getNumberReturned();
            OptionalLong numberMatched = transformationContext.getState()
                                                              .getNumberMatched();

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
}

/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import de.ii.ldproxy.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoJsonWriterId implements GeoJsonWriter {

    @Override
    public GeoJsonWriterId create() {
        return new GeoJsonWriterId();
    }

    private String currentId;
    private boolean currentIdIsInteger;
    private boolean writeAtFeatureEnd = false;

    @Override
    public int getSortPriority() {
        return 10;
    }

    @Override
    public void onFeatureEnd(EncodingAwareContextGeoJson context,
                             Consumer<EncodingAwareContextGeoJson> next) throws IOException {

        if (writeAtFeatureEnd) {
            this.writeAtFeatureEnd = false;

            if (Objects.nonNull(currentId)) {
                if (currentIdIsInteger)
                    context.encoding().getJson()
                                         .writeNumberField("id", Long.parseLong(currentId));
                else
                    context.encoding().getJson()
                                         .writeStringField("id", currentId);
                writeLink(context, currentId);
                this.currentId = null;
                this.currentIdIsInteger = false;
            }
        }

        next.accept(context);
    }

    @Override
    public void onValue(EncodingAwareContextGeoJson context,
                           Consumer<EncodingAwareContextGeoJson> next) throws IOException {

        if (context.schema().isPresent() && Objects.nonNull(context.value())) {
            FeatureSchema currentSchema = context.schema().get();

            if (currentSchema.isId()) {
                String id = context.value();

                boolean isInteger = context.valueType() == Type.INTEGER;
                if (writeAtFeatureEnd) {
                    currentId = id;
                    currentIdIsInteger = isInteger;
                } else {
                    if (isInteger)
                        context.encoding().getJson()
                            .writeNumberField("id", Long.parseLong(id));
                    else
                        context.encoding().getJson()
                            .writeStringField("id", id);

                    writeLink(context, id);
                }
            } else {
                this.writeAtFeatureEnd = true;
            }
        }

        next.accept(context);
    }

    @Override
    public void onCoordinates(EncodingAwareContextGeoJson context,
                              Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        this.writeAtFeatureEnd = true;

        next.accept(context);
    }

    private void writeLink(EncodingAwareContextGeoJson context,
                           String featureId) throws IOException {
        if (context.encoding().isFeatureCollection() &&
                context.encoding().getShowsFeatureSelfLink() &&
                Objects.nonNull(featureId) &&
                !featureId.isEmpty()) {
            context.encoding().getJson()
                                 .writeFieldName("links");
            context.encoding().getJson()
                                 .writeStartArray(1);
            context.encoding().getJson()
                                 .writeStartObject();
            context.encoding().getJson()
                                 .writeStringField("rel", "self");
            context.encoding().getJson()
                                 .writeStringField("href", context.encoding().getServiceUrl() + "/collections/" + context.encoding().getCollectionId() + "/items/" + featureId);
            context.encoding().getJson()
                                 .writeEndObject();
            context.encoding().getJson()
                                 .writeEndArray();
        }
    }
}

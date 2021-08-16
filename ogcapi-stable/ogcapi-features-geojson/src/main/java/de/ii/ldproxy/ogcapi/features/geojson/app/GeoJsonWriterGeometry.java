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
import de.ii.ldproxy.ogcapi.features.geojson.domain.legacy.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import java.io.IOException;
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
public class GeoJsonWriterGeometry implements GeoJsonWriter {

    private boolean geometryOpen;
    private boolean hasPrimaryGeometry;
    private FeatureSchema primaryGeometryProperty;

    @Override
    public GeoJsonWriterGeometry create() {
        return new GeoJsonWriterGeometry();
    }

    @Override
    public int getSortPriority() {
        return 30;
    }

    @Override
    public void onStart(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        this.primaryGeometryProperty = context.schema().flatMap(SchemaBase::getPrimaryGeometry).orElse(null);
    }

    @Override
    public void onFeatureStart(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        this.hasPrimaryGeometry = false;
    }

    @Override
    public void onObjectStart(EncodingAwareContextGeoJson context,
        Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        if (context.schema()
            .filter(SchemaBase::isGeometry)
            .isPresent()
            && context.geometryType().isPresent()) {
            if (context.schema().get().equals(primaryGeometryProperty)) {
                this.hasPrimaryGeometry = true;
                context.encoding().stopBuffering();

                context.encoding().getJson()
                       .writeFieldName("geometry");
            } else {
                context.encoding().getJson()
                       .writeFieldName(context.schema().get().getName());
            }

            context.encoding().getJson()
                .writeStartObject();
            context.encoding().getJson()
                .writeStringField("type", GEO_JSON_GEOMETRY_TYPE.forGmlType(context.geometryType().get()).toString());
            context.encoding().getJson()
                .writeFieldName("coordinates");

            this.geometryOpen = true;
        } else {
            startBufferingIfNecessary(context);
        }

        next.accept(context);
    }

    @Override
    public void onArrayStart(EncodingAwareContextGeoJson context,
        Consumer<EncodingAwareContextGeoJson> next) throws IOException {

        if (geometryOpen) {
            context.encoding().getJson()
                .writeStartArray();
        } else {
            startBufferingIfNecessary(context);
        }

        next.accept(context);
    }

    @Override
    public void onArrayEnd(EncodingAwareContextGeoJson context,
        Consumer<EncodingAwareContextGeoJson> next) throws IOException {

        if (geometryOpen) {
            context.encoding().getJson()
                .writeEndArray();
        }

        next.accept(context);
    }

    @Override
    public void onObjectEnd(EncodingAwareContextGeoJson context,
        Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        if (context.schema()
            .filter(SchemaBase::isGeometry)
            .isPresent()
            && geometryOpen) {

            boolean stopBuffering = context.schema().get().equals(primaryGeometryProperty);
            if (stopBuffering) {
                context.encoding().stopBuffering();
            }

            this.geometryOpen = false;

            //close geometry object
            context.encoding().getJson()
                .writeEndObject();

            if (stopBuffering) {
                context.encoding().flushBuffer();
            }
        }

        next.accept(context);
    }

    @Override
    public void onValue(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {

        if (geometryOpen) {
            context.encoding().getJson()
                .writeRawValue(context.value());
        } else {
            startBufferingIfNecessary(context);
        }

        next.accept(context);
    }


    @Override
    public void onFeatureEnd(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {

        // write null geometry if none was written for this feature
        if (!hasPrimaryGeometry) {
            context.encoding().stopBuffering();

            // null geometry
            context.encoding().getJson()
                .writeFieldName("geometry");
            context.encoding().getJson()
                .writeNull();

            context.encoding().flushBuffer();
        }

        next.accept(context);
    }

    private void startBufferingIfNecessary(EncodingAwareContextGeoJson context) {
        if (!geometryOpen && !hasPrimaryGeometry && !context.encoding().getState()
                                                            .isBuffering()) {
            // buffer properties until primary geometry arrives
            try {
                context.encoding().startBuffering();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}

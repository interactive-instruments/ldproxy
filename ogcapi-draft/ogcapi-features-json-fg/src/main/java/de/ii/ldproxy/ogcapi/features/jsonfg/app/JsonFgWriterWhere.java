/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import de.ii.ldproxy.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.geojson.domain.legacy.GeoJsonGeometryMapping;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.xtraplatform.features.domain.SchemaBase;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

@Component
@Provides
@Instantiate
public class JsonFgWriterWhere implements GeoJsonWriter {

    static public String JSON_KEY = "where";

    @Override
    public JsonFgWriterWhere create() {
        return new JsonFgWriterWhere();
    }

    boolean isEnabled;
    private boolean geometryOpen;
    private boolean hasPrimaryGeometry;
    private boolean suppressWhere;
    private TokenBuffer json;

    @Override
    public int getSortPriority() {
        return 140;
    }

    private void reset(EncodingAwareContextGeoJson context) {
        this.geometryOpen = false;
        this.hasPrimaryGeometry = false;
        this.json = new TokenBuffer(new ObjectMapper(), false);
        if (context.encoding().getPrettify()) {
            json.useDefaultPrettyPrinter();
        }
    }

    @Override
    public void onStart(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        isEnabled = isEnabled(context);

        // set where to null, if the geometry is in WGS84 (in this case it is in "geometry")
        suppressWhere = context.encoding().getTargetCrs().equals(context.encoding().getDefaultCrs());

        next.accept(context);
    }

    @Override
    public void onFeatureStart(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        if (isEnabled)
            reset(context);

        next.accept(context);
    }

    @Override
    public void onObjectStart(EncodingAwareContextGeoJson context,
                              Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        if (isEnabled
                && !suppressWhere
                && context.schema()
                   .filter(SchemaBase::isSpatial)
                   .isPresent()
                && context.geometryType().isPresent()
                && context.schema().get().isPrimaryGeometry()) {

            json.writeFieldName(JSON_KEY);
            json.writeStartObject();
            json.writeStringField("type", GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.forGmlType(context.geometryType().get()).toString());
            json.writeFieldName("coordinates");

            geometryOpen = true;
            hasPrimaryGeometry = true;
        }

        next.accept(context);
    }

    @Override
    public void onArrayStart(EncodingAwareContextGeoJson context,
                             Consumer<EncodingAwareContextGeoJson> next) throws IOException {

        if (geometryOpen) {
            json.writeStartArray();
        }

        next.accept(context);
    }

    @Override
    public void onArrayEnd(EncodingAwareContextGeoJson context,
                           Consumer<EncodingAwareContextGeoJson> next) throws IOException {

        if (geometryOpen) {
            json.writeEndArray();
        }

        next.accept(context);
    }

    @Override
    public void onObjectEnd(EncodingAwareContextGeoJson context,
                            Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        if (context.schema()
                   .filter(SchemaBase::isSpatial)
                   .isPresent()
                && geometryOpen) {

            this.geometryOpen = false;

            //close geometry object
            json.writeEndObject();
        }

        next.accept(context);
    }

    @Override
    public void onValue(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {

        if (geometryOpen) {
            json.writeRawValue(context.value());
        }

        next.accept(context);
    }


    @Override
    public void onFeatureEnd(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {

        if (isEnabled) {
            if (!hasPrimaryGeometry) {
                // write null geometry if none was written for this feature
                json.writeFieldName(JSON_KEY);
                json.writeNull();
            }
            json.serialize(context.encoding().getJson());
            json.flush();
        }

        next.accept(context);
    }

    private boolean isEnabled(EncodingAwareContextGeoJson context) {
        return context.encoding().getApiData()
                                    .getCollections()
                                    .get(context.encoding().getCollectionId())
                                    .getExtension(JsonFgConfiguration.class)
                                    .filter(JsonFgConfiguration::isEnabled)
                                    .filter(cfg -> Objects.requireNonNullElse(Objects.nonNull(cfg.getWhere()) ? Objects.requireNonNullElse(cfg.getWhere()
                                                                                                                                              .getEnabled(), true) : true, true))
                                    .filter(cfg -> cfg.getIncludeInGeoJson().contains(JsonFgConfiguration.OPTION.where) ||
                                            context.encoding().getMediaType().equals(FeaturesFormatJsonFg.MEDIA_TYPE) ||
                                        context.encoding().getMediaType().equals(FeaturesFormatJsonFgCompatibility.MEDIA_TYPE))
                                    .isPresent();
    }
}

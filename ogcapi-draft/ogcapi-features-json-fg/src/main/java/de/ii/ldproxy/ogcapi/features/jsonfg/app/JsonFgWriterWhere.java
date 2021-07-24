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
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.geojson.domain.legacy.GeoJsonGeometryMapping;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.FeatureTransformationContextJsonFgExtension;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.function.Consumer;

@Component
@Provides
@Instantiate
public class JsonFgWriterWhere implements GeoJsonWriter {

    // TODO consolidate with GeoJsonWriterGeometry

    @Override
    public JsonFgWriterWhere create() {
        return new JsonFgWriterWhere();
    }

    boolean isEnabled;
    protected String jsonKey = "where";
    private int geometryNestingDepth = 0;
    private boolean geometryOpen;
    private GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE currentGeometryType;
    private TokenBuffer json;

    @Override
    public int getSortPriority() {
        return 140;
    }

    private void reset(FeatureTransformationContextGeoJson transformationContext) {
        this.geometryNestingDepth = 0;
        this.geometryOpen = false;
        this.json = new TokenBuffer(new ObjectMapper(), false);
        if (transformationContext.getPrettify()) {
            json.useDefaultPrettyPrinter();
        }
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        isEnabled = isEnabled(transformationContext);
        if (isEnabled)
            reset(transformationContext);

        next.accept(transformationContext);
    }

    @Override
    public void onCoordinates(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (isEnabled
                && transformationContext.getState()
                                        .getCurrentGeometryType()
                                        .isPresent()
                && transformationContext.getState()
                                        .getCoordinatesWriterBuilder()
                                        .isPresent()
                && transformationContext.getState()
                                        .getCurrentValue()
                                        .isPresent()) {

            FeatureTransformationContextJsonFgExtension jsonfg = (FeatureTransformationContextJsonFgExtension) transformationContext.getExtensions().get("jsonfg");
            if (Objects.nonNull(jsonfg) && !jsonfg.getSuppressWhere()) {
                this.currentGeometryType = transformationContext.getState()
                                                                .getCurrentGeometryType()
                                                                .get();
                int currentGeometryNestingChange = currentGeometryType == GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.LINE_STRING ? 0 : transformationContext.getState()
                                                                                                                                                               .getCurrentGeometryNestingChange();

                // handle nesting
                if (!geometryOpen) {
                    this.geometryOpen = true;
                    //TODO: to FeatureTransformerGeoJson
                    this.geometryNestingDepth = currentGeometryNestingChange;

                    json.writeFieldName(jsonKey);
                    json.writeStartObject();
                    json.writeStringField("type", currentGeometryType.toString());
                    json.writeFieldName("coordinates");
                    if (currentGeometryType != GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.POINT &&
                            currentGeometryType != GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.MULTI_POINT) {
                        json.writeStartArray();
                    }
                    for (int i = 0; i < currentGeometryNestingChange; i++) {
                        json.writeStartArray();
                    }
                } else if (currentGeometryNestingChange > 0) {
                    for (int i = 0; i < currentGeometryNestingChange; i++) {
                        json.writeEndArray();
                    }
                    for (int i = 0; i < currentGeometryNestingChange; i++) {
                        json.writeStartArray();
                    }
                }

                Writer coordinatesWriter = transformationContext.getState()
                                                                .getCoordinatesWriterBuilder()
                                                                .get()
                                                                .crsTransformer(jsonfg.getCrsTransformerWhere())
                                                                .precision(jsonfg.getGeometryPrecisionWhere())
                                                                .maxAllowableOffset(jsonfg.getMaxAllowableOffsetWhere())
                                                                .isSwapXY(jsonfg.shouldSwapCoordinatesWhere())
                                                                .coordinatesWriter(ImmutableCoordinatesWriterJsonFg.of(json, 2))
                                                                .build();
                coordinatesWriter.write(transformationContext.getState()
                                                             .getCurrentValue()
                                                             .get());
                coordinatesWriter.close();
            }
        }

        next.accept(transformationContext);
    }

    @Override
    public void onFeatureEnd(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (isEnabled) {
            if (geometryOpen) {
                // close geometry field
                this.geometryOpen = false;

                // close nesting braces
                for (int i = 0; i < geometryNestingDepth; i++) {
                    json.writeEndArray();
                }

                this.geometryNestingDepth = 0;

                //close coordinates
                if (currentGeometryType != GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.POINT &&
                        currentGeometryType != GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.MULTI_POINT) {
                    json.writeEndArray();
                }

                //close geometry object
                json.writeEndObject();

                json.serialize(transformationContext.getJson());
                json.flush();
            } else {
                // write null geometry
                json.writeFieldName(jsonKey);
                json.writeNull();
                json.serialize(transformationContext.getJson());
                json.flush();
            }
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    private boolean isEnabled(FeatureTransformationContextGeoJson transformationContext) {
        return transformationContext.getApiData()
                                    .getCollections()
                                    .get(transformationContext.getCollectionId())
                                    .getExtension(JsonFgConfiguration.class)
                                    .filter(JsonFgConfiguration::isEnabled)
                                    .filter(cfg -> Objects.requireNonNullElse(Objects.nonNull(cfg.getWhere()) ? Objects.requireNonNullElse(cfg.getWhere()
                                                                                                                                              .getEnabled(), true) : true, true))
                                    .filter(cfg -> cfg.getIncludeInGeoJson().contains(JsonFgConfiguration.OPTION.where) ||
                                            transformationContext.getMediaType().equals(FeaturesFormatJsonFg.MEDIA_TYPE))
                                    .isPresent();
    }
}

/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiExtension;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorCollectionOpenApi;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessChain;
import de.ii.ldproxy.ogcapi.json.domain.JsonConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.api.DapaResultFormatExtension;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingStatisticalFunction;
import de.ii.ldproxy.ogcapi.observation_processing.data.Geometry;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryMultiPolygon;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryPoint;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class ResultFormatExtensionGeoJson implements DapaResultFormatExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultFormatExtensionGeoJson.class);

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "geo+json"))
            .label("GeoJSON")
            .parameter("json")
            .build();

    private final SchemaGeneratorCollectionOpenApi schemaGeneratorFeatureCollection;

    public ResultFormatExtensionGeoJson(@Requires SchemaGeneratorCollectionOpenApi schemaGeneratorFeatureCollection) {
        this.schemaGeneratorFeatureCollection = schemaGeneratorFeatureCollection;
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Object initializeResult(FeatureProcessChain processes, Map<String, Object> processingParameters, List<Variable> variables, OutputStream outputStream, OgcApiDataV2 apiData) {
        boolean pretty = apiData.getExtension(JsonConfiguration.class)
                                .map(config -> config.getUseFormattedJsonOutput())
                                .orElse(false);
        Result result = new Result(processes.getSubSubPath(), processingParameters, outputStream, pretty);
        result.featureCollection.put("type", "FeatureCollection");
        ArrayNode features = result.featureCollection.putArray("features");
        return result;
    }

    @Override
    public void addFeature(Object result, Optional<String> locationCode, Optional<String> locationName, Geometry geometry, Temporal timeBegin, Temporal timeEnd, Map<String, Number> values) {
        ObjectNode featureCollection = ((Result) result).featureCollection;
        ArrayNode features = (ArrayNode) (featureCollection.get("features"));
        ObjectNode feature = features.addObject();
        feature.put("type", "Feature");
        addGeometry(feature, geometry);
        ObjectNode properties = feature.putObject("properties");
        if (locationCode.isPresent())
            properties.put("locationCode", locationCode.get());
        if (locationName.isPresent())
            properties.put("locationName", locationName.get());
        if (timeBegin==timeEnd)
            properties.put("phenomenonTime", timeBegin.toString());
        else
            properties.put("phenomenonTime", timeBegin.toString()+"/"+timeEnd.toString());
        values.entrySet().parallelStream()
                .forEach(entry -> {
                    String variable = entry.getKey();
                    Number val = entry.getValue();
                    if (val instanceof Integer)
                        properties.put(variable, val.intValue());
                    else
                        properties.put(variable, val.floatValue());
                });
    }

    private void addGeometry(ObjectNode feature, Geometry geometry) {
        ArrayNode coord = feature.putObject("geometry")
                .put("type", geometry instanceof GeometryPoint ? "Point" : ((GeometryMultiPolygon) geometry).size()==1 ? "Polygon" : "MultiPolygon" )
                .putArray("coordinates");
        if (geometry instanceof GeometryPoint) {
            ((GeometryPoint) geometry).asList().stream().forEachOrdered(ord -> coord.add(ord));
        } else {
            GeometryMultiPolygon multiPolygon = (GeometryMultiPolygon) geometry;
            multiPolygon.asList().stream().forEachOrdered(polygon -> {
                ArrayNode coordPoly = multiPolygon.size()==1 ? coord : coord.addArray();
                polygon.stream().forEachOrdered(ring -> {
                    ArrayNode coordRing = coordPoly.addArray();
                    ring.stream().forEachOrdered(pos -> {
                        ArrayNode coordPos = coordRing.addArray();
                        pos.stream().forEach(ord -> coordPos.add(ord));
                    });
                });
            });
        }
    }

    @Override
    public void finalizeResult(Object result) throws IOException {
        ((Result)result).mapper.writeValue(((Result)result).outputStreamWriter, ((Result) result).featureCollection);
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        // TODO specific schemas and example
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(schemaGeneratorFeatureCollection.getSchemaOpenApi())
                .schemaRef(schemaGeneratorFeatureCollection.getSchemaReferenceOpenApi())
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public boolean contentPerApi() {
        return true;
    }

    @Override
    public boolean contentPerResource() {
        return true;
    }

    class Result {
        final String processName;
        final List<String> variables;
        final List<ObservationProcessingStatisticalFunction> functions;
        final List<String> var_funct;
        final OutputStreamWriter outputStreamWriter;
        final ObjectMapper mapper;
        ObjectNode featureCollection;
        Result(String processName, Map<String, Object> processingParameters, OutputStream outputStream, boolean pretty) {
            this.outputStreamWriter = new OutputStreamWriter(outputStream);
            this.processName = processName;
            variables = (List<String>) processingParameters.getOrDefault("variables", ImmutableList.of());
            functions = (List<ObservationProcessingStatisticalFunction>) processingParameters.getOrDefault("functions", ImmutableList.of());
            var_funct = variables.stream()
                    .map(var -> functions.stream().map(funct -> var+"_"+funct.getName()).collect(Collectors.toList()))
                    .flatMap(Collection::stream)
                    .sorted()
                    .collect(Collectors.toList());
            this.mapper = pretty ? new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT) : new ObjectMapper();
            featureCollection = mapper.createObjectNode();
        }
    }
}

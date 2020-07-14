/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessChain;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingOutputFormat;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingStatisticalFunction;
import de.ii.ldproxy.ogcapi.observation_processing.data.Geometry;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryPoint;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Provides(specifications = {OutputFormatCsv.class, ObservationProcessingOutputFormat.class, FormatExtension.class, OgcApiExtension.class})
@Instantiate
public class OutputFormatCsv implements ObservationProcessingOutputFormat {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutputFormatCsv.class);

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("text", "csv"))
            .label("CSV")
            .parameter("csv")
            .build();

    private final Schema schemaCsv;
    public final static String SCHEMA_REF_CSV = "#/components/schemas/csv";

    public OutputFormatCsv() {
        schemaCsv = new StringSchema().example("phenomenonTime,longitude,latitude,TMAX,TMIN,PRCP,SNWD" + System.lineSeparator() +
                "2019-08-11,6.9299617,50.000008,206.35806,103.4677,8.454727,0.0" + System.lineSeparator() +
                "2019-08-10,6.9299617,50.000008,229.26971,136.94968,0.19743806,0.0");
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Object initializeResult(FeatureProcessChain processes, Map<String, Object> processingParameters, OutputStreamWriter outputStreamWriter) throws IOException {
        Result result = new Result(processes.getSubSubPath(), processingParameters, outputStreamWriter);
        switch (result.processName.substring(DAPA_PATH_ELEMENT.length()+2)) {
            case "position":
                result.outputStreamWriter.write("phenomenonTime,"+String.join(",", result.variables)+System.lineSeparator());
                break;
            case "area":
                result.outputStreamWriter.write("longitude,latitude,locationCode,locationName,phenomenonTime,"+String.join(",", result.variables)+System.lineSeparator());
                break;
            case "resample-to-grid":
                result.outputStreamWriter.write("longitude,latitude,phenomenonTime,"+String.join(",", result.variables)+System.lineSeparator());
                break;
            case "position:aggregate-time":
            case "area:aggregate-space-time":
                result.outputStreamWriter.write(String.join(",", result.var_funct)+System.lineSeparator());
                break;
            case "area:aggregate-time":
                result.outputStreamWriter.write("longitude,latitude,locationCode,locationName,"+String.join(",", result.var_funct)+System.lineSeparator());
                break;
            case "resample-to-grid:aggregate-time":
                result.outputStreamWriter.write("longitude,latitude,"+String.join(",", result.var_funct)+System.lineSeparator());
                break;
            case "area:aggregate-space":
                result.outputStreamWriter.write("phenomenonTime,"+String.join(",", result.var_funct)+System.lineSeparator());
                break;
        }
        return result;
    }

    @Override
    public void addFeature(Object entity, Optional<String> locationCode, Optional<String> locationName, Geometry geometry, Temporal timeBegin, Temporal timeEnd, Map<String, Number> values) throws IOException {
        Result result = (Result) entity;
        String phenomenonTime =  timeBegin==timeEnd ? timeBegin.toString() : timeBegin.toString()+"/"+timeEnd.toString();
        List<String> point =  geometry instanceof GeometryPoint ?
            ((GeometryPoint) geometry).asList().stream().map(ord -> String.valueOf(ord)).collect(Collectors.toList()) : ImmutableList.of();
        switch (result.processName.substring(DAPA_PATH_ELEMENT.length()+2)) {
            case "position":
                result.outputStreamWriter.write(phenomenonTime+","+String.join(",", mapValues(result.variables, values))+System.lineSeparator());
                break;
            case "area":
                result.outputStreamWriter.write(String.join(",", point)+","+locationCode.orElse("")+","+locationName.orElse("")+","+phenomenonTime+","+String.join(",", mapValues(result.variables, values))+System.lineSeparator());
                ;
                break;
            case "resample-to-grid":
                result.outputStreamWriter.write(String.join(",", point)+","+phenomenonTime+","+String.join(",", mapValues(result.variables, values))+System.lineSeparator());
                break;
            case "position:aggregate-time":
            case "area:aggregate-space-time":
                result.outputStreamWriter.write(String.join(",", mapValues(result.var_funct, values))+System.lineSeparator());
                break;
            case "area:aggregate-time":
                result.outputStreamWriter.write(String.join(",", point)+","+locationCode.orElse("")+","+locationName.orElse("")+","+String.join(",", mapValues(result.var_funct, values))+System.lineSeparator());
                break;
            case "resample-to-grid:aggregate-time":
                result.outputStreamWriter.write(String.join(",", point)+","+String.join(",", mapValues(result.var_funct, values))+System.lineSeparator());
                break;
            case "area:aggregate-space":
                result.outputStreamWriter.write(phenomenonTime+","+String.join(",", mapValues(result.var_funct, values))+System.lineSeparator());
                break;
        }
    }

    private List<String> mapValues(List<String> headings, Map<String, Number> values) {
        return headings.stream()
                       .map(heading -> values.containsKey(heading) ? String.valueOf(values.get(heading)) : "")
                       .collect(Collectors.toList());
    }

    @Override
    public void finalizeResult(Object result) throws IOException {
        ((Result) result).outputStreamWriter.flush();
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(schemaCsv)
                .schemaRef(SCHEMA_REF_CSV)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public boolean contentPerApi() {
        return false;
    }

    @Override
    public boolean contentPerResource() {
        return false;
    }

    class Result {
        final String processName;
        final List<String> variables;
        final List<ObservationProcessingStatisticalFunction> functions;
        final List<String> var_funct;
        final OutputStreamWriter outputStreamWriter;
        String csv;
        Result(String processName, Map<String, Object> processingParameters, OutputStreamWriter outputStreamWriter) {
            this.outputStreamWriter = outputStreamWriter;
            this.processName = processName;
            variables = (List<String>) processingParameters.getOrDefault("variables", ImmutableList.of());
            functions = (List<ObservationProcessingStatisticalFunction>) processingParameters.getOrDefault("functions", ImmutableList.of());
            var_funct = variables.stream()
                    .map(var -> functions.stream().map(funct -> var+"_"+funct.getName()).collect(Collectors.toList()))
                    .flatMap(Collection::stream)
                    .sorted()
                    .collect(Collectors.toList());
            csv = "";
        }
    }
}

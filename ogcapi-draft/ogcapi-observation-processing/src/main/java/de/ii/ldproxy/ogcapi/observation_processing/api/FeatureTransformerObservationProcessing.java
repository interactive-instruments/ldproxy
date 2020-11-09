/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.api;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformerBase;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcess;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessChain;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.application.ResultFormatExtensionGeoJson;
import de.ii.ldproxy.ogcapi.observation_processing.application.Variable;
import de.ii.ldproxy.ogcapi.observation_processing.data.DataArrayXy;
import de.ii.ldproxy.ogcapi.observation_processing.data.DataArrayXyt;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryPoint;
import de.ii.ldproxy.ogcapi.observation_processing.data.ObservationCollectionArea;
import de.ii.ldproxy.ogcapi.observation_processing.data.ObservationCollectionAreaTimeSeries;
import de.ii.ldproxy.ogcapi.observation_processing.data.ObservationCollectionPoint;
import de.ii.ldproxy.ogcapi.observation_processing.data.ObservationCollectionPointList;
import de.ii.ldproxy.ogcapi.observation_processing.data.ObservationCollectionPointTimeSeries;
import de.ii.ldproxy.ogcapi.observation_processing.data.ObservationCollectionPointTimeSeriesList;
import de.ii.ldproxy.ogcapi.observation_processing.data.Observations;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertySchemaTransformer;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyValueTransformer;
import de.ii.xtraplatform.geometries.domain.ImmutableCoordinatesTransformer;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.streams.domain.HttpClient;
import io.dropwizard.views.ViewRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Vector;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

public class FeatureTransformerObservationProcessing extends FeatureTransformerBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerObservationProcessing.class);

    private OutputStream outputStream;

    private final boolean isFeatureCollection;
    private final ViewRenderer mustacheRenderer;
    private final int pageSize;
    private CrsTransformer crsTransformer;
    private final FeatureTransformationContextObservationProcessing transformationContext;
    private final ObservationProcessingConfiguration configuration;
    private final FeatureProcessChain processes;
    private final Map<String, Object> processingParameters;
    private final List<Variable> variables;
    private final DapaResultFormatExtension outputFormat;
    private final TemporalInterval interval;

    private ImmutableCoordinatesTransformer.Builder coordinatesTransformerBuilder;
    private SimpleFeatureGeometry currentGeometryType;
    private ImmutableCoordinatesTransformer.Builder currentCoordinatesTransformerBuilder;
    private int currentGeometryNesting;

    private Object currentFeature;
    private StringBuilder currentValueBuilder = new StringBuilder();
    private String currentValue = null;
    private FeatureProperty currentProperty = null;
    private ArrayList<FeatureProperty> currentFeatureProperties = null;
    private Observations observations;
    private int observationCount = 0;
    private Float currentResult;
    private Float currentLon;
    private Float currentLat;
    private Temporal currentTime;
    private String currentVar;
    private String currentUom;
    private Integer currentVarIdx;
    private String currentLocationCode;
    private String currentLocationName;
    private String currentId;

    public FeatureTransformerObservationProcessing(FeatureTransformationContextObservationProcessing transformationContext, HttpClient httpClient) {
        super(GeoJsonConfiguration.class,
              transformationContext.getApiData(), transformationContext.getCollectionId(),
              transformationContext.getCodelists(), transformationContext.getServiceUrl(),
              transformationContext.isFeatureCollection());
        this.outputStream = transformationContext.getOutputStream();
        this.isFeatureCollection = transformationContext.isFeatureCollection();
        this.pageSize = transformationContext.getLimit();
        this.crsTransformer = transformationContext.getCrsTransformer()
                                                   .orElse(null);
        this.mustacheRenderer = null; // TODO transformationContext.getMustacheRenderer();
        this.configuration = transformationContext.getConfiguration();
        this.transformationContext = transformationContext;
        this.processes = transformationContext.getProcesses();
        this.processingParameters = transformationContext.getProcessingParameters();
        this.variables = transformationContext.getVariables();
        this.outputFormat = transformationContext.getOutputFormat();
        this.interval = (TemporalInterval) processingParameters.get("interval");

        FeatureTypeConfigurationOgcApi featureType = transformationContext.getApiData()
                .getCollections()
                .get(transformationContext.getCollectionId());
    }

    @Override
    public String getTargetFormat() {
        return ResultFormatExtensionGeoJson.MEDIA_TYPE.toString();
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) {

        if (numberReturned.isPresent()) {
            long returned = numberReturned.getAsLong();
            long matched = numberMatched.orElse(-1);
            LOGGER.debug("numberMatched {}", matched);
            LOGGER.debug("numberReturned {}", returned);
            observations = new Observations((int) returned);
        } else {
            // TODO if numberReturned not present, abort or use the page size as default?
            observations = new Observations(pageSize);
        }

        // TODO if numberMatched is the page size, abort?
    }

    @Override
    public void onEnd() throws IOException {

        LOGGER.debug("{} observations received.", observationCount);

        Object entity = outputFormat.initializeResult(processes, processingParameters, variables, outputStream, transformationContext.getApiData());

        Object data = observations;
        for (FeatureProcess process : processes.asList()) {
            data = process.execute(data, processingParameters);
            LOGGER.debug("Process '{}' completed.", process.getName());
        }

        if (data!=null) {
            if (data instanceof ObservationCollectionPointTimeSeries) {
                ObservationCollectionPointTimeSeries result = (ObservationCollectionPointTimeSeries) data;
                for (Map.Entry<Temporal, ConcurrentMap<String, Number>> entry : result.getValues().entrySet()) {
                    outputFormat.addFeature(entity, result.getCode(), result.getName(), result.getGeometry(),
                            entry.getKey(), entry.getKey(), entry.getValue());
                }
            } else if (data instanceof ObservationCollectionPointTimeSeriesList) {
                ObservationCollectionPointTimeSeriesList result = (ObservationCollectionPointTimeSeriesList) data;
                for (ObservationCollectionPointTimeSeries pos : result) {
                    for (Map.Entry<Temporal, ConcurrentMap<String, Number>> entry : pos.getValues().entrySet()) {
                        outputFormat.addFeature(entity, pos.getCode(), pos.getName(), pos.getGeometry(),
                                entry.getKey(), entry.getKey(), entry.getValue());
                    }
                }
            } else if (data instanceof ObservationCollectionAreaTimeSeries) {
                ObservationCollectionAreaTimeSeries result = (ObservationCollectionAreaTimeSeries) data;
                for (Map.Entry<Temporal, ConcurrentMap<String, Number>> entry : result.getValues().entrySet()) {
                    outputFormat.addFeature(entity, Optional.empty(), Optional.empty(), result.getGeometry(),
                            entry.getKey(), entry.getKey(), entry.getValue());
                }
            } else if (data instanceof ObservationCollectionPoint) {
                ObservationCollectionPoint result = (ObservationCollectionPoint) data;
                outputFormat.addFeature(entity, result.getCode(), result.getName(), result.getGeometry(),
                        result.getInterval().getBegin(), result.getInterval().getEnd(), result.getValues());
            } else if (data instanceof ObservationCollectionPointList) {
                ObservationCollectionPointList result = (ObservationCollectionPointList) data;
                for (ObservationCollectionPoint pos : result) {
                    outputFormat.addFeature(entity, pos.getCode(), pos.getName(), pos.getGeometry(),
                            pos.getInterval().getBegin(), pos.getInterval().getEnd(), pos.getValues());
                }
            } else if (data instanceof ObservationCollectionArea) {
                ObservationCollectionArea result = (ObservationCollectionArea) data;
                outputFormat.addFeature(entity, Optional.empty(), Optional.empty(), result.getGeometry(),
                           result.getInterval().getBegin(), result.getInterval().getEnd(), result.getValues());
            } else if (data instanceof DataArrayXyt) {
                DataArrayXyt result = (DataArrayXyt) data;
                boolean formatAcceptsDataArray = outputFormat.addDataArray(entity, result);
                if (!formatAcceptsDataArray) {
                    Vector<String> vars = result.getVars();
                    for (int i0 = 0; i0 < result.getWidth(); i0++)
                        for (int i1 = 0; i1 < result.getHeight(); i1++)
                            for (int i2 = 0; i2 < result.getSteps(); i2++) {
                                Map<String, Number> map = new HashMap<>();
                                for (int i3 = 0; i3 < vars.size(); i3++)
                                    if (!Float.isNaN(result.array[i2][i1][i0][i3]))
                                        map.put(vars.get(i3), result.array[i2][i1][i0][i3]);
                                LocalDate date = result.date(i2);
                                if (!map.isEmpty())
                                    outputFormat.addFeature(entity, Optional.empty(), Optional.empty(),
                                            new GeometryPoint(result.lon(i0), result.lat(i1)),
                                            date, date, map);
                            }
                }
            } else if (data instanceof DataArrayXy) {
                DataArrayXy result = (DataArrayXy) data;
                boolean formatAcceptsDataArray = outputFormat.addDataArray(entity, result);
                if (!formatAcceptsDataArray) {
                    Vector<String> vars = result.getVars();
                    for (int i0=0; i0<result.getWidth(); i0++)
                        for (int i1=0; i1<result.getHeight(); i1++) {
                            Map<String, Number> map = new HashMap<>();
                            for (int i3 = 0; i3 < vars.size(); i3++)
                                if (!Float.isNaN(result.array[i1][i0][i3]))
                                    map.put(vars.get(i3), result.array[i1][i0][i3]);
                            if (!map.isEmpty())
                                outputFormat.addFeature(entity, Optional.empty(), Optional.empty(),
                                        new GeometryPoint(result.lon(i0), result.lat(i1)),
                                        result.getInterval().getBegin(), result.getInterval().getEnd(), map);
                        }
                }
            }
        }

        outputFormat.finalizeResult(entity);
    }


    @Override
    public void onFeatureStart(FeatureType featureType) {

        currentFeature = null;
        currentResult = null;
        currentLon = null;
        currentLat = null;
        currentTime = null;
        currentVar = null;
        currentUom = null;
        currentVarIdx = null;
        currentLocationCode = null;
        currentLocationName = null;
        currentId = null;
        currentFeatureProperties = new ArrayList<>(featureType.getProperties().values());
    }

    @Override
    public void onFeatureEnd() {

        if (Objects.nonNull(currentLon) && Objects.nonNull(currentLat) &&
            Objects.nonNull(currentTime) && Objects.nonNull(currentVarIdx) &&
            Objects.nonNull(currentResult)) {

            boolean added = observations.addValue(currentId, currentLon, currentLat, currentTime, currentVarIdx, currentResult,
                    currentLocationCode, currentLocationName);
            if (added)
                observationCount++;

        } else {
            // TODO incomplete information, throw error and ignore feature

        }

        currentFeature = null;
    }

    @Override
    public void onPropertyStart(FeatureProperty featureProperty, List<Integer> multiplicities) {
        // TODO current assumptions: no arrays, no object values,
        //      properties "observedProperty", "phenomenonTime", "result", "locationCode", "locationName", "id";
        //      other properties are ignored

        FeatureProperty processedFeatureProperty = featureProperty;
        if (Objects.nonNull(processedFeatureProperty)) {

            List<FeaturePropertySchemaTransformer> schemaTransformations = getSchemaTransformations(processedFeatureProperty);
            for (FeaturePropertySchemaTransformer schemaTransformer : schemaTransformations) {
                processedFeatureProperty = schemaTransformer.transform(processedFeatureProperty);
            }
        }

        switch (processedFeatureProperty.getName()) {
            default:
                currentProperty = null;
                break;
            case "observedProperty":
            case "phenomenonTime":
            case "result":
            case "locationCode":
            case "locationName":
            case "id":
                currentProperty = processedFeatureProperty;
        }
    }

    @Override
    public void onPropertyText(String text) {
        if (Objects.nonNull(currentProperty))
            currentValueBuilder.append(text);
    }

    @Override
    public void onPropertyEnd() throws Exception {
        if (currentValueBuilder.length() > 0) {
            String value = currentValueBuilder.toString();
            List<FeaturePropertyValueTransformer> valueTransformations = getValueTransformations(currentProperty);
            for (FeaturePropertyValueTransformer valueTransformer : valueTransformations) {
                value = valueTransformer.transform(value);
                if (Objects.isNull(value))
                    break;
            }
            // skip, if the value has been transformed to null
            if (Objects.nonNull(value)) {
                switch (currentProperty.getName()) {
                    case "observedProperty":
                        currentVar = value;
                        currentVarIdx = observations.getOrAddVariable(value);
                        break;
                    case "phenomenonTime":
                        currentTime = interval.getTime(value);
                        break;
                    case "result":
                        currentResult = Float.valueOf(value);
                        break;
                    case "locationCode":
                        currentLocationCode = value;
                        break;
                    case "locationName":
                        currentLocationName = value;
                        break;
                    case "id":
                        currentId = value;
                        break;
                }
            }
            currentValueBuilder.setLength(0);
        }

        this.currentProperty = null;

        // reset
        currentValueBuilder.setLength(0);
        currentValue = null;
        currentProperty = null;
    }

    @Override
    public void onGeometryStart(FeatureProperty featureProperty, SimpleFeatureGeometry type, Integer dimension) {
        if (Objects.nonNull(featureProperty)) {

            currentProperty = featureProperty;
            currentGeometryType = type;

            // TODO
            if (type!=SimpleFeatureGeometry.POINT) {
                // TODO throw error
            }

            /* TODO
            ImmutableCoordinatesTransformer.Builder coordinatesTransformerBuilder = ImmutableCoordinatesTransformer.builder();

            if (transformationContext.getCrsTransformer()
                    .isPresent()) {
                coordinatesTransformerBuilder.crsTransformer(transformationContext.getCrsTransformer()
                        .get());
            }

            //TODO: might set dimension in FromSql2?
            int fallbackDimension = Objects.nonNull(dimension) ? dimension : 2;
            coordinatesTransformerBuilder.sourceDimension(transformationContext.getCrsTransformer()
                    .map(CrsTransformer::getSourceDimension)
                    .orElse(fallbackDimension));
            coordinatesTransformerBuilder.targetDimension(transformationContext.getCrsTransformer()
                    .map(CrsTransformer::getTargetDimension)
                    .orElse(fallbackDimension));

            //TODO ext
            if (transformationContext.getMaxAllowableOffset() > 0) {
                int minPoints = currentGeometryType == SimpleFeatureGeometry.MULTI_POLYGON || currentGeometryType == SimpleFeatureGeometry.POLYGON ? 4 : 2;
                coordinatesTransformerBuilder.maxAllowableOffset(transformationContext.getMaxAllowableOffset());
                coordinatesTransformerBuilder.minNumberOfCoordinates(minPoints);
            }

            if (transformationContext.shouldSwapCoordinates()) {
                coordinatesTransformerBuilder.isSwapXY(true);
            }

            if (transformationContext.getGeometryPrecision() > 0) {
                coordinatesTransformerBuilder.precision(transformationContext.getGeometryPrecision());
            }

            if (Objects.equals(featureProperty.isForceReversePolygon(), true)) {
                coordinatesTransformerBuilder.isReverseOrder(true);
            }

            currentCoordinatesTransformerBuilder = coordinatesTransformerBuilder;
            currentGeometryNesting = 0;
             */
            currentValue = "";
        }
    }

    @Override
    public void onGeometryNestedStart() {
        if (Objects.isNull(currentGeometryType))
            return;

        // TODO throw error
        currentGeometryNesting++;
    }

    @Override
    public void onGeometryCoordinates(String text) {
        if (Objects.isNull(currentGeometryType))
            return;

        currentValue += text;
    }

    @Override
    public void onGeometryNestedEnd() {
        // TODO throw error
        currentGeometryNesting--;
    }

    @Override
    public void onGeometryEnd() {
        if (!currentValue.isEmpty()) {
            // TODO points only
            List<String> ords = Splitter.on(Pattern.compile("\\s"))
                    .trimResults()
                    .omitEmptyStrings()
                    .splitToList(Strings.nullToEmpty(currentValue));
            if (ords.size()>=2) {
                currentLon = Float.valueOf(ords.get(0));
                currentLat = Float.valueOf(ords.get(1));
            } else {
                // TODO report error
            }
        }

        currentProperty = null;
        currentGeometryType = null;
        currentValue = null;
    }
}

/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableCollection;
import de.ii.ldproxy.target.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.ldproxy.wfs3.api.FeatureTransformationContext;
import de.ii.xtraplatform.crs.api.CoordinatesWriterType;
import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.OnTheFlyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

import static de.ii.xtraplatform.util.functional.LambdaWithException.consumerMayThrow;

/**
 * @author zahnen
 */
public class FeatureTransformerGeoJson implements FeatureTransformer, FeatureTransformer.OnTheFly {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerGeoJson.class);

    private final ImmutableCollection<GeoJsonWriter> featureWriters;
    private final FeatureTransformationContextGeoJson transformationContext;
    private final StringBuilder stringBuilder;

    @Override
    public OnTheFlyMapping getOnTheFlyMapping() {
        return new GeoJsonOnTheFlyMapping();
    }

    public enum NESTED_OBJECTS {NEST, FLATTEN}

    public enum MULTIPLICITY {ARRAY, SUFFIX}


    public FeatureTransformerGeoJson(FeatureTransformationContextGeoJson transformationContext,
                                     ImmutableCollection<GeoJsonWriter> featureWriters) {
        this.transformationContext = transformationContext;
        this.featureWriters = featureWriters;
        this.stringBuilder = new StringBuilder();
    }

    @Override
    public String getTargetFormat() {
        return Gml2GeoJsonMappingProvider.MIME_TYPE;
    }

    private Consumer<FeatureTransformationContextGeoJson> executePipeline(
            final Iterator<GeoJsonWriter> featureWriterIterator) {
        return consumerMayThrow(nextTransformationContext -> {
            if (featureWriterIterator.hasNext()) {
                featureWriterIterator.next()
                                     .onEvent(nextTransformationContext, this.executePipeline(featureWriterIterator));
            }
        });
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws IOException {
        transformationContext.getState()
                             .setNumberReturned(numberReturned);
        transformationContext.getState()
                             .setNumberMatched(numberMatched);

        transformationContext.getState()
                             .setEvent(FeatureTransformationContext.Event.START);
        executePipeline(featureWriters.iterator()).accept(transformationContext);
    }

    @Override
    public void onEnd() throws IOException {
        transformationContext.getState()
                             .setEvent(FeatureTransformationContext.Event.END);
        executePipeline(featureWriters.iterator()).accept(transformationContext);

        transformationContext.getJson()
                             .close();
    }

    @Override
    public void onFeatureStart(TargetMapping mapping) throws IOException {
        transformationContext.getState()
                             .setCurrentMapping(Optional.ofNullable(mapping));

        transformationContext.getState()
                             .setEvent(FeatureTransformationContext.Event.FEATURE_START);
        executePipeline(featureWriters.iterator()).accept(transformationContext);

        transformationContext.getState()
                             .setCurrentMapping(Optional.empty());
    }

    @Override
    public void onFeatureEnd() throws IOException {

        transformationContext.getState()
                             .setEvent(FeatureTransformationContext.Event.FEATURE_END);
        executePipeline(featureWriters.iterator()).accept(transformationContext);
    }

    @Override
    public void onPropertyStart(TargetMapping mapping, List<Integer> multiplicities) throws IOException {
        if (Objects.nonNull(mapping)) {

            //TODO
            /*if (Objects.nonNull(transformationContext.getState().getCurrentMapping())
                    && Objects.nonNull(transformationContext.getState().getCurrentMapping().getFormat())
                    && Objects.equals(transformationContext.getState().getCurrentMapping().getFormat(), mapping.getFormat())) {
                return;
            }*/

            transformationContext.getState()
                                 .setCurrentMapping(Optional.ofNullable(mapping));
            transformationContext.getState()
                                 .setCurrentMultiplicity(multiplicities);
            //this.currentMapping = (GeoJsonPropertyMapping) mapping;


        }
    }

    @Override
    public void onPropertyText(String text) {
        if (transformationContext.getState()
                                 .getCurrentMapping()
                                 .isPresent()) stringBuilder.append(text);
    }

    @Override
    public void onPropertyEnd() throws IOException {
        if (stringBuilder.length() > 0) {
            transformationContext.getState()
                                 .setCurrentValue(stringBuilder.toString());
            stringBuilder.setLength(0);

            transformationContext.getState()
                                 .setEvent(FeatureTransformationContext.Event.PROPERTY);
            executePipeline(featureWriters.iterator()).accept(transformationContext);
        }

        transformationContext.getState()
                             .setCurrentMapping(Optional.empty());
        transformationContext.getState()
                             .setCurrentValue(Optional.empty());
    }

    @Override
    public void onGeometryStart(TargetMapping mapping, SimpleFeatureGeometry type, Integer dimension) throws
            IOException {
        if (Objects.nonNull(mapping)) {
            //TODO see below (transformationContext.getJsonGenerator())
            //transformationContext.stopBuffering();

            final GeoJsonGeometryMapping geometryMapping = (GeoJsonGeometryMapping) mapping;

            GEO_JSON_GEOMETRY_TYPE currentGeometryType = geometryMapping.getGeometryType();
            if (currentGeometryType == GEO_JSON_GEOMETRY_TYPE.GENERIC) {
                currentGeometryType = GEO_JSON_GEOMETRY_TYPE.forGmlType(type);
            }

            CoordinatesWriterType.Builder cwBuilder = CoordinatesWriterType.builder();
            //cwBuilder.format(new JsonCoordinateFormatter(transformationContext.getJson()));

            if (transformationContext.getCrsTransformer()
                                     .isPresent()) {
                cwBuilder.transformer(transformationContext.getCrsTransformer()
                                                           .get());
            }

            if (dimension != null) {
                cwBuilder.dimension(dimension);
            }

            //TODO ext
            if (transformationContext.getMaxAllowableOffset() > 0) {
                int minPoints = currentGeometryType == GEO_JSON_GEOMETRY_TYPE.MULTI_POLYGON || currentGeometryType == GEO_JSON_GEOMETRY_TYPE.POLYGON ? 4 : 2;
                cwBuilder.simplifier(transformationContext.getMaxAllowableOffset(), minPoints);
            }

            if (transformationContext.shouldSwapCoordinates()) {
                cwBuilder.swap();
            }

            if (transformationContext.getGeometryPrecision() > 0) {
                cwBuilder.precision(transformationContext.getGeometryPrecision());
            }

            transformationContext.getState()
                                 .setCurrentMapping(Optional.ofNullable(mapping));
            transformationContext.getState()
                                 .setCurrentGeometryType(currentGeometryType);
            transformationContext.getState()
                                 .setCoordinatesWriterBuilder(cwBuilder);
        }
    }

    @Override
    public void onGeometryNestedStart() throws IOException {
        if (!transformationContext.getState()
                                  .getCurrentGeometryType()
                                  .isPresent()) return;

        transformationContext.getState()
                             .setCurrentGeometryNestingChange(transformationContext.getState()
                                                                                   .getCurrentGeometryNestingChange() + 1);
    }

    @Override
    public void onGeometryCoordinates(String text) throws IOException {
        if (!transformationContext.getState()
                                  .getCurrentGeometryType()
                                  .isPresent()) return;

        transformationContext.getState()
                             .setCurrentValue(text);

        transformationContext.getState()
                             .setEvent(FeatureTransformationContext.Event.COORDINATES);
        executePipeline(featureWriters.iterator()).accept(transformationContext);

        transformationContext.getState()
                             .setCurrentGeometryNestingChange(0);
    }

    @Override
    public void onGeometryNestedEnd() throws IOException {
        if (!transformationContext.getState()
                                  .getCurrentGeometryType()
                                  .isPresent()) return;
    }

    @Override
    public void onGeometryEnd() throws IOException {
        transformationContext.getState()
                             .setEvent(FeatureTransformationContext.Event.GEOMETRY_END);
        executePipeline(featureWriters.iterator()).accept(transformationContext);

        transformationContext.getState()
                             .setCurrentMapping(Optional.empty());
        transformationContext.getState()
                             .setCurrentValue(Optional.empty());
        transformationContext.getState()
                             .setCurrentGeometryType(Optional.empty());
    }
}
/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.google.common.collect.ImmutableCollection;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformerBase;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.geojson.app.FeaturesFormatGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.legacy.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertySchemaTransformer;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyValueTransformer;
import de.ii.xtraplatform.geometries.domain.ImmutableCoordinatesTransformer;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

import static de.ii.xtraplatform.dropwizard.domain.LambdaWithException.consumerMayThrow;


/**
 * @author zahnen
 */
public class FeatureTransformerGeoJson extends FeatureTransformerBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerGeoJson.class);

    private final ImmutableCollection<GeoJsonWriter> featureWriters;
    private final FeatureTransformationContextGeoJson transformationContext;
    private final StringBuilder stringBuilder;
    private FeatureProperty currentProperty;
    private boolean combineCurrentPropertyValues;

    public FeatureTransformerGeoJson(FeatureTransformationContextGeoJson transformationContext,
                                     ImmutableCollection<GeoJsonWriter> featureWriters) {
        super(GeoJsonConfiguration.class,
              transformationContext.getApiData(), transformationContext.getCollectionId(),
              transformationContext.getCodelists(), transformationContext.getServiceUrl(),
              transformationContext.isFeatureCollection());
        this.transformationContext = transformationContext;
        this.featureWriters = featureWriters;
        this.stringBuilder = new StringBuilder();
    }

    @Override
    public String getTargetFormat() {
        return FeaturesFormatGeoJson.MEDIA_TYPE.type().toString();
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
    public void onFeatureStart(FeatureType featureType) throws IOException {
        transformationContext.getState()
                             .setCurrentFeatureType(Optional.ofNullable(featureType));

        transformationContext.getState()
                             .setEvent(FeatureTransformationContext.Event.FEATURE_START);
        executePipeline(featureWriters.iterator()).accept(transformationContext);

        transformationContext.getState()
                             .setCurrentFeatureType(Optional.empty());
    }

    @Override
    public void onFeatureEnd() throws IOException {

        transformationContext.getState()
                             .setEvent(FeatureTransformationContext.Event.FEATURE_END);
        executePipeline(featureWriters.iterator()).accept(transformationContext);
    }

    @Override
    public void onPropertyStart(FeatureProperty featureProperty, List<Integer> multiplicities) throws IOException {
        FeatureProperty processedFeatureProperty = featureProperty;
        if (Objects.nonNull(processedFeatureProperty)) {

            List<FeaturePropertySchemaTransformer> schemaTransformations = getSchemaTransformations(processedFeatureProperty);
            for (FeaturePropertySchemaTransformer schemaTransformer : schemaTransformations) {
                processedFeatureProperty = schemaTransformer.transform(processedFeatureProperty);
            }

            if (Objects.nonNull(processedFeatureProperty)) {
                transformationContext.getState()
                                     .setCurrentFeatureProperty(Optional.ofNullable(processedFeatureProperty));
                transformationContext.getState()
                                     .setCurrentMultiplicity(multiplicities);

                //TODO: implement double col support as provider transformer and remove this
                if (processedFeatureProperty.hasDoubleColumn()) {
                    if (combineCurrentPropertyValues) {
                        this.combineCurrentPropertyValues = false;
                        stringBuilder.append("|||");
                    } else {
                        this.combineCurrentPropertyValues = true;
                    }
                }
            }
        }

        this.currentProperty = processedFeatureProperty;
    }

    @Override
    public void onPropertyText(String text) {
        if (transformationContext.getState()
                                 .getCurrentFeatureProperty()
                                 .isPresent()) stringBuilder.append(text);
    }

    @Override
    public void onPropertyEnd() throws IOException {
        if (combineCurrentPropertyValues) {
            return;
        }

        if (stringBuilder.length() > 0) {
            String value = stringBuilder.toString();
            List<FeaturePropertyValueTransformer> valueTransformations = getValueTransformations(currentProperty);
            for (FeaturePropertyValueTransformer valueTransformer : valueTransformations) {
                value = valueTransformer.transform(value);
                if (Objects.isNull(value))
                    break;
            }
            // skip, if the value has been transformed to null
            if (Objects.nonNull(value)) {
                transformationContext.getState()
                        .setCurrentValue(value);

                transformationContext.getState()
                        .setEvent(FeatureTransformationContext.Event.PROPERTY);
                executePipeline(featureWriters.iterator()).accept(transformationContext);
            }
            stringBuilder.setLength(0);
        }

        transformationContext.getState()
                             .setCurrentFeatureProperty(Optional.empty());
        transformationContext.getState()
                             .setCurrentValue(Optional.empty());
        this.currentProperty = null;
        this.combineCurrentPropertyValues = false;
    }

    @Override
    public void onGeometryStart(FeatureProperty featureProperty, SimpleFeatureGeometry type, Integer dimension) throws
            IOException {
        if (Objects.nonNull(featureProperty)) {
            //TODO see below (transformationContext.getJsonGenerator())
            //transformationContext.stopBuffering();

            //TODO
            //final GeoJsonGeometryMapping geometryMapping = (GeoJsonGeometryMapping) mapping;

            GEO_JSON_GEOMETRY_TYPE currentGeometryType;// = geometryMapping.getGeometryType();
            //if (currentGeometryType == GEO_JSON_GEOMETRY_TYPE.GENERIC) {
            currentGeometryType = GEO_JSON_GEOMETRY_TYPE.forGmlType(type);
            //} else if (currentGeometryType != GEO_JSON_GEOMETRY_TYPE.forGmlType(type)) {
            //    return;
            //}

            ImmutableCoordinatesTransformer.Builder coordinatesTransformerBuilder = ImmutableCoordinatesTransformer.builder();
            //cwBuilder.format(new JsonCoordinateFormatter(transformationContext.getJson()));

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
                int minPoints = currentGeometryType == GEO_JSON_GEOMETRY_TYPE.MULTI_POLYGON || currentGeometryType == GEO_JSON_GEOMETRY_TYPE.POLYGON ? 4 : 2;
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

            transformationContext.getState()
                                 .setCurrentFeatureProperty(Optional.ofNullable(featureProperty));
            transformationContext.getState()
                                 .setCurrentGeometryType(currentGeometryType);
            transformationContext.getState()
                                 .setCoordinatesWriterBuilder(coordinatesTransformerBuilder);
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
                             .setCurrentFeatureProperty(Optional.empty());
        transformationContext.getState()
                             .setCurrentValue(Optional.empty());
        transformationContext.getState()
                             .setCurrentGeometryType(Optional.empty());
    }
}

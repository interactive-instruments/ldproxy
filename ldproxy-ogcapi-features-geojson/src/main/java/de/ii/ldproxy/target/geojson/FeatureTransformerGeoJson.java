/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.codelists.Codelist;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformations;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.target.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.feature.provider.api.FeatureProperty;
import de.ii.xtraplatform.feature.provider.api.FeatureTransformer2;
import de.ii.xtraplatform.feature.provider.api.FeatureType;
import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.transformer.api.FeaturePropertySchemaTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeaturePropertyValueTransformer;
import de.ii.xtraplatform.feature.transformer.api.OnTheFly;
import de.ii.xtraplatform.feature.transformer.api.OnTheFlyMapping;
import de.ii.xtraplatform.geometries.domain.ImmutableCoordinatesTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static de.ii.xtraplatform.util.functional.LambdaWithException.consumerMayThrow;

/**
 * @author zahnen
 */
public class FeatureTransformerGeoJson implements FeatureTransformer2, OnTheFly {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerGeoJson.class);

    private final ImmutableCollection<GeoJsonWriter> featureWriters;
    private final FeatureTransformationContextGeoJson transformationContext;
    private final StringBuilder stringBuilder;
    private final Optional<FeatureTransformations> baseTransformations;
    private final Optional<FeatureTransformations> geojsonTransformations;
    private FeatureProperty currentProperty;

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

        FeatureTypeConfigurationOgcApi featureType = transformationContext.getApiData()
                .getCollections()
                .get(transformationContext.getCollectionId());
        baseTransformations = Objects.isNull(featureType) ? Optional.empty() : featureType
                .getExtension(OgcApiFeaturesCoreConfiguration.class)
                .map(coreConfiguration -> coreConfiguration);
        geojsonTransformations = Objects.isNull(featureType) ? Optional.empty() : featureType
                .getExtension(GeoJsonConfiguration.class)
                .map(coreConfiguration -> coreConfiguration);
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

            //TODO
            /*if (Objects.nonNull(transformationContext.getState().getCurrentMapping())
                    && Objects.nonNull(transformationContext.getState().getCurrentMapping().getFormat())
                    && Objects.equals(transformationContext.getState().getCurrentMapping().getFormat(), mapping.getFormat())) {
                return;
            }*/

            List<FeaturePropertySchemaTransformer> schemaTransformations = getSchemaTransformations(processedFeatureProperty);
            for (FeaturePropertySchemaTransformer schemaTransformer : schemaTransformations) {
                processedFeatureProperty = schemaTransformer.transform(processedFeatureProperty);
            }

            if (Objects.nonNull(processedFeatureProperty)) {
                transformationContext.getState()
                        .setCurrentFeatureProperty(Optional.ofNullable(processedFeatureProperty));
                transformationContext.getState()
                        .setCurrentMultiplicity(multiplicities);
            }
        }

        currentProperty = processedFeatureProperty;
    }

    @Override
    public void onPropertyText(String text) {
        if (transformationContext.getState()
                                 .getCurrentFeatureProperty()
                                 .isPresent()) stringBuilder.append(text);
    }

    @Override
    public void onPropertyEnd() throws IOException {
        if (stringBuilder.length() > 0) {
            String value = stringBuilder.toString();
            List<FeaturePropertyValueTransformer> valueTransformations = getValueTransformations(currentProperty);
            for (FeaturePropertyValueTransformer valueTransformer : valueTransformations) {
                value = valueTransformer.transform(value);
            }
            transformationContext.getState()
                    .setCurrentValue(value);
            stringBuilder.setLength(0);


            transformationContext.getState()
                                 .setEvent(FeatureTransformationContext.Event.PROPERTY);
            executePipeline(featureWriters.iterator()).accept(transformationContext);
        }

        transformationContext.getState()
                             .setCurrentFeatureProperty(Optional.empty());
        transformationContext.getState()
                             .setCurrentValue(Optional.empty());
    }

    private List<FeaturePropertyValueTransformer> getValueTransformations(FeatureProperty featureProperty) {
        List<FeaturePropertyValueTransformer> valueTransformations = null;
        if (baseTransformations.isPresent()) {
            valueTransformations = baseTransformations.get()
                    .getValueTransformations(new HashMap<String, Codelist>(), transformationContext.getServiceUrl())
                    .get(featureProperty.getName().replaceAll("\\[^\\]+?\\]", "[]"));
        }
        if (geojsonTransformations.isPresent()) {
            if (Objects.nonNull(valueTransformations)) {
                List<FeaturePropertyValueTransformer> moreTransformations = geojsonTransformations.get()
                        .getValueTransformations(new HashMap<String, Codelist>(), transformationContext.getServiceUrl())
                        .get(featureProperty.getName().replaceAll("\\[^\\]+?\\]", "[]"));
                if (Objects.nonNull(moreTransformations)) {
                    valueTransformations = Stream
                            .of(valueTransformations, moreTransformations)
                            .flatMap(Collection::stream)
                            .collect(ImmutableList.toImmutableList());
                }
            } else {
                valueTransformations = geojsonTransformations.get()
                        .getValueTransformations(new HashMap<String, Codelist>(), transformationContext.getServiceUrl())
                        .get(featureProperty.getName().replaceAll("\\[^\\]+?\\]", "[]"));
            }
        }

        return Objects.nonNull(valueTransformations) ? valueTransformations : ImmutableList.of();
    }

    private List<FeaturePropertySchemaTransformer> getSchemaTransformations(FeatureProperty featureProperty) {
        List<FeaturePropertySchemaTransformer> schemaTransformations = null;
        if (baseTransformations.isPresent()) {
            schemaTransformations = baseTransformations.get()
                    .getSchemaTransformations(false)
                    .get(featureProperty.getName().replaceAll("\\[^\\]+?\\]", "[]"));
        }
        if (geojsonTransformations.isPresent()) {
            if (Objects.nonNull(schemaTransformations)) {
                List<FeaturePropertySchemaTransformer> moreTransformations = geojsonTransformations.get()
                        .getSchemaTransformations(false)
                        .get(featureProperty.getName().replaceAll("\\[^\\]+?\\]", "[]"));
                if (Objects.nonNull(moreTransformations)) {
                    schemaTransformations = Stream
                            .of(schemaTransformations, moreTransformations)
                            .flatMap(Collection::stream)
                            .collect(ImmutableList.toImmutableList());
                }
            } else {
                schemaTransformations = geojsonTransformations.get()
                        .getSchemaTransformations(false)
                        .get(featureProperty.getName().replaceAll("\\[^\\]+?\\]", "[]"));
            }
        }

        return Objects.nonNull(schemaTransformations) ? schemaTransformations : ImmutableList.of();
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
            if (dimension != null) {
                coordinatesTransformerBuilder.sourceDimension(transformationContext.getCrsTransformer().map(CrsTransformer::getSourceDimension).orElse(dimension));
                coordinatesTransformerBuilder.targetDimension(transformationContext.getCrsTransformer().map(CrsTransformer::getTargetDimension).orElse(dimension));
            }

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
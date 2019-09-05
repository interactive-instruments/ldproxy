/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.wfs3.api.ImmutableFeatureTransformationContextGeneric;
import de.ii.ldproxy.wfs3.api.Wfs3CollectionFormatExtension;
import de.ii.ldproxy.wfs3.api.Wfs3FeatureFormatExtension;
import de.ii.ldproxy.wfs3.api.Wfs3LinksGenerator;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.api.FeatureStream;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.GmlConsumer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.codahale.metrics.MetricRegistry.name;


/**
 * @author zahnen
 */
@Component
@Provides(specifications = {Wfs3Core.class})
@Instantiate
public class Wfs3Core implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3Core.class);

    private final OgcApiExtensionRegistry extensionRegistry;
    private final MetricRegistry metricRegistry;

    public Wfs3Core(@Requires OgcApiExtensionRegistry extensionRegistry, @Requires Dropwizard dropwizard) {
        this.extensionRegistry = extensionRegistry;

        // TODO: temporary hack so that the ogcapi-features-1/core conformance class can be added, too. Refactoring is required so that the extension registry is not part of Wfs3Core
        this.extensionRegistry.addExtension(this);

        this.metricRegistry = dropwizard.getEnvironment()
                                        .metrics();
    }

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core";
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, Wfs3CoreConfiguration.class);
    }

    public void checkCollectionName(OgcApiDatasetData datasetData, String collectionName) {
        if (!datasetData.isFeatureTypeEnabled(collectionName)) {
            throw new NotFoundException();
        }
    }

    private List<OgcApiCollectionExtension> getCollectionExtenders() {
        return extensionRegistry.getExtensionsForType(OgcApiCollectionExtension.class);
    }

    public OgcApiCollection createCollection(FeatureTypeConfigurationOgcApi featureType, OgcApiDatasetData apiData,
                                             OgcApiMediaType mediaType, List<OgcApiMediaType> alternateMediaTypes,
                                             URICustomizer uriCustomizer, boolean isNested) {
        Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();

        List<OgcApiMediaType> featureMediaTypes = extensionRegistry.getExtensionsForType(Wfs3FeatureFormatExtension.class)
                .stream()
                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
                .map(outputFormatExtension -> outputFormatExtension.getMediaType())
                .collect(Collectors.toList());

        // TODO add support
        Optional<String> describeFeatureTypeUrl = Optional.empty();

        ImmutableOgcApiCollection.Builder collection = ImmutableOgcApiCollection.builder()
                                                                            .id(featureType.getId())
                                                                            .title(featureType.getLabel())
                                                                            .description(featureType.getDescription())
                                                                            .links(wfs3LinksGenerator.generateDatasetCollectionLinks(uriCustomizer.copy(), featureType.getId(), featureType.getLabel(), describeFeatureTypeUrl, mediaType, alternateMediaTypes, featureMediaTypes));

        if (apiData.getFilterableFieldsForFeatureType(featureType.getId())
                       .containsKey("time")) {
            FeatureTypeConfigurationOgcApi.TemporalExtent temporal = featureType.getExtent()
                                                                                .getTemporal();
            collection.extent(new OgcApiExtent(
                    temporal.getStart() == 0 ? -1 : temporal.getStart(),
                    temporal.getEnd() == 0 ? -1 : temporal.getComputedEnd(),
                    featureType.getExtent()
                               .getSpatial()
                               .getXmin(),
                    featureType.getExtent()
                               .getSpatial()
                               .getYmin(),
                    featureType.getExtent()
                               .getSpatial()
                               .getXmax(),
                    featureType.getExtent()
                               .getSpatial()
                               .getYmax()));
        } else {
            collection.extent(new OgcApiExtent(
                    featureType.getExtent()
                               .getSpatial()
                               .getXmin(),
                    featureType.getExtent()
                               .getSpatial()
                               .getYmin(),
                    featureType.getExtent()
                               .getSpatial()
                               .getXmax(),
                    featureType.getExtent()
                               .getSpatial()
                               .getYmax()));
        }

        //TODO: to crs extension
        if (isNested) {
            collection.crs(
                    ImmutableList.<String>builder()
                            .addAll(Stream.of(apiData.getFeatureProvider()
                                    .getNativeCrs()
                                    .getAsUri())
                                    .filter(crsUri -> !crsUri.equalsIgnoreCase(OgcApiDatasetData.DEFAULT_CRS_URI))
                                    .collect(Collectors.toList()))
                            .add(OgcApiDatasetData.DEFAULT_CRS_URI)
                            .addAll(apiData.getAdditionalCrs()
                                    .stream()
                                    .map(EpsgCrs::getAsUri)
                                    .filter(crsUri -> !crsUri.equalsIgnoreCase(OgcApiDatasetData.DEFAULT_CRS_URI))
                                    .collect(Collectors.toList()))
                            .build()
            );
        }

        for (OgcApiCollectionExtension ogcApiCollectionExtension : getCollectionExtenders()) {
            collection = ogcApiCollectionExtension.process(collection, featureType, uriCustomizer.copy(), isNested, apiData);
        }

        return collection.build();
    }

    public Response getItemsResponse(OgcApiDataset dataset, OgcApiRequestContext wfs3Request, String collectionName,
                                     FeatureQuery query, Wfs3FeatureFormatExtension outputFormat,
                                     List<OgcApiMediaType> alternateMediaTypes, boolean onlyHitsIfMore) {
        return getItemsResponse(dataset, wfs3Request, collectionName, query, true, outputFormat, alternateMediaTypes, onlyHitsIfMore);
    }

    public Response getItemResponse(OgcApiDataset dataset, OgcApiRequestContext wfs3Request, String collectionName,
                                    FeatureQuery query, Wfs3FeatureFormatExtension outputFormat,
                                    List<OgcApiMediaType> alternateMediaTypes) {
        return getItemsResponse(dataset, wfs3Request, collectionName, query, false, outputFormat, alternateMediaTypes, false);
    }

    private Response getItemsResponse(OgcApiDataset dataset, OgcApiRequestContext wfs3Request, String collectionName,
                                      FeatureQuery query,
                                      boolean isCollection, Wfs3FeatureFormatExtension outputFormat,
                                      List<OgcApiMediaType> alternateMediaTypes, boolean onlyHitsIfMore) {
        //Wfs3MediaType wfs3MediaType = checkMediaType(mediaType);
        checkCollectionName(dataset.getData(), collectionName);
        Optional<CrsTransformer> crsTransformer = dataset.getCrsTransformer(query.getCrs());

        boolean swapCoordinates = crsTransformer.isPresent() ? crsTransformer.get().needsCoordinateSwap() : dataset.getFeatureProvider()
                                                                        .shouldSwapCoordinates(query.getCrs());

        final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();
        int pageSize = query.getLimit();
        int page = pageSize > 0 ? (pageSize + query.getOffset()) / pageSize : 0;

        List<OgcApiMediaType> collectionMediaTypes = extensionRegistry.getExtensionsForType(Wfs3CollectionFormatExtension.class)
                .stream()
                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(dataset.getData()))
                .map(outputFormatExtension -> outputFormatExtension.getMediaType())
                .collect(Collectors.toList());

        List<OgcApiLink> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(wfs3Request.getUriCustomizer(), isCollection, page, pageSize, wfs3Request.getMediaType(), alternateMediaTypes, collectionMediaTypes);

        ImmutableFeatureTransformationContextGeneric.Builder transformationContext = new ImmutableFeatureTransformationContextGeneric.Builder()
                .serviceData(dataset.getData())
                .collectionName(collectionName)
                .wfs3Request(wfs3Request)
                .crsTransformer(crsTransformer)
                .links(links)
                .isFeatureCollection(isCollection)
                .isHitsOnly(query.hitsOnly())
                .isPropertyOnly(query.propertyOnly())
                .fields(query.getFields())
                .limit(query.getLimit())
                .offset(query.getOffset())
                .maxAllowableOffset(query.getMaxAllowableOffset())
                .geometryPrecision(query.getGeometryPrecision())
                .shouldSwapCoordinates(swapCoordinates)
                .isHitsOnlyIfMore(onlyHitsIfMore);

        StreamingOutput streamingOutput;
        if (wfs3Request.getMediaType()
                       .matches(MediaType.valueOf(dataset.getFeatureProvider()
                                                         .getSourceFormat()))
                && outputFormat.canPassThroughFeatures()) {
            FeatureStream<GmlConsumer> featureStream = dataset.getFeatureProvider()
                                                              .getFeatureStream(query);

            streamingOutput = stream2(featureStream, outputStream -> outputFormat.getFeatureConsumer(transformationContext.outputStream(outputStream)
                                                                                                                          .build())
                                                                                 .get());
        } else if (outputFormat.canTransformFeatures()) {
            FeatureStream<FeatureTransformer> featureTransformStream = dataset.getFeatureProvider()
                                                                              .getFeatureTransformStream(query);

            streamingOutput = stream(featureTransformStream, outputStream -> outputFormat.getFeatureTransformer(transformationContext.outputStream(outputStream)
                                                                                                                                     .build())
                                                                                         .get());
        } else {
            throw new NotAcceptableException();
        }

        return response(streamingOutput, wfs3Request.getMediaType()
                                                    .type()
                                                    .toString());

        //return outputFormat
        //                        .getItemsResponse(getData(), wfs3Request.getMediaType(), getAlternateMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), collectionName, query, featureTransformStream, crsTransformer, wfs3Request.getStaticUrlPrefix(), featureStream);
    }

    private Response response(Object entity, String type) {
        Response.ResponseBuilder response = Response.ok()
                                                    .entity(entity);
        if (type != null) {
            response.type(type);
        }

        return response.build();
    }

    private StreamingOutput stream(FeatureStream<FeatureTransformer> featureTransformStream,
                                   final Function<OutputStream, FeatureTransformer> featureTransformer) {
        Timer.Context timer = metricRegistry.timer(name(Wfs3Core.class, "stream"))
                                            .time();
        Timer.Context timer2 = metricRegistry.timer(name(Wfs3Core.class, "wait"))
                                             .time();

        return outputStream -> {
            try {
                featureTransformStream.apply(featureTransformer.apply(outputStream), timer2)
                                      .toCompletableFuture()
                                      .join();
                timer.stop();
            } catch (CompletionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException) e.getCause();
                }
                throw new IllegalStateException("Feature stream error", e.getCause());
            }
        };
    }

    private StreamingOutput stream2(FeatureStream<GmlConsumer> featureTransformStream,
                                    final Function<OutputStream, GmlConsumer> featureTransformer) {
        return outputStream -> {
            try {
                featureTransformStream.apply(featureTransformer.apply(outputStream), null)
                                      .toCompletableFuture()
                                      .join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException) e.getCause();
                }
                throw new IllegalStateException("Feature stream error", e.getCause());
            }
        };
    }
}

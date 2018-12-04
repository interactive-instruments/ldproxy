/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import akka.Done;
import akka.japi.function.Creator;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3ServiceData;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.Wfs3ConformanceClass;
import de.ii.ldproxy.wfs3.api.Wfs3ExtensionRegistry;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.ldproxy.wfs3.api.Wfs3LinksGenerator;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.ldproxy.wfs3.api.Wfs3Service2;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.core.Wfs3Core;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.entity.api.handler.Entity;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.ImmutableFeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.*;
import de.ii.xtraplatform.crs.api.*;
import de.ii.xtraplatform.entity.api.handler.Entity;
import de.ii.xtraplatform.feature.provider.wfs.FeatureProviderWfs;
import de.ii.xtraplatform.feature.query.api.FeatureProvider;
import de.ii.xtraplatform.feature.query.api.FeatureProviderRegistry;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.feature.query.api.FeatureStream;
import de.ii.xtraplatform.feature.transformer.api.*;
import de.ii.xtraplatform.feature.transformer.geojson.GeoJsonStreamParser;
import de.ii.xtraplatform.feature.transformer.geojson.MappingSwapper;
import de.ii.xtraplatform.service.api.AbstractService;
import de.ii.xtraplatform.service.api.Service;
import org.apache.felix.ipojo.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.namespace.QName;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;


/**
 * @author zahnen
 */
@Component
@Provides
@Entity(entityType = Service.class, dataType = Wfs3ServiceData.class)
// TODO: @Stereotype does not seem to work, maybe test with bnd-ipojo-plugin
// needed to register the ConfigurationHandler when no other properties are set
@HandlerDeclaration("<properties></properties>")

public class Wfs3Service extends AbstractService<Wfs3ServiceData> implements FeatureTransformerService2, Wfs3Service2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3Service.class);

    private static final ExecutorService startupTaskExecutor = MoreExecutors.getExitingExecutorService((ThreadPoolExecutor) Executors.newFixedThreadPool(1));

    @Requires
    private CrsTransformation crsTransformation;

    @Requires
    private FeatureProviderRegistry featureProviderRegistry;

    @Requires
    private Wfs3Core wfs3Core;

    private TransformingFeatureProvider featureProvider;

    private final List<Wfs3ConformanceClass> wfs3ConformanceClasses;
    private final Map<Wfs3MediaType, Wfs3OutputFormatExtension> wfs3OutputFormats;
    private final List<Wfs3StartupTask> wfs3StartupTasks;

    private CrsTransformer defaultTransformer;
    private CrsTransformer defaultReverseTransformer;
    private final Map<String, CrsTransformer> additonalTransformers;
    private final Map<String, CrsTransformer> additonalReverseTransformers;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public Wfs3Service(@Requires Wfs3ExtensionRegistry wfs3ConformanceClassRegistry) {
        super();
        this.wfs3ConformanceClasses = wfs3ConformanceClassRegistry.getConformanceClasses();
        this.wfs3OutputFormats = wfs3ConformanceClassRegistry.getOutputFormats();
        this.wfs3StartupTasks = wfs3ConformanceClassRegistry.getStartupTasks();

        this.additonalTransformers = new LinkedHashMap<>();
        this.additonalReverseTransformers = new LinkedHashMap<>();
    }

    //TODO: setData not called without this
    @Validate
    void onStart() {
        LOGGER.debug("STARTED {} {}", getId(), shouldRegister());
    }


    @Override
    protected ImmutableWfs3ServiceData dataToImmutable(Wfs3ServiceData data) {

        //TODO
        this.featureProvider = (TransformingFeatureProvider) featureProviderRegistry.createFeatureProvider(data.getFeatureProvider());

        ImmutableWfs3ServiceData serviceData;

        try {
            EpsgCrs sourceCrs = data.getFeatureProvider()
                                    .getNativeCrs();
            this.defaultTransformer = crsTransformation.getTransformer(sourceCrs, Wfs3ServiceData.DEFAULT_CRS);
            this.defaultReverseTransformer = crsTransformation.getTransformer(Wfs3ServiceData.DEFAULT_CRS, sourceCrs);


            ImmutableMap<String, FeatureTypeConfigurationWfs3> featureTypesWithComputedBboxes = computeMissingBboxes(data.getFeatureTypes(), featureProvider, defaultTransformer);

            serviceData = ImmutableWfs3ServiceData.builder()
                                                  .from(data)
                                                  .featureTypes(featureTypesWithComputedBboxes)
                                                  .build();

            data.getAdditionalCrs()
                .forEach(crs -> {
                    additonalTransformers.put(crs.getAsUri(), crsTransformation.getTransformer(sourceCrs, crs));
                    additonalReverseTransformers.put(crs.getAsUri(), crsTransformation.getTransformer(crs, sourceCrs));
                });

            LOGGER.debug("TRANSFORMER {} {} -> {} {}", sourceCrs.getCode(), sourceCrs.isForceLongitudeFirst() ? "lonlat" : "latlon", Wfs3ServiceData.DEFAULT_CRS.getCode(), Wfs3ServiceData.DEFAULT_CRS.isForceLongitudeFirst() ? "lonlat" : "latlon");
        } catch (Throwable e) {
            LOGGER.error("CRS transformer could not created"/*, e*/);
            serviceData = ImmutableWfs3ServiceData.copyOf(data);
        }

        ImmutableWfs3ServiceData finalServiceData = serviceData;


        Map<Thread, String> threadMap=null;
        for (Wfs3StartupTask startupTask : wfs3StartupTasks) {
            threadMap= startupTask.getThreadMap();
        }

        if (threadMap != null) {
            for (Map.Entry<Thread, String> entry : threadMap.entrySet()) {
                if(entry.getValue().equals(serviceData.getId())){
                    if(entry.getKey().getState()!= Thread.State.TERMINATED) {
                        entry.getKey().interrupt();
                        wfs3StartupTasks.forEach(wfs3StartupTask -> wfs3StartupTask.removeThreadMapEntry(entry.getKey()));
                    }
                }
            }
        }

        wfs3StartupTasks.forEach(wfs3StartupTask -> startupTaskExecutor.submit(wfs3StartupTask.getTask(finalServiceData,featureProvider)));


        return serviceData;
    }

    @Override
    public Wfs3ServiceData getData() {
        return (Wfs3ServiceData) super.getData();
    }

    public Response getConformanceResponse(Wfs3RequestContext wfs3Request) {
        //Wfs3MediaType wfs3MediaType = checkMediaType(mediaType);

        return wfs3OutputFormats.get(wfs3Request.getMediaType())
                                .getConformanceResponse(wfs3ConformanceClasses, getData().getLabel(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), wfs3Request.getStaticUrlPrefix());
    }

    public Response getDatasetResponse(Wfs3RequestContext wfs3Request, boolean isCollections) {
        //Wfs3MediaType wfs3MediaType = checkMediaType(mediaType);

        return wfs3OutputFormats.get(wfs3Request.getMediaType())
                                .getDatasetResponse(wfs3Core.createCollections(getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer()), getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), wfs3Request.getStaticUrlPrefix(), isCollections);
    }

    public Response getCollectionResponse(Wfs3RequestContext wfs3Request, String collectionName) {
        //Wfs3MediaType wfs3MediaType = checkMediaType(mediaType);
        checkCollectionName(collectionName);

        Wfs3Collection wfs3Collection = wfs3Core.createCollection(getData().getFeatureTypes()
                                                                           .get(collectionName), new Wfs3LinksGenerator(), getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), true);

        return wfs3OutputFormats.get(wfs3Request.getMediaType())
                                .getCollectionResponse(wfs3Collection, getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), collectionName);
    }

    @Override
    public Response getItemsResponse(Wfs3RequestContext wfs3Request, String collectionName, FeatureQuery query) {
        //Wfs3MediaType wfs3MediaType = checkMediaType(mediaType);
        checkCollectionName(collectionName);
        CrsTransformer crsTransformer = getCrsTransformer(query.getCrs());

        final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();
        int pageSize = query.getLimit();
        int page = pageSize > 0 ? (pageSize + query.getOffset()) / pageSize : 0;
        boolean isCollection = wfs3Request.getUriCustomizer()
                                          .isLastPathSegment("items");

        List<Wfs3Link> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(wfs3Request.getUriCustomizer(), isCollection, page, pageSize, wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()));

        ImmutableFeatureTransformationContextGeneric.Builder transformationContext = ImmutableFeatureTransformationContextGeneric.builder()
                                                                                                                                 .serviceData(getData())
                                                                                                                                 .collectionName(collectionName)
                                                                                                                                 .wfs3Request(wfs3Request)
                                                                                                                                 .crsTransformer(crsTransformer)
                                                                                                                                 .links(links)
                                                                                                                                 .isFeatureCollection(isCollection)
                                                                                                                                 .limit(query.getLimit())
                                                                                                                                 .offset(query.getOffset())
                                                                                                                                 .maxAllowableOffset(query.getMaxAllowableOffset());

        StreamingOutput streamingOutput;
        if (wfs3Request.getMediaType()
                       .matches(MediaType.valueOf(getFeatureProvider().getSourceFormat()))
                && wfs3OutputFormats.get(wfs3Request.getMediaType())
                                    .canPassThroughFeatures()) {
            FeatureStream<GmlConsumer> featureStream = getFeatureProvider().getFeatureStream(query);

            streamingOutput = stream2(featureStream, outputStream -> wfs3OutputFormats.get(wfs3Request.getMediaType())
                                                                                      .getFeatureConsumer(transformationContext.outputStream(outputStream)
                                                                                                                               .build())
                                                                                      .get());
        } else if (wfs3OutputFormats.get(wfs3Request.getMediaType())
                                    .canTransformFeatures()) {
            FeatureStream<FeatureTransformer> featureTransformStream = getFeatureProvider().getFeatureTransformStream(query);

            streamingOutput = stream(featureTransformStream, outputStream -> wfs3OutputFormats.get(wfs3Request.getMediaType())
                                                                                              .getFeatureTransformer(transformationContext.outputStream(outputStream)
                                                                                                                                          .build())
                                                                                              .get());
        } else {
            throw new NotAcceptableException();
        }

        return response(streamingOutput, wfs3Request.getMediaType()
                                                    .main()
                                                    .toString());

        //return wfs3OutputFormats.get(wfs3Request.getMediaType())
        //                        .getItemsResponse(getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), collectionName, query, featureTransformStream, crsTransformer, wfs3Request.getStaticUrlPrefix(), featureStream);
    }

    public Response postItemsResponse(Wfs3MediaType mediaType, URICustomizer uriCustomizer, String collectionName, InputStream requestBody) {
        List<String> ids = getFeatureProvider()
                .addFeaturesFromStream(collectionName, defaultReverseTransformer, getFeatureTransformStream(mediaType, collectionName, requestBody));

        if (ids.isEmpty()) {
            throw new BadRequestException("No features found in input");
        }
        URI firstFeature = null;
        try {
            firstFeature = uriCustomizer.copy()
                                        .ensureLastPathSegment(ids.get(0))
                                        .build();
        } catch (URISyntaxException e) {
            //ignore
        }

        return Response.created(firstFeature)
                       .build();
    }

    public Response putItemResponse(Wfs3MediaType mediaType, String collectionName, String featureId, InputStream requestBody) {
        getFeatureProvider().updateFeatureFromStream(collectionName, featureId, defaultReverseTransformer, getFeatureTransformStream(mediaType, collectionName, requestBody));

        return Response.noContent()
                       .build();
    }

    public Response deleteItemResponse(String collectionName, String featureId) {
        getFeatureProvider().deleteFeature(collectionName, featureId);

        return Response.noContent()
                       .build();
    }

    // TODO
    private Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> getFeatureTransformStream(Wfs3MediaType mediaType, String collectionName, InputStream requestBody) {
        return featureTransformer -> {
            MappingSwapper mappingSwapper = new MappingSwapper();
            Sink<ByteString, CompletionStage<Done>> transformer = GeoJsonStreamParser.transform(mappingSwapper.swapMapping(getData().getFeatureProvider()
                                                                                                                                    .getMappings()
                                                                                                                                    .get(collectionName), "SQL"), featureTransformer);
            return StreamConverters.fromInputStream((Creator<InputStream>) () -> requestBody)
                                   .toMat(transformer, Keep.right());

            //return CompletableFuture.completedFuture(Done.getInstance());
        };
    }

    private Wfs3MediaType[] getAlternativeMediaTypes(Wfs3MediaType mediaType) {
        return wfs3OutputFormats.keySet()
                                .stream()
                                .filter(wfs3MediaType -> !wfs3MediaType.equals(mediaType))
                                .toArray(Wfs3MediaType[]::new);
    }

    private Wfs3MediaType checkMediaType(MediaType mediaType) {
        return wfs3OutputFormats.keySet()
                                .stream()
                                .filter(wfs3MediaType -> wfs3MediaType.matches(mediaType))
                                .findFirst()
                                .orElseThrow(NotAcceptableException::new);
    }

    private void checkCollectionName(String collectionName) {
        if (!getData().isFeatureTypeEnabled(collectionName)) {
            throw new NotFoundException();
        }
    }

    public CrsTransformer getCrsTransformer(EpsgCrs crs) {
        CrsTransformer crsTransformer = crs != null ? additonalTransformers.get(crs.getAsUri()) : defaultTransformer;

        if (crsTransformer == null) {
            throw new BadRequestException("Invalid CRS");
        }

        return crsTransformer;
    }

    @Override
    public Optional<FeatureTypeConfiguration> getFeatureTypeByName(String name) {
        return Optional.ofNullable(getData().getFeatureTypes()
                                            .get(name));
    }

    @Override
    public TransformingFeatureProvider getFeatureProvider() {
        return featureProvider;
    }

    @Override
    public BoundingBox transformBoundingBox(BoundingBox bbox) throws CrsTransformationException {
        if (Objects.equals(bbox.getEpsgCrs(), Wfs3ServiceData.DEFAULT_CRS)) {
            return defaultReverseTransformer.transformBoundingBox(bbox);
        }

        return additonalReverseTransformers.get(bbox.getEpsgCrs()
                                                    .getAsUri())
                                           .transformBoundingBox(bbox);
    }


    private Response response(Object entity) {
        return response(entity, null);
    }

    private Response response(Object entity, String type) {
        Response.ResponseBuilder response = Response.ok()
                                                    .entity(entity);
        if (type != null) {
            response.type(type);
        }

        return response.build();
    }

    private StreamingOutput stream(FeatureStream<FeatureTransformer> featureTransformStream, final Function<OutputStream, FeatureTransformer> featureTransformer) {
        return outputStream -> {
            try {
                featureTransformStream.apply(featureTransformer.apply(outputStream))
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

    private StreamingOutput stream2(FeatureStream<GmlConsumer> featureTransformStream, final Function<OutputStream, GmlConsumer> featureTransformer) {
        return outputStream -> {
            try {
                featureTransformStream.apply(featureTransformer.apply(outputStream))
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
//TODO Test
    private ImmutableMap<String, FeatureTypeConfigurationWfs3> computeMissingBboxes(Map<String, FeatureTypeConfigurationWfs3> featureTypes, FeatureProvider featureProvider, CrsTransformer defaultTransformer) throws IllegalStateException{
        return featureTypes
                .entrySet()
                .stream()
                .map(entry -> {

                    if (Objects.isNull(entry.getValue().getExtent().getSpatial())) {
                        boolean isComputed=true;
                        BoundingBox bbox = null;
                        try {
                            bbox = defaultTransformer.transformBoundingBox(featureProvider.getSpatialExtent(entry.getValue()
                                                                                                                 .getId()));
                        } catch ( CrsTransformationException | CompletionException e) {
                              bbox=new BoundingBox(-180.0,-90.0,180.0,90.0, new EpsgCrs(4326,true));
                        }

                        ImmutableFeatureTypeConfigurationWfs3 featureTypeConfigurationWfs3 = ImmutableFeatureTypeConfigurationWfs3.builder()
                                    .from(entry.getValue())
                                    .extent(ImmutableFeatureTypeExtent.builder()
                                            .from(entry.getValue()
                                                    .getExtent())
                                            .spatial(bbox)
                                            .spatialComputed(isComputed)
                                            .build())
                                    .build();


                        return new AbstractMap.SimpleEntry<>(entry.getKey(), featureTypeConfigurationWfs3);
                    }
                    return entry;
                })
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

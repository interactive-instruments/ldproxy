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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.typesafe.config.ConfigException;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.ImmutableFeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.ImmutableFeatureTypeExtent;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3Collections;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3ServiceData;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.Wfs3Collections;
import de.ii.ldproxy.wfs3.api.Wfs3ConformanceClass;
import de.ii.ldproxy.wfs3.api.Wfs3ExtensionRegistry;
import de.ii.ldproxy.wfs3.api.Wfs3Extent;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.ldproxy.wfs3.api.Wfs3LinksGenerator;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.api.Wfs3StartupTask;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
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
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.HandlerDeclaration;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlType;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author zahnen
 */
@Component
@Provides
@Entity(entityType = Service.class, dataType = Wfs3ServiceData.class)
// TODO: @Stereotype does not seem to work, maybe test with bnd-ipojo-plugin
// needed to register the ConfigurationHandler when no other properties are set
@HandlerDeclaration("<properties></properties>")

public class Wfs3Service extends AbstractService<Wfs3ServiceData> implements FeatureTransformerService2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3Service.class);

    private static final ExecutorService startupTaskExecutor = MoreExecutors.getExitingExecutorService((ThreadPoolExecutor) Executors.newFixedThreadPool(1));

    @Requires
    private CrsTransformation crsTransformation;

    @Requires
    private FeatureProviderRegistry featureProviderRegistry;

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
                                .getDatasetResponse(createCollections(getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer()), getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), wfs3Request.getStaticUrlPrefix(), isCollections);
    }

    public Response getCollectionResponse(Wfs3RequestContext wfs3Request, String collectionName) {
        //Wfs3MediaType wfs3MediaType = checkMediaType(mediaType);
        checkCollectionName(collectionName);

        Wfs3Collection wfs3Collection = createCollection(getData().getFeatureTypes()
                                                                  .get(collectionName), new Wfs3LinksGenerator(), getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), true);

        return wfs3OutputFormats.get(wfs3Request.getMediaType())
                                .getCollectionResponse(wfs3Collection, getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), collectionName);
    }

    public Response getItemsResponse(Wfs3RequestContext wfs3Request, String collectionName, FeatureQuery query) {
        //Wfs3MediaType wfs3MediaType = checkMediaType(mediaType);
        checkCollectionName(collectionName);
        CrsTransformer crsTransformer = getCrsTransformer(query.getCrs());
        FeatureStream<FeatureTransformer> featureTransformStream = getFeatureProvider().getFeatureTransformStream(query);
        //TODO
        FeatureStream<GmlConsumer> featureStream = null;
        if (getFeatureProvider() instanceof FeatureProviderWfs) {
            featureStream = ((FeatureProviderWfs) getFeatureProvider()).getFeatureStream(query);
        }

        return wfs3OutputFormats.get(wfs3Request.getMediaType())
                                .getItemsResponse(getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), collectionName, query, featureTransformStream, crsTransformer, wfs3Request.getStaticUrlPrefix(), featureStream);
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

    public BoundingBox transformBoundingBox(BoundingBox bbox) throws CrsTransformationException {
        if (Objects.equals(bbox.getEpsgCrs(), Wfs3ServiceData.DEFAULT_CRS)) {
            return defaultReverseTransformer.transformBoundingBox(bbox);
        }

        return additonalReverseTransformers.get(bbox.getEpsgCrs()
                                                    .getAsUri())
                                           .transformBoundingBox(bbox);
    }

    private Wfs3Collections createCollections(Wfs3ServiceData serviceData, Wfs3MediaType mediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer) {
        final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();

        List<Wfs3Collection> collections = serviceData.getFeatureTypes()
                                                      .values()
                                                      .stream()
                                                      //TODO
                                                      .filter(featureType -> serviceData.isFeatureTypeEnabled(featureType.getId()))
                                                      .sorted(Comparator.comparing(FeatureTypeConfigurationWfs3::getId))
                                                      .map(featureType -> createCollection(featureType, wfs3LinksGenerator, serviceData, mediaType, alternativeMediaTypes, uriCustomizer, false))
                                                      .collect(Collectors.toList());

        ImmutableList<String> crs = ImmutableList.<String>builder()
                .add(serviceData.getFeatureProvider()
                                .getNativeCrs()
                                .getAsUri())
                .add(Wfs3ServiceData.DEFAULT_CRS_URI)
                .addAll(serviceData.getAdditionalCrs()
                                   .stream()
                                   .map(EpsgCrs::getAsUri)
                                   .collect(Collectors.toList()))
                .build();

        List<Wfs3Link> wfs3Links = wfs3LinksGenerator.generateDatasetLinks(uriCustomizer.copy(), Optional.empty()/*new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType()).getAsUrl()*/, mediaType, false,alternativeMediaTypes);


        return ImmutableWfs3Collections.builder()
                                       .collections(collections)
                                       .crs(crs)
                                       .links(wfs3Links)
                                       .build();
    }

    private Wfs3Collection createCollection(FeatureTypeConfigurationWfs3 featureType, Wfs3LinksGenerator wfs3LinksGenerator, Wfs3ServiceData serviceData, Wfs3MediaType mediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, boolean withCrs) {
        final Wfs3Collection collection = new Wfs3Collection();

        final String qn = featureType.getLabel()/*service.getWfsAdapter()
                                                               .getNsStore()
                                                               .getNamespacePrefix(featureType.getNamespace()) + ":" + featureType.getName()*/;

        collection.setName(featureType.getId());
        collection.setTitle(featureType.getLabel());
        collection.setPrefixedName(qn);
        collection.setLinks(wfs3LinksGenerator.generateDatasetCollectionLinks(uriCustomizer.copy(), featureType.getId(), featureType.getLabel(), Optional.empty() /* new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType(ImmutableMap.of(featureType.getNamespace(), ImmutableList.of(featureType.getName())))).getAsUrl()*/, mediaType, alternativeMediaTypes));

        collection.setExtent(new Wfs3Extent());
        if (serviceData.getFilterableFieldsForFeatureType(featureType.getId())
                       .containsKey("time")) {
            collection.setExtent(new Wfs3Extent(
                    featureType.getExtent()
                               .getTemporal()
                               .getStart(),
                    featureType.getExtent()
                               .getTemporal()
                               .getComputedEnd(),
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
            collection.setExtent(new Wfs3Extent(
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

        if (withCrs) {
            collection.setCrs(
                    ImmutableList.<String>builder()
                            .add(serviceData.getFeatureProvider()
                                            .getNativeCrs()
                                            .getAsUri())
                            .add(Wfs3ServiceData.DEFAULT_CRS_URI)
                            .addAll(serviceData.getAdditionalCrs()
                                               .stream()
                                               .map(EpsgCrs::getAsUri)
                                               .collect(Collectors.toList()))
                            .build()
            );
        }
        return collection;
    }
//TODO Test
    private ImmutableMap<String, FeatureTypeConfigurationWfs3> computeMissingBboxes(Map<String, FeatureTypeConfigurationWfs3> featureTypes, FeatureProvider featureProvider, CrsTransformer defaultTransformer) throws IllegalStateException{
        return featureTypes
                .entrySet()
                .stream()
                .map(entry -> {


                    if (Objects.isNull(entry.getValue().getExtent().getSpatial())) {

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
                                            .build())
                                    .build();


                        return new AbstractMap.SimpleEntry<>(entry.getKey(), featureTypeConfigurationWfs3);
                    }
                    return entry;
                })
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

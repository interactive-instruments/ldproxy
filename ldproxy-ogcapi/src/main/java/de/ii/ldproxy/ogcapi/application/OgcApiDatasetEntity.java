/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.application;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ImmutableDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OutputFormatExtension;
import de.ii.ldproxy.ogcapi.domain.Wfs3DatasetMetadataExtension;
import de.ii.ldproxy.ogcapi.domain.Wfs3Link;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.Wfs3StartupTask;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.entity.api.EntityComponent;
import de.ii.xtraplatform.entity.api.handler.Entity;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import de.ii.xtraplatform.feature.transformer.api.TransformingFeatureProvider;
import de.ii.xtraplatform.service.api.AbstractService;
import de.ii.xtraplatform.service.api.Service;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


/**
 * @author zahnen
 */

@EntityComponent
@Entity(entityType = Service.class, dataType = OgcApiDatasetData.class)
public class OgcApiDatasetEntity extends AbstractService<OgcApiDatasetData> implements OgcApiDataset {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiDatasetEntity.class);
    private static final ExecutorService startupTaskExecutor = MoreExecutors.getExitingExecutorService((ThreadPoolExecutor) Executors.newFixedThreadPool(1));

    private final OgcApiExtensionRegistry extensionRegistry;
    //TODO: only needed for wfs3, no?
    private TransformingFeatureProvider featureProvider;
    //TODO: encapsulate
    private CrsTransformer defaultTransformer;
    private CrsTransformer defaultReverseTransformer;
    private final Map<String, CrsTransformer> additionalTransformers;
    private final Map<String, CrsTransformer> additionalReverseTransformers;

    public OgcApiDatasetEntity(@Requires OgcApiExtensionRegistry extensionRegistry,
                               @Property TransformingFeatureProvider featureProvider,
                               @Property CrsTransformer defaultTransformer,
                               @Property CrsTransformer defaultReverseTransformer,
                               @Property Map<String, CrsTransformer> additionalTransformers,
                               @Property Map<String, CrsTransformer> additionalReverseTransformers) {
        this.extensionRegistry = extensionRegistry;
        this.featureProvider = featureProvider;
        this.defaultTransformer = defaultTransformer;
        this.defaultReverseTransformer = defaultReverseTransformer;
        this.additionalTransformers = additionalTransformers;
        this.additionalReverseTransformers = additionalReverseTransformers;

        LOGGER.debug("OgcApiDataset: {} {} {}", featureProvider, defaultTransformer, defaultReverseTransformer);
    }

    //TODO
    @Override
    protected void onStart() {
        List<Wfs3StartupTask> wfs3StartupTasks = getStartupTasks();
        Map<Thread, String> threadMap = null;
        for (Wfs3StartupTask startupTask : wfs3StartupTasks) {
            threadMap = startupTask.getThreadMap();
        }

        if (threadMap != null) {
            for (Map.Entry<Thread, String> entry : threadMap.entrySet()) {
                if (entry.getValue()
                         .equals(getData().getId())) {
                    if (entry.getKey()
                             .getState() != Thread.State.TERMINATED) {
                        entry.getKey()
                             .interrupt();
                        wfs3StartupTasks.forEach(wfs3StartupTask -> wfs3StartupTask.removeThreadMapEntry(entry.getKey()));
                    }
                }
            }
        }

        wfs3StartupTasks.forEach(wfs3StartupTask -> startupTaskExecutor.submit(wfs3StartupTask.getTask(getData(), featureProvider)));

    }

    @Override
    public OgcApiDatasetData getData() {
        return super.getData();
    }

    private List<Wfs3DatasetMetadataExtension> getDatasetExtenders() {
        return extensionRegistry.getExtensionsForType(Wfs3DatasetMetadataExtension.class);
    }

    private List<ConformanceClass> getConformanceClasses() {
        return extensionRegistry.getExtensionsForType(ConformanceClass.class);
    }

    private Map<OgcApiMediaType, OutputFormatExtension> getOutputFormats() {
        return extensionRegistry.getExtensionsForType(OutputFormatExtension.class)
                                .stream()
                                .map(outputFormatExtension -> new AbstractMap.SimpleEntry<>(outputFormatExtension.getMediaType(), outputFormatExtension))
                                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<Wfs3StartupTask> getStartupTasks() {
        return extensionRegistry.getExtensionsForType(Wfs3StartupTask.class);
    }

    private OutputFormatExtension getOutputFormatForService(OgcApiMediaType mediaType) {

        if (!getOutputFormats().get(mediaType)
                              .isEnabledForDataset(getData())) {
            throw new NotAcceptableException();
        }
        return getOutputFormats().get(mediaType);
    }

    private boolean isAlternativeMediaTypeEnabled(OgcApiMediaType mediaType) {

        return getOutputFormats().get(mediaType)
                                 .isEnabledForDataset(getData());
    }

    private boolean isConformanceEnabled(ConformanceClass conformanceClass) {

        return conformanceClass.isEnabledForDataset(getData());
    }

    @Override
    public Response getConformanceResponse(OgcApiRequestContext wfs3Request) {
        //Wfs3MediaType wfs3MediaType = checkMediaType(mediaType);

        List<ConformanceClass> conformanceClasses = getConformanceClasses().stream()
                                                                           .filter(this::isConformanceEnabled)
                                                                           .collect(Collectors.toList());

        return getOutputFormatForService(wfs3Request.getMediaType())
                .getConformanceResponse(conformanceClasses, getData().getLabel(), wfs3Request.getMediaType(), Arrays.asList(getAlternativeMediaTypes(wfs3Request.getMediaType())), wfs3Request.getUriCustomizer(), wfs3Request.getStaticUrlPrefix());
    }

    @Override
    public Response getDatasetResponse(OgcApiRequestContext wfs3Request, boolean isCollections) {
        //Wfs3MediaType wfs3MediaType = checkMediaType(mediaType);

        final DatasetLinksGenerator linksGenerator = new DatasetLinksGenerator();

        //TODO: to crs extension
        ImmutableList<String> crs = ImmutableList.<String>builder()
                .add(getData().getFeatureProvider()
                                .getNativeCrs()
                                .getAsUri())
                .add(OgcApiDatasetData.DEFAULT_CRS_URI)
                .addAll(getData().getAdditionalCrs()
                                   .stream()
                                   .map(EpsgCrs::getAsUri)
                                   .collect(Collectors.toList()))
                .build();


        List<Wfs3Link> wfs3Links = linksGenerator.generateDatasetLinks(wfs3Request.getUriCustomizer().copy(), Optional.empty()/*new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType()).getAsUrl()*/, wfs3Request.getMediaType(), wfs3Request.getAlternativeMediaTypes());


        ImmutableDataset.Builder dataset = new ImmutableDataset.Builder()
                                                           //.collections(collections)
                                                           .crs(crs)
                                                           .links(wfs3Links);



        for (Wfs3DatasetMetadataExtension wfs3DatasetMetadataExtension: getDatasetExtenders()) {
            dataset = wfs3DatasetMetadataExtension.process(dataset, getData(), wfs3Request.getUriCustomizer().copy(), wfs3Request.getMediaType(), wfs3Request.getAlternativeMediaTypes());
        }

        return getOutputFormatForService(wfs3Request.getMediaType())
                .getDatasetResponse(dataset.build(), getData(), wfs3Request.getMediaType(), wfs3Request.getAlternativeMediaTypes(), wfs3Request.getUriCustomizer(), wfs3Request.getStaticUrlPrefix(), isCollections);
    }

    /*
        public Response getCollectionResponse(Wfs3RequestContext wfs3Request, String collectionName) {
            //Wfs3MediaType wfs3MediaType = checkMediaType(mediaType);
            checkCollectionName(collectionName);

            Wfs3Collection wfs3Collection = wfs3Core.createCollection(getData().getFeatureTypes()
                                                                               .get(collectionName), new Wfs3LinksGenerator(), getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType(), getData()), wfs3Request.getUriCustomizer(), true);

            return getOutputFormatForService(wfs3Request.getMediaType())
                    .getCollectionResponse(wfs3Collection, getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType(), getData()), wfs3Request.getUriCustomizer(), collectionName);
        }






    */
    private OgcApiMediaType[] getAlternativeMediaTypes(OgcApiMediaType mediaType) {
        return getOutputFormats().keySet()
                                .stream()
                                .filter(wfs3MediaType -> !wfs3MediaType.equals(mediaType))
                                .filter(this::isAlternativeMediaTypeEnabled)
                                .toArray(OgcApiMediaType[]::new);
    }

    private OgcApiMediaType checkMediaType(MediaType mediaType) {
        return getOutputFormats().keySet()
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

    @Override
    public Optional<CrsTransformer> getCrsTransformer(EpsgCrs crs) {
        if (featureProvider.supportsCrs(crs)) {
            return Optional.empty();
        }

        CrsTransformer crsTransformer = crs != null ? additionalTransformers.get(crs.getAsUri()) : defaultTransformer;

        if (crsTransformer == null) {
            throw new BadRequestException("Invalid CRS");
        }

        return Optional.of(crsTransformer);
    }

    @Override
    public CrsTransformer getCrsReverseTransformer(EpsgCrs crs) {
        CrsTransformer crsTransformer = crs != null ? additionalReverseTransformers.get(crs.getAsUri()) : defaultReverseTransformer;

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
        if (Objects.equals(bbox.getEpsgCrs(), OgcApiDatasetData.DEFAULT_CRS)) {
            return defaultReverseTransformer.transformBoundingBox(bbox);
        }

        return additionalReverseTransformers.get(bbox.getEpsgCrs()
                                                     .getAsUri())
                                            .transformBoundingBox(bbox);
    }

    @Override
    public List<List<Double>> transformCoordinates(List<List<Double>> coordinates,
                                                   EpsgCrs crs) throws CrsTransformationException {
        CrsTransformer transformer = Objects.equals(crs, OgcApiDatasetData.DEFAULT_CRS) ? this.defaultReverseTransformer : additionalReverseTransformers.get(crs.getAsUri());
        if (Objects.nonNull(transformer)) {
            double[] transformed = transformer.transform(coordinates.stream()
                                                                    .flatMap(Collection::stream)
                                                                    .mapToDouble(Double::doubleValue)
                                                                    .toArray(), coordinates.size());
            List<List<Double>> result = new ArrayList<>();
            for (int i = 0; i < transformed.length; i += 2) {
                result.add(ImmutableList.of(transformed[i], transformed[i + 1]));
            }

            return result;
        }

        return coordinates;
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

}

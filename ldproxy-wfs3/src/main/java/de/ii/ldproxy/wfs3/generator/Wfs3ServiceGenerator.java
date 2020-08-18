/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.generator;

import de.ii.ldproxy.ogcapi.domain.OgcApiApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.xtraplatform.entity.api.EntityData;
import de.ii.xtraplatform.event.store.EntityDataStore;
import de.ii.xtraplatform.scheduler.api.Scheduler;
import de.ii.xtraplatform.scheduler.api.TaskQueue;
import de.ii.xtraplatform.scheduler.api.TaskStatus;
import de.ii.xtraplatform.service.api.ServiceBackgroundTasks;
import de.ii.xtraplatform.service.api.ServiceGenerator;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.ServiceReference;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
@Component
@Provides(properties = {
//        @StaticServiceProperty(name = Entity.TYPE_KEY, type = "java.lang.String", value = Service.TYPE),
//        @StaticServiceProperty(name = Entity.SUB_TYPE_KEY, type = "java.lang.String", value = OgcApiApiDataV2.SERVICE_TYPE)
})
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.ldproxy.ogcapi.domain.OgcApiDataset)",
        onArrival = "onArrival",
        onDeparture = "onDeparture",
        onModification = "onModification")
public class Wfs3ServiceGenerator implements ServiceGenerator<OgcApiApiDataV2>, ServiceBackgroundTasks {

    // TODO split into ProviderGenerator + ServiceGenerator

   /* private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3ServiceGenerator.class);

    @Context
    private BundleContext bundleContext;

    @Requires
    private OgcApiExtensionRegistry wfs3ConformanceClassRegistry;

    @Requires
    private FeatureProviderRegistry featureProviderRegistry;

    @Requires
    private EntityRepository entityRepository;

    private final EntityDataStore<ServiceData> serviceRepository;

    @Requires
    private KeyValueStore rootKeyValueStore;*/

    private final TaskQueue taskQueue;

    Wfs3ServiceGenerator(
            @Requires EntityDataStore<EntityData> entityRepository,
            @Requires Scheduler scheduler) {
        //this.serviceRepository = entityRepository.forType(ServiceData.class);
        this.taskQueue = scheduler.createQueue(this.getType()
                                                   .getSimpleName());
    }

    @Override
    public Class<OgcApiApiDataV2> getType() {
        return OgcApiApiDataV2.class;
    }

    //TODO: depends on provider, this is for WFS based services
    @Override
    public OgcApiApiDataV2 generate(Map<String, String> partialData) {
/*
        if (Objects.isNull(partialData) || !partialData.containsKey("id") || !partialData.containsKey("url")) {
            throw new BadRequestException();
        }

        OgcApiConfigPreset preset = OgcApiConfigPreset.fromString(partialData.get("preset"));

        try {

            ImmutableConnectionInfoWfsHttp.Builder connectionInfoBuilder = new ImmutableConnectionInfoWfsHttp.Builder().uri(URI.create(partialData.get("url")))
                                                                                                                       .connectionUri(partialData.get("url"))
                                                                                                                       .method(ConnectionInfoWfsHttp.METHOD.GET)
                                                                                                                       .version("2.0.0")
                                                                                                                       .gmlVersion("3.2.1");

            if (partialData.containsKey("isBasicAuth") && Objects.equals(partialData.get("isBasicAuth"), "true")) {
                connectionInfoBuilder.user(partialData.get("baUser"))
                                     .password(partialData.get("baPassword"));
            }

            ImmutableFeatureProviderDataTransformer provider = new ImmutableFeatureProviderDataTransformer.Builder()
                    .providerType("WFS")
                    .connectorType("HTTP")
                    .connectionInfo(connectionInfoBuilder.build())
                    .nativeCrs(new EpsgCrs())
                    .mappingStatus(new ImmutableMappingStatus.Builder().enabled(true)
                                                                       .supported(false)
                                                                       .refined(false)
                                                                       .build())
                    .build();

            ImmutableOgcApiDatasetData.Builder wfs3ServiceData = new ImmutableOgcApiDatasetData.Builder()
                    .id(partialData.get("id"))
                    .shouldStart(true)
                    .serviceType("WFS3")
                    .featureProvider(provider);

            wfs3ConformanceClassRegistry.getExtensions()
                                        .stream()
                                        .filter(wfs3Extension -> wfs3Extension instanceof OgcApiCapabilityExtension)
                                        .sorted(Comparator.comparing(wfs3Extension -> wfs3Extension.getClass()
                                                                                                   .getSimpleName()))
                                        .map(wfs3Extension -> (OgcApiCapabilityExtension) wfs3Extension)
                                        .forEach(wfs3Capability -> {
                                            wfs3ServiceData.addCapabilities(wfs3Capability.getDefaultConfiguration(preset));
                                        });


            FeatureProvider2 featureProvider = featureProviderRegistry.createFeatureProvider(provider);

            if (!(featureProvider instanceof FeatureMetadata && featureProvider instanceof FeatureSchema)) {
                throw new IllegalArgumentException("feature provider not metadata aware");
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Generating service of type {} with id {}", OgcApiDatasetData.class, partialData.get("id"));
            }

            FeatureMetadata metadataAware = (FeatureMetadata) featureProvider;
            FeatureProviderGenerator dataGenerator = (FeatureProviderGenerator) featureProvider;

            FeatureProviderMetadataConsumer metadataConsumer = new MultiFeatureProviderMetadataConsumer(new Metadata2Wfs3(wfs3ServiceData), dataGenerator.getDataGenerator(provider, wfs3ServiceData.featureProviderBuilder()));
            //TODO: getMetadata(partialData.getFeatureProvider(), FeatureProviderMetadataConsumer... additionalConsumers)
            metadataAware.getMetadata(metadataConsumer);

            //TODO: only if gsfs is enabled???
            wfs3ServiceData.addAdditionalCrs(new EpsgCrs(3857), new EpsgCrs(102100), new EpsgCrs(4326, true));

            return wfs3ServiceData.build();
        } catch (Throwable e) {
            if (e instanceof WebApplicationException) {
                throw e;
            }
            if (e instanceof CompletionException) {
                throw new IllegalStateException(e.getCause());
            }
            throw new IllegalStateException(e);
        }*/
return null;
    }

    /*private List<TargetMappingProviderFromGml> getMappingProviders() {
        return Stream.concat(
                Stream.of(new Gml2Wfs3GenericMappingProvider()),
                wfs3ConformanceClassRegistry.getExtensionsForType(OgcApiFeatureFormatExtension.class)
                                            .stream()
                                            .map(OgcApiFeatureFormatExtension::getMappingGenerator)
                                            .filter(Optional::isPresent)
                                            .map(Optional::get)
        )
                     .collect(Collectors.toList());
    }

    private List<TargetMappingRefiner> getMappingRefiners() {
        return wfs3ConformanceClassRegistry.getExtensionsForType(OgcApiFeatureFormatExtension.class)
                                           .stream()
                                           .map(OgcApiFeatureFormatExtension::getMappingRefiner)
                                           .filter(Optional::isPresent)
                                           .map(Optional::get)
                                           .collect(Collectors.toList());
    }*/

    private synchronized void onArrival(ServiceReference<OgcApiApi> ref) {
        /*try {
            checkGenerateMapping(ref);
            checkRefineMapping(ref);
        } catch (Throwable e) {
            LOGGER.error("Error starting a service. ", e);
        }*/
    }

    private synchronized void onDeparture(ServiceReference<OgcApiApi> ref) {
    }

    private synchronized void onModification(ServiceReference<OgcApiApi> ref) {
        /*try {
            checkGenerateMapping(ref);
            checkRefineMapping(ref);
        } catch (Throwable e) {
            LOGGER.error("Error modifying a service. ", e);
        }*/
    }

    /*private void checkGenerateMapping(ServiceReference<OgcApiDataset> ref) {
        final OgcApiDataset ogcApiDataset = bundleContext.getService(ref);

        if (Objects.nonNull(ogcApiDataset)) {
            final FeatureProviderDataTransformer featureProviderData = ogcApiDataset.getData()
                                                                                  .getFeatureProvider();

            if (canGenerateMapping(ogcApiDataset.getFeatureProvider()) && shouldGenerateMapping(featureProviderData
                    .getMappingStatus(), ogcApiDataset.getId())) {

                taskQueue.launch(new AnalyzeSchemaTask(ogcApiDataset, featureProviderData));
            }
        }
    }

    private void checkRefineMapping(ServiceReference<OgcApiDataset> ref) {
        final OgcApiDataset ogcApiDataset = bundleContext.getService(ref);

        if (Objects.nonNull(ogcApiDataset)) {
            final FeatureProviderDataTransformer featureProviderData = ogcApiDataset.getData()
                                                                                  .getFeatureProvider();
            final List<TargetMappingRefiner> mappingRefiners = getMappingRefiners();

            if (ogcApiDataset.getFeatureProvider()
                           .supportsQueries()
                    && canGenerateMapping(ogcApiDataset.getFeatureProvider())
                    && shouldRefineMapping(ogcApiDataset.getData(), featureProviderData.getMappingStatus(), ogcApiDataset.getId(), mappingRefiners)) {

                taskQueue.launch(new AnalyzeDataTask(ogcApiDataset, featureProviderData, mappingRefiners));
            }
        }
    }

    private boolean canGenerateMapping(FeatureProvider2 featureProvider) {
        return featureProvider instanceof FeatureSchema && featureProvider instanceof FeatureProviderGenerator;
    }

    private boolean shouldGenerateMapping(MappingStatus mappingStatus, String id) {
        return mappingStatus.getLoading()
                && !getCurrentTask().filter(task -> task.getId()
                                                        .equals(id) && task.getLabel()
                                                                           .equals(AnalyzeSchemaTask.LABEL))
                                    .isPresent()
                && taskQueue.getFutureTasks()
                            .stream()
                            .noneMatch(task -> task.getId()
                                                   .equals(id) && task.getLabel()
                                                                      .equals(AnalyzeSchemaTask.LABEL));
    }

    private boolean shouldRefineMapping(OgcApiDatasetData data, MappingStatus mappingStatus, String id,
                                        List<TargetMappingRefiner> mappingRefiners) {
        if (mappingStatus.getRefined()
                || shouldGenerateMapping(mappingStatus, id)
                || getCurrentTask().filter(task -> task.getId()
                                                       .equals(id) && task.getLabel()
                                                                          .equals(AnalyzeDataTask.LABEL))
                                   .isPresent()
                || taskQueue.getFutureTasks()
                            .stream()
                            .anyMatch(task -> task.getId()
                                                  .equals(id) && task.getLabel()
                                                                     .equals(AnalyzeDataTask.LABEL))) {
            return false;
        }

        for (FeatureTypeConfigurationOgcApi featureType : data.getFeatureTypes()
                                                              .values()) {
            if (!data.isFeatureTypeEnabled(featureType.getId())) {
                continue;
            }

            boolean needsRefinement = data.getFeatureProvider()
                                          .getMappings()
                                          .containsKey(featureType.getId()) &&
                    data.getFeatureProvider()
                        .getMappings()
                        .get(featureType.getId())
                        .getMappingsWithPathAsList()
                        .values()
                        .stream()
                        .anyMatch(sourcePathMapping -> mappingRefiners.stream()
                                                                      .anyMatch(targetMappingRefiner -> targetMappingRefiner.needsRefinement(sourcePathMapping)));
            if (needsRefinement) {
                return true;
            }
        }

        return false;
    }*/


    private Map<String, TaskStatus> lastTasks = new HashMap<>();

    @Override
    public Optional<TaskStatus> getCurrentTask() {
        return taskQueue.getCurrentTask();
    }

    @Override
    public Optional<TaskStatus> getCurrentTaskForService(String id) {
        Optional<TaskStatus> optionalTaskStatus = taskQueue.getCurrentTask()
                                                           .filter(taskStatus -> taskStatus.getId()
                                                                                           .equals(id));
        if (optionalTaskStatus.isPresent()) {
            lastTasks.put(id, optionalTaskStatus.get());
        } else {
            optionalTaskStatus = Optional.ofNullable(lastTasks.get(id))
                                         .filter(taskStatus -> Instant.now()
                                                                      .toEpochMilli() - taskStatus.getEndTime() < 10000);
        }

        return optionalTaskStatus;
    }

    /*private class AnalyzeSchemaTask implements Task {

        static final String LABEL = "Analyzing schema";

        private final OgcApiDataset wfs3Service;
        private final FeatureProviderDataTransformer featureProviderData;

        public AnalyzeSchemaTask(OgcApiDataset wfs3Service, FeatureProviderDataTransformer featureProviderData) {
            this.wfs3Service = wfs3Service;
            this.featureProviderData = featureProviderData;
        }

        @Override
        public String getId() {
            return wfs3Service.getId();
        }

        @Override
        public String getLabel() {
            return LABEL;
        }

        @Override
        public void run(TaskContext taskContext) {

            //TODO: to onServiceStart queue
            //TODO: set mappingStatus.enabled to false for catalog services
            //TODO: reactivate on-the-fly mapping
            FeatureSchema schemaAware = (FeatureSchema) wfs3Service.getFeatureProvider();
            FeatureProviderGenerator dataGenerator = (FeatureProviderGenerator) wfs3Service.getFeatureProvider();
            //TODO: getSchema(partialData.getFeatureProvider(), getMappingProviders(), FeatureProviderSchemaConsumer... additionalConsumers)

            ImmutableFeatureProviderDataTransformer.Builder builder = new ImmutableFeatureProviderDataTransformer.Builder().from(featureProviderData);

            schemaAware.getSchema(dataGenerator.getMappingGenerator(featureProviderData, builder, getMappingProviders()), featureProviderData.getLocalFeatureTypeNames(), taskContext);

            ImmutableOgcApiDatasetData updated = new ImmutableOgcApiDatasetData.Builder().from(wfs3Service.getData())
                                                                                         .featureProvider(builder.build())
                                                                                         .build();

            serviceRepository.put(wfs3Service.getId(), updated);

            //TODO: generate GSFS styles
            KeyValueStore stylesStore = rootKeyValueStore.getChildStore("styles")
                                                         .getChildStore(wfs3Service.getData()
                                                                                   .getId());

            for (OgcApiStyleGeneratorExtension wfs3StyleGeneratorExtension : wfs3ConformanceClassRegistry.getExtensionsForType(OgcApiStyleGeneratorExtension.class)) {
                int i = 0;
                for (FeatureTypeConfigurationOgcApi featureTypeConfiguration : updated.getFeatureTypes()
                                                                                      .values()) {

                    String style = wfs3StyleGeneratorExtension.generateStyle(updated, featureTypeConfiguration, i++);

                    if (!Strings.isNullOrEmpty(style)) {
                        String styleId = "gsfs_" + featureTypeConfiguration.getId();
                        WriteTransaction<String> writeTransaction = stylesStore.openWriteTransaction(styleId);
                        try {
                            writeTransaction.write(style);
                            writeTransaction.execute();
                            writeTransaction.commit();
                        } catch (IOException e) {
                            writeTransaction.rollback();
                        }
                    }
                }
            }
        }
    }

    private class AnalyzeDataTask implements Task {

        static final String LABEL = "Analyzing data";

        private final OgcApiDataset wfs3Service;
        private final FeatureProviderDataTransformer featureProviderData;
        private final List<TargetMappingRefiner> mappingRefiners;

        public AnalyzeDataTask(OgcApiDataset wfs3Service, FeatureProviderDataTransformer featureProviderData,
                               List<TargetMappingRefiner> mappingRefiners) {
            this.wfs3Service = wfs3Service;
            this.featureProviderData = featureProviderData;
            this.mappingRefiners = mappingRefiners;
        }

        @Override
        public String getId() {
            return wfs3Service.getId();
        }

        @Override
        public String getLabel() {
            return LABEL;
        }

        @Override
        public void run(TaskContext taskContext) {

            double factor = 1.0 / wfs3Service.getData()
                                             .getFeatureTypes()
                                             .size();

            double progress = 0.0;
            final int[] i = {0};

            ImmutableFeatureProviderDataTransformer.Builder builder = new ImmutableFeatureProviderDataTransformer.Builder().from(featureProviderData);

            for (FeatureTypeConfigurationOgcApi featureTypeConfigurationOgcApi : wfs3Service.getData()
                                                                                            .getFeatureTypes()
                                                                                            .values()) {
                final boolean[] hasData = {false};
                final boolean[] foundGeometry = {false};
                final SourcePathMapping[] currentMapping = new SourcePathMapping[1];
                final List<String>[] currentGeometryPath = new List[1];
                final ImmutableSourcePathMapping.Builder[] currentBuilder = new ImmutableSourcePathMapping.Builder[1];
                final SimpleFeatureGeometry[] currentGeometryType = new SimpleFeatureGeometry[1];

                final ImmutableFeatureTypeMapping.Builder mappingBuilder = builder.getMappings()
                                                                                  .get(featureTypeConfigurationOgcApi.getId());

                taskContext.setStatusMessage(featureTypeConfigurationOgcApi.getLabel());

                FeatureQuery query = ImmutableFeatureQuery.builder()
                                                          .type(featureTypeConfigurationOgcApi.getId())
                                                          .limit(5)
                                                          .build();

                FeatureSourceStream featureStream = wfs3Service.getFeatureProvider()
                                                               .passThrough()
                                                               .getFeatureSourceStream(query);
                featureStream.runWith(new FeatureConsumer() {
                    @Override
                    public void onStart(OptionalLong numberReturned,
                                        OptionalLong numberMatched,
                                        Map<String, String> additionalInfos) throws Exception {

                    }

                    @Override
                    public void onEnd() throws Exception {

                    }

                    @Override
                    public void onFeatureStart(List<String> path,
                                               Map<String, String> additionalInfos) throws Exception {
                        hasData[0] = true;
                    }

                    @Override
                    public void onFeatureEnd(List<String> path) throws Exception {

                    }

                    @Override
                    public void onPropertyStart(List<String> path,
                                                List<Integer> multiplicities,
                                                Map<String, String> additionalInfos) throws Exception {
                        if (foundGeometry[0]) {
                            return;
                        }

                        Optional<SourcePathMapping> optionalSourcePathMapping = Optional.ofNullable(featureProviderData.getMappings()
                                                                                                                       .get(featureTypeConfigurationOgcApi.getId())
                                                                                                                       .getMappingsWithPathAsList()
                                                                                                                       .get(path));
                        boolean isSpatial = optionalSourcePathMapping.map(sourcePathMapping -> sourcePathMapping.getMappingForType(TargetMapping.BASE_TYPE))
                                                                     .filter(TargetMapping::isSpatial)
                                                                     .isPresent();

                        if (isSpatial) {

                            currentGeometryPath[0] = ImmutableList.copyOf(path);
                            currentMapping[0] = optionalSourcePathMapping.get();
                        } else if (Objects.nonNull(currentGeometryPath[0]) && path.size() > currentGeometryPath[0].size() && Objects.equals(path.subList(0, currentGeometryPath[0].size()), currentGeometryPath[0])) {
                            String s = path.get(currentGeometryPath[0].size());
                            s = s.substring(s.lastIndexOf(":") + 1);
                            LOGGER.debug("geometry type {} {} {} {}", featureTypeConfigurationOgcApi.getId(), s, currentMapping[0].getMappings(), GML_GEOMETRY_TYPE.fromString(s));

                            currentBuilder[0] = mappingBuilder.getMappings()
                                                              .get(Joiner.on('/')
                                                                         .join(currentGeometryPath[0]));
                            currentGeometryType[0] = GML_GEOMETRY_TYPE.fromString(s)
                                                                      .toSimpleFeatureGeometry();
                            currentGeometryPath[0] = null;
                            currentMapping[0] = null;
                        }
                    }

                    @Override
                    public void onPropertyText(String text) throws Exception {
                        if (currentBuilder[0] != null) {
                            boolean mustReversePolygon = false;
                            if (currentGeometryType[0] == SimpleFeatureGeometry.POLYGON || currentGeometryType[0] == SimpleFeatureGeometry.MULTI_POLYGON) {
                                PolygonAnalyzer polygonAnalyzer = new PolygonAnalyzer(null, 2);
                                polygonAnalyzer.write(text);
                                LOGGER.debug("COO: {}", text);
                                LOGGER.debug("REVERSE: {}", polygonAnalyzer.isClockwise());
                                mustReversePolygon = polygonAnalyzer.isClockwise();
                            }

                            for (TargetMappingRefiner targetMappingRefiner : mappingRefiners) {
                                SourcePathMapping refined = targetMappingRefiner.refine(currentBuilder[0].build(), currentGeometryType[0], mustReversePolygon);
                                currentBuilder[0].mappings(refined.getMappings());
                            }

                            //TODO: use StylesStore
                            KeyValueStore stylesStore = rootKeyValueStore.getChildStore("styles")
                                                                         .getChildStore(wfs3Service.getData()
                                                                                                   .getId());

                            for (OgcApiStyleGeneratorExtension wfs3StyleGeneratorExtension : wfs3ConformanceClassRegistry.getExtensionsForType(OgcApiStyleGeneratorExtension.class)) {

                                String style = wfs3StyleGeneratorExtension.generateStyle(currentGeometryType[0], i[0]);

                                if (!Strings.isNullOrEmpty(style)) {
                                    String styleId = "gsfs_" + featureTypeConfigurationOgcApi.getId();
                                    WriteTransaction<String> writeTransaction = stylesStore.openWriteTransaction(styleId);
                                    try {
                                        writeTransaction.write(style);
                                        writeTransaction.execute();
                                        writeTransaction.commit();
                                    } catch (IOException e) {
                                        writeTransaction.rollback();
                                    }
                                }
                            }

                            currentBuilder[0] = null;
                            currentGeometryType[0] = null;
                            foundGeometry[0] = true;
                        }
                    }

                    @Override
                    public void onPropertyEnd(List<String> path) throws Exception {

                    }
                })
                             .toCompletableFuture()
                             .join();

                if (!hasData[0]) {
                    LOGGER.debug("NO DATA found for {}", featureTypeConfigurationOgcApi.getId());

                    ImmutableSourcePathMapping.Builder next = mappingBuilder.getMappings()
                                                                            .values()
                                                                            .iterator()
                                                                            .next();

                    //TODO
                    HashMap<String, TargetMapping> map = Maps.newHashMap(next.build()
                                                                             .getMappings());
                    ((OgcApiFeaturesGenericMapping) map.get(TargetMapping.BASE_TYPE)).setEnabled(false);
                    next.mappings(map);

                }

                progress += factor;
                taskContext.setCompleteness(progress);
                i[0]++;

            }

            builder.mappingStatus(new ImmutableMappingStatus.Builder().from(featureProviderData.getMappingStatus())
                                                                      .refined(true)
                                                                      .build());

            ImmutableOgcApiDatasetData updated = new ImmutableOgcApiDatasetData.Builder().from(wfs3Service.getData())
                                                                                         .featureProvider(builder.build())
                                                                                         .build();

            serviceRepository.put(wfs3Service.getId(), updated);

            taskContext.setCompleteness(1);
            taskContext.setStatusMessage("success");
        }
    }

    static class PolygonAnalyzer extends DefaultCoordinatesWriter {

        private String tuple;
        private int cnt;

        private double curX = 0;
        private double prevX = 0;
        private double prevY = 0;
        private double a = 0;

        public PolygonAnalyzer(JsonGenerator json, int srsDimension) {
            super(new JsonCoordinateFormatter(json), srsDimension);
            tuple = "";
            cnt = 0;
        }

        public boolean isClockwise() {
            return a > 0;
        }

        @Override
        protected void writeStart() throws IOException {

        }

        @Override
        protected void writeSeparator() throws IOException {

        }

        @Override
        protected void writeEnd() throws IOException {

        }

        @Override
        protected void writeValue(char[] chars, int start, int end) throws IOException {
            flushChunkBoundaryBuffer();
            for (int k = start; k < start + end; k++) {
                tuple += chars[k];
            }
        }

        @Override
        protected void formatValue(String buf) throws IOException {
            tuple += buf;
        }

        @Override
        public final void close() throws IOException {
            if (!lastWhite) {
                counter++;
            }
            flushChunkBoundaryBuffer();
            isCompleteTuple();
        }

        @Override
        protected boolean isCompleteTuple() {
            boolean ict = super.isCompleteTuple();
            cnt++;
            if (this.isXValue()) {
                curX = Double.parseDouble(tuple);
                tuple = "";
            } else if (this.isYValue()) {
                double curY = Double.parseDouble(tuple);
                tuple = "";

                if (cnt >= 4) {
                    a += (prevX + curX) * (curY - prevY);
                }
                prevX = curX;
                prevY = curY;
            }
            return ict;
        }

    }*/

}

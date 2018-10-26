/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.generator;

import de.ii.ldproxy.wfs3.Gml2Wfs3GenericMappingProvider;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.ModifiableWfs3ServiceData;
import de.ii.ldproxy.wfs3.api.Wfs3ExtensionRegistry;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.xtraplatform.entity.api.EntityDataGenerator;
import de.ii.xtraplatform.entity.api.EntityRepository;
import de.ii.xtraplatform.entity.api.EntityRepositoryForType;
import de.ii.xtraplatform.feature.provider.wfs.ModifiableFeatureProviderDataWfs;
import de.ii.xtraplatform.feature.query.api.FeatureProvider;
import de.ii.xtraplatform.feature.query.api.FeatureProviderMetadataConsumer;
import de.ii.xtraplatform.feature.query.api.FeatureProviderRegistry;
import de.ii.xtraplatform.feature.query.api.MultiFeatureProviderMetadataConsumer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformerService2;
import de.ii.xtraplatform.feature.transformer.api.MappingStatus;
import de.ii.xtraplatform.feature.transformer.api.ModifiableMappingStatus;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.feature.transformer.api.TransformingFeatureProvider;
import de.ii.xtraplatform.scheduler.api.Scheduler;
import de.ii.xtraplatform.scheduler.api.Task;
import de.ii.xtraplatform.scheduler.api.TaskContext;
import de.ii.xtraplatform.scheduler.api.TaskQueue;
import de.ii.xtraplatform.scheduler.api.TaskStatus;
import de.ii.xtraplatform.service.api.Service;
import de.ii.xtraplatform.service.api.ServiceTasks;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.xtraplatform.feature.transformer.api.FeatureTransformerService2)",
        onArrival = "onArrival",
        onDeparture = "onDeparture",
        onModification = "onModification")
public class Wfs3ServiceGenerator implements EntityDataGenerator<Wfs3ServiceData>, ServiceTasks {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3ServiceGenerator.class);

    @Context
    private BundleContext bundleContext;

    @Requires
    private Wfs3ExtensionRegistry wfs3ConformanceClassRegistry;

    @Requires
    private FeatureProviderRegistry featureProviderRegistry;

    @Requires
    private EntityRepository entityRepository;

    private final TaskQueue taskQueue;

    Wfs3ServiceGenerator(@Requires Scheduler scheduler) {
        this.taskQueue = scheduler.createQueue(this.getType()
                                                   .getSimpleName());
    }

    @Override
    public Class<Wfs3ServiceData> getType() {
        return Wfs3ServiceData.class;
    }

    @Override
    public Wfs3ServiceData generate(Wfs3ServiceData partialData) {
        try {
            ModifiableWfs3ServiceData wfs3ServiceData = ModifiableWfs3ServiceData.create()
                                                                                 .from(partialData);
            //TODO: init from defaults? (can be achieved through copies)
            final long now = Instant.now()
                                    .toEpochMilli();
            wfs3ServiceData.setCreatedAt(now);
            wfs3ServiceData.setLastModified(now);
            wfs3ServiceData.setShouldStart(true);
            if (!((ModifiableFeatureProviderDataWfs)wfs3ServiceData.getFeatureProvider()).mappingStatusIsSet()) {
                ((ModifiableFeatureProviderDataWfs) wfs3ServiceData.getFeatureProvider()).setMappingStatus(ModifiableMappingStatus.create()
                                                                                                                                  .setEnabled(true)
                                                                                                                                  .setSupported(false));
            }


            FeatureProvider featureProvider = featureProviderRegistry.createFeatureProvider(wfs3ServiceData.getFeatureProvider());

            if (!(featureProvider instanceof FeatureProvider.MetadataAware && featureProvider instanceof TransformingFeatureProvider.SchemaAware)) {
                throw new IllegalArgumentException("feature provider not metadata aware");
            }

            LOGGER.debug("GENERATING {} {}", Wfs3ServiceData.class, partialData.getId());

            FeatureProvider.MetadataAware metadataAware = (FeatureProvider.MetadataAware) featureProvider;
            FeatureProvider.DataGenerator dataGenerator = (TransformingFeatureProvider.DataGenerator) featureProvider;

            FeatureProviderMetadataConsumer metadataConsumer = new MultiFeatureProviderMetadataConsumer(new Metadata2Wfs3(wfs3ServiceData), dataGenerator.getDataGenerator(wfs3ServiceData.getFeatureProvider()));
            //TODO: getMetadata(partialData.getFeatureProvider(), FeatureProviderMetadataConsumer... additionalConsumers)
            metadataAware.getMetadata(metadataConsumer);

            return wfs3ServiceData;
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }

    private List<TargetMappingProviderFromGml> getMappingProviders() {
        return Stream.concat(
                Stream.of(new Gml2Wfs3GenericMappingProvider()),
                wfs3ConformanceClassRegistry.getOutputFormats()
                                            .values()
                                            .stream()
                                            .map(Wfs3OutputFormatExtension::getMappingGenerator)
                                            .filter(Optional::isPresent)
                                            .map(Optional::get)
        )
                     .collect(Collectors.toList());
    }

    private synchronized void onArrival(ServiceReference<FeatureTransformerService2> ref) {
        checkGenerateMapping(ref);
    }

    private synchronized void onDeparture(ServiceReference<FeatureTransformerService2> ref) {
    }

    private synchronized void onModification(ServiceReference<FeatureTransformerService2> ref) {
        LOGGER.debug("MOD");
        checkGenerateMapping(ref);
    }

    static final String GENERATE_LABEL = "Analyzing feature types";

    private void checkGenerateMapping(ServiceReference<FeatureTransformerService2> ref) {
        final FeatureTransformerService2 featureTransformerService = bundleContext.getService(ref);

        if (featureTransformerService instanceof Wfs3Service) {
            final Wfs3Service wfs3Service = (Wfs3Service) featureTransformerService;

            if (canGenerateMapping(wfs3Service.getFeatureProvider()) && shouldGenerateMapping(wfs3Service.getData()
                                                                                                         .getFeatureProvider()
                                                                                                         .getMappingStatus(), wfs3Service.getId())) {
                final Wfs3ServiceData partialData = ModifiableWfs3ServiceData.create()
                                                                             .from(wfs3Service.getData());

                taskQueue.launch(new Task() {

                    @Override
                    public String getId() {
                        return wfs3Service.getId();
                    }

                    @Override
                    public String getLabel() {
                        return GENERATE_LABEL;
                    }

                    @Override
                    public void run(TaskContext taskContext) {

                        //TODO: to onServiceStart queue
                        //TODO: set mappingStatus.enabled to false for catalog services
                        //TODO: reactivate on-the-fly mapping
                        TransformingFeatureProvider.SchemaAware schemaAware = (TransformingFeatureProvider.SchemaAware) wfs3Service.getFeatureProvider();
                        FeatureProvider.DataGenerator dataGenerator = (TransformingFeatureProvider.DataGenerator) wfs3Service.getFeatureProvider();
                        //TODO: getSchema(partialData.getFeatureProvider(), getMappingProviders(), FeatureProviderSchemaConsumer... additionalConsumers)
                        schemaAware.getSchema(((TransformingFeatureProvider.DataGenerator) dataGenerator).getMappingGenerator(partialData.getFeatureProvider(), getMappingProviders()), partialData.getFeatureProvider()
                                                                                                                                                                                                   .getFeatureTypes(), taskContext);

                        try {
                            new EntityRepositoryForType(entityRepository, Service.ENTITY_TYPE).replaceEntity(partialData);
                        } catch (IOException e) {
                            //ignore
                        }
                    }
                });
            }
        }
    }

    private boolean canGenerateMapping(TransformingFeatureProvider featureProvider) {
        return featureProvider instanceof TransformingFeatureProvider.SchemaAware && featureProvider instanceof FeatureProvider.DataGenerator;
    }

    private boolean shouldGenerateMapping(MappingStatus mappingStatus, String id) {
        return mappingStatus.getLoading()
                && !getCurrentTask().filter(task -> task.getId().equals(id) && task.getLabel().equals(GENERATE_LABEL)).isPresent()
                && taskQueue.getFutureTasks().stream().noneMatch(task -> task.getId().equals(id) && task.getLabel().equals(GENERATE_LABEL));
    }


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
}

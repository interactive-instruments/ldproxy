/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.application;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import de.ii.ldproxy.ogcapi.domain.*;
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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData.DEFAULT_CRS;


@EntityComponent
@Entity(entityType = Service.class, dataType = OgcApiDatasetData.class)
public class OgcApiApiEntity extends AbstractService<OgcApiDatasetData> implements OgcApiDataset {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiApiEntity.class);
    private static final ExecutorService startupTaskExecutor = MoreExecutors.getExitingExecutorService((ThreadPoolExecutor) Executors.newFixedThreadPool(1));

    private final OgcApiExtensionRegistry extensionRegistry;
    //TODO: only needed for OGC API Features, enable APIs that do not implement features, too
    private TransformingFeatureProvider featureProvider;
    //TODO: encapsulate
    private CrsTransformer defaultTransformer;
    private CrsTransformer defaultReverseTransformer;
    private final Map<String, CrsTransformer> additionalTransformers;
    private final Map<String, CrsTransformer> additionalReverseTransformers;

    public OgcApiApiEntity(@Requires OgcApiExtensionRegistry extensionRegistry,
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

    @Override
    protected void onStart() {
        List<OgcApiStartupTask> ogcApiStartupTasks = getStartupTasks();
        Map<Thread, String> threadMap = null;
        for (OgcApiStartupTask startupTask : ogcApiStartupTasks) {
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
                        ogcApiStartupTasks.forEach(ogcApiStartupTask -> ogcApiStartupTask.removeThreadMapEntry(entry.getKey()));
                    }
                }
            }
        }

        ogcApiStartupTasks.forEach(ogcApiStartupTask -> startupTaskExecutor.submit(ogcApiStartupTask.getTask(this, featureProvider)));

    }

    @Override
    public OgcApiDatasetData getData() {
        return super.getData();
    }

    private List<OgcApiStartupTask> getStartupTasks() {
        return extensionRegistry.getExtensionsForType(OgcApiStartupTask.class);
    }

    @Override
    public <T extends FormatExtension> Optional<T> getOutputFormat(Class<T> extensionType, OgcApiMediaType mediaType, String path) {
        return extensionRegistry.getExtensionsForType(extensionType)
                .stream()
                .filter(outputFormatExtension -> path.matches(outputFormatExtension.getPathPattern()))
                .filter(outputFormatExtension -> mediaType.type().isCompatible(outputFormatExtension.getMediaType().type()))
                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(getData()))
                .findFirst();
    }

    @Override
    public <T extends FormatExtension> List<T> getAllOutputFormats(Class<T> extensionType, OgcApiMediaType mediaType, String path, Optional<T> excludeFormat) {
        return extensionRegistry.getExtensionsForType(extensionType)
                .stream()
                .filter(outputFormatExtension -> !Objects.equals(outputFormatExtension, excludeFormat.orElse(null)))
                .filter(outputFormatExtension -> path.matches(outputFormatExtension.getPathPattern()))
                .filter(outputFormatExtension -> mediaType.type().isCompatible(outputFormatExtension.getMediaType().type()))
                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(getData()))
                .collect(Collectors.toList());
    }

    private boolean isConformanceEnabled(ConformanceClass conformanceClass) {

        return conformanceClass.isEnabledForApi(getData());
    }

    @Override
    public Optional<CrsTransformer> getCrsTransformer(EpsgCrs crs) {
        if (featureProvider.supportsCrs(crs)) {
            return Optional.empty();
        }
        if (Objects.isNull(crs) || Objects.equals(crs, DEFAULT_CRS)) {
            return Optional.of(defaultTransformer);
        }

        if (!additionalTransformers.containsKey(crs.getAsUri())) {
            throw new BadRequestException("Invalid CRS");
        }

        return Optional.of(additionalTransformers.get(crs.getAsUri()));
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
        if (Objects.equals(bbox.getEpsgCrs(), DEFAULT_CRS)) {
            return defaultReverseTransformer.transformBoundingBox(bbox);
        }

        return additionalReverseTransformers.get(bbox.getEpsgCrs()
                                                     .getAsUri())
                                            .transformBoundingBox(bbox);
    }

    @Override
    public List<List<Double>> transformCoordinates(List<List<Double>> coordinates,
                                                   EpsgCrs crs) throws CrsTransformationException {
        CrsTransformer transformer = Objects.equals(crs, DEFAULT_CRS) ? this.defaultReverseTransformer : additionalReverseTransformers.get(crs.getAsUri());
        if (Objects.nonNull(transformer)) {
            double[] transformed = transformer.transform(coordinates.stream()
                                                                    .flatMap(Collection::stream)
                                                                    .mapToDouble(Double::doubleValue)
                                                                    .toArray(), coordinates.size(), false);
            List<List<Double>> result = new ArrayList<>();
            for (int i = 0; i < transformed.length; i += 2) {
                result.add(ImmutableList.of(transformed[i], transformed[i + 1]));
            }

            return result;
        }

        return coordinates;
    }
}

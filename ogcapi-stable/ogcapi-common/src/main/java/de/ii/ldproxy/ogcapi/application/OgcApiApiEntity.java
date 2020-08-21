/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.application;

import com.google.common.util.concurrent.MoreExecutors;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiStartupTask;
import de.ii.xtraplatform.entities.domain.EntityComponent;
import de.ii.xtraplatform.entities.domain.handler.Entity;
import de.ii.xtraplatform.service.api.AbstractService;
import de.ii.xtraplatform.service.api.Service;
import de.ii.xtraplatform.service.api.ServiceData;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@EntityComponent
@Entity(
        type = Service.TYPE,
        subType = OgcApiApiDataV2.SERVICE_TYPE,
        dataClass = ServiceData.class,
        dataSubClass = OgcApiApiDataV2.class
)
public class OgcApiApiEntity extends AbstractService<OgcApiApiDataV2> implements OgcApiApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiApiEntity.class);
    private static final ExecutorService startupTaskExecutor = MoreExecutors.getExitingExecutorService((ThreadPoolExecutor) Executors.newFixedThreadPool(1));

    private final OgcApiExtensionRegistry extensionRegistry;

    public OgcApiApiEntity(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    protected void onStart() {
        if (shouldRegister()) {
            //TODO: check all extensions if startup was successful
            LOGGER.info("Service with id '{}' started successfully.", getId());

            //TODO: merge with service background tasks
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

            ogcApiStartupTasks.forEach(ogcApiStartupTask -> startupTaskExecutor.submit(ogcApiStartupTask.getTask(this)));
        }
    }

    @Override
    public OgcApiApiDataV2 getData() {
        return super.getData();
    }

    private List<OgcApiStartupTask> getStartupTasks() {
        return extensionRegistry.getExtensionsForType(OgcApiStartupTask.class);
    }

    @Override
    public <T extends FormatExtension> Optional<T> getOutputFormat(Class<T> extensionType, OgcApiMediaType mediaType,
                                                                   String path, Optional<String> collectionId) {
        return extensionRegistry.getExtensionsForType(extensionType)
                                .stream()
                                .filter(outputFormatExtension -> path.matches(outputFormatExtension.getPathPattern()))
                                .filter(outputFormatExtension -> mediaType.type()
                                                                          .isCompatible(outputFormatExtension.getMediaType()
                                                                                                             .type()))
                                .filter(outputFormatExtension -> collectionId.isPresent() ? outputFormatExtension.isEnabledForApi(getData(),collectionId.get()) :
                                                                                            outputFormatExtension.isEnabledForApi(getData()))
                                .findFirst();
    }

    @Override
    public <T extends FormatExtension> List<T> getAllOutputFormats(Class<T> extensionType, OgcApiMediaType mediaType,
                                                                   String path, Optional<T> excludeFormat) {
        return extensionRegistry.getExtensionsForType(extensionType)
                                .stream()
                                .filter(outputFormatExtension -> !Objects.equals(outputFormatExtension, excludeFormat.orElse(null)))
                                .filter(outputFormatExtension -> path.matches(outputFormatExtension.getPathPattern()))
                                .filter(outputFormatExtension -> mediaType.type()
                                                                          .isCompatible(outputFormatExtension.getMediaType()
                                                                                                             .type()))
                                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(getData()))
                                .collect(Collectors.toList());
    }

}

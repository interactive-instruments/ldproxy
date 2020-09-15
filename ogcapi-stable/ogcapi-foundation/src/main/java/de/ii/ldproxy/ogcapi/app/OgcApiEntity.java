/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.app;

import com.google.common.util.concurrent.MoreExecutors;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.StartupTask;
import de.ii.xtraplatform.services.domain.AbstractService;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.store.domain.entities.EntityComponent;
import de.ii.xtraplatform.store.domain.entities.handler.Entity;
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
        subType = OgcApiDataV2.SERVICE_TYPE,
        dataClass = ServiceData.class,
        dataSubClass = OgcApiDataV2.class
)
public class OgcApiEntity extends AbstractService<OgcApiDataV2> implements OgcApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiEntity.class);
    private static final ExecutorService startupTaskExecutor = MoreExecutors.getExitingExecutorService((ThreadPoolExecutor) Executors.newFixedThreadPool(1));

    private final ExtensionRegistry extensionRegistry;

    public OgcApiEntity(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    protected void onStart() {
        if (shouldRegister()) {
            //TODO: check all extensions if startup was successful
            LOGGER.info("Service with id '{}' started successfully.", getId());

            //TODO: merge with service background tasks
            List<StartupTask> startupTasks = getStartupTasks();
            Map<Thread, String> threadMap = null;
            for (StartupTask startupTask : startupTasks) {
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
                            startupTasks.forEach(startupTask -> startupTask.removeThreadMapEntry(entry.getKey()));
                        }
                    }
                }
            }

            // TODO getTask is already starting the task (and adds it to the thread map)
            //startupTasks.forEach(startupTask -> startupTaskExecutor.submit(startupTask.getTask(this)));
            startupTasks.forEach(startupTask -> startupTask.getTask(this));
        }
    }

    @Override
    public OgcApiDataV2 getData() {
        return super.getData();
    }

    private List<StartupTask> getStartupTasks() {
        return extensionRegistry.getExtensionsForType(StartupTask.class);
    }

    @Override
    public <T extends FormatExtension> Optional<T> getOutputFormat(Class<T> extensionType, ApiMediaType mediaType,
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
    public <T extends FormatExtension> List<T> getAllOutputFormats(Class<T> extensionType, ApiMediaType mediaType,
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

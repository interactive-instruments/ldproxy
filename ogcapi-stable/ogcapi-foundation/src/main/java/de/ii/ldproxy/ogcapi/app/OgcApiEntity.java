/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.app;

import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.services.domain.AbstractService;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.store.domain.entities.EntityComponent;
import de.ii.xtraplatform.store.domain.entities.handler.Entity;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    private final ExtensionRegistry extensionRegistry;

    public OgcApiEntity(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public OgcApiDataV2 getData() {
        return super.getData();
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

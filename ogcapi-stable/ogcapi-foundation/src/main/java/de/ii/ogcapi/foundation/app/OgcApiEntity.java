/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.app;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.ogcapi.foundation.domain.ApiExtension;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.services.domain.AbstractService;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OgcApiEntity extends AbstractService<OgcApiDataV2> implements OgcApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiEntity.class);

    private final ExtensionRegistry extensionRegistry;

    @AssistedInject
    public OgcApiEntity(ExtensionRegistry extensionRegistry, @Assisted OgcApiDataV2 data) {
        super(data);
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public OgcApiDataV2 getData() {
        return super.getData();
    }

    @Override
    protected boolean onStartup() throws InterruptedException {

        // validate the API, the behaviour depends on the validation option for the API:
        // NONE: no validation
        // LAX: invalid the configuration and try to remove invalid options, but try to start the service with the valid options
        // STRICT: no validation during hydration, validation will be done in onStartup() and startup will fail in case of any error
        boolean isSuccess = true;
        OgcApiDataV2 apiData = getData();
        MODE apiValidation = apiData.getApiValidation();

        if (apiValidation!= MODE.NONE)
            LOGGER.info("Validating service '{}'.", apiData.getId());

        for (ApiExtension extension : extensionRegistry.getExtensions()) {
            if (extension.isEnabledForApi(apiData)) {
                ValidationResult result = extension.onStartup(getData(), apiValidation);
                isSuccess = isSuccess && result.isSuccess();
                result.getErrors().forEach(LOGGER::error);
                result.getStrictErrors().forEach(result.getMode() == MODE.STRICT ? LOGGER::error : LOGGER::warn);
                result.getWarnings().forEach(LOGGER::warn);
            }
            checkForStartupCancel();
        }

        if (!isSuccess)
            LOGGER.error("Service with id '{}' could not be started. See previous log messages for reasons.", apiData.getId());

        return isSuccess;
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
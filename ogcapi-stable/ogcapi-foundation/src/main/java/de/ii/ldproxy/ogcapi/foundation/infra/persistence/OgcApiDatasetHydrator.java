/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.foundation.infra.persistence;

import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataHydratorExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.store.domain.entities.EntityHydrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

public class OgcApiDatasetHydrator implements EntityHydrator<OgcApiDataV2> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiDatasetHydrator.class);

    private final ExtensionRegistry extensionRegistry;

    public OgcApiDatasetHydrator(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public OgcApiDataV2 hydrateData(OgcApiDataV2 apiData) {

        OgcApiDataV2 hydrated = apiData;

        if (hydrated.isAuto()) {
            LOGGER.info("Service with id '{}' is in auto mode, generating configuration ...", hydrated.getId());
        }

        List<OgcApiDataHydratorExtension> extensions = extensionRegistry.getExtensionsForType(OgcApiDataHydratorExtension.class);
        extensions.sort(Comparator.comparing(OgcApiDataHydratorExtension::getSortPriority));
        for (OgcApiDataHydratorExtension hydrator : extensions) {
            if (hydrator.isEnabledForApi(hydrated)) {
                hydrated = hydrator.getHydratedData(hydrated);
            }
        }

        return hydrated;
    }

}

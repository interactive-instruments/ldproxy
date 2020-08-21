/**
 * Copyright 2020 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.persistence;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.entities.domain.EntityHydrator;
import de.ii.xtraplatform.entities.domain.handler.Entity;
import de.ii.xtraplatform.service.api.Service;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Provides(properties = {
        @StaticServiceProperty(name = Entity.TYPE_KEY, type = "java.lang.String", value = Service.TYPE),
        @StaticServiceProperty(name = Entity.SUB_TYPE_KEY, type = "java.lang.String", value = OgcApiDataV2.SERVICE_TYPE)
})
@Instantiate
public class OgcApiDatasetHydrator implements EntityHydrator<OgcApiDataV2> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiDatasetHydrator.class);

    private final ExtensionRegistry extensionRegistry;

    public OgcApiDatasetHydrator(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public OgcApiDataV2 hydrateData(OgcApiDataV2 apiData) {

        OgcApiDataV2 hydrated = apiData;

        if (hydrated.isAuto()) {
            LOGGER.info("Service with id '{}' is in auto mode, generating configuration ...", hydrated.getId());
        }

        hydrated = generateBuildingBlocksIfNecessary(hydrated);

        for (OgcApiDataHydratorExtension hydrator : extensionRegistry.getExtensionsForType(OgcApiDataHydratorExtension.class)) {
            if (hydrator.isEnabledForApi(hydrated)) {
                hydrated = hydrator.getHydratedData(hydrated);
            }
        }

        return hydrated;
    }

    private OgcApiDataV2 generateBuildingBlocksIfNecessary(OgcApiDataV2 data) {

        if (data.isAuto() && data.getExtensions()
                                  .isEmpty()) {

            List<ExtensionConfiguration> buildingBlocks = extensionRegistry.getExtensionsForType(ApiBuildingBlock.class)
                                                                           .stream()
                                                                           .sorted(Comparator.comparing(buildingBlock -> buildingBlock.getClass()
                                                                                                                                      .getSimpleName()))
                                                                           .map(ApiBuildingBlock::getDefaultConfiguration)
                                                                           .collect(Collectors.toList());

            return new ImmutableOgcApiDataV2.Builder().from(data)
                                                         .extensions(buildingBlocks)
                                                         .build();
        }

        return data;
    }
}

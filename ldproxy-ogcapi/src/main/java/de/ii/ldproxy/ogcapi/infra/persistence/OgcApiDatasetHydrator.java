/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.persistence;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataHydratorExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.xtraplatform.event.store.EntityHydrator;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component
@Provides(properties = {
        //TODO: how to connect to entity
        @StaticServiceProperty(name = "entityType", type = "java.lang.String", value = "services")
})
@Instantiate
public class OgcApiDatasetHydrator implements EntityHydrator<OgcApiApiDataV2> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiDatasetHydrator.class);

    private final OgcApiExtensionRegistry extensionRegistry;

    public OgcApiDatasetHydrator(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public Map<String, Object> getInstanceConfiguration(OgcApiApiDataV2 apiData) {

        OgcApiApiDataV2 newData = apiData;

        for (OgcApiDataHydratorExtension hydrator : extensionRegistry.getExtensionsForType(OgcApiDataHydratorExtension.class)) {
            if (hydrator.isEnabledForApi(apiData)) {
                newData = hydrator.getHydratedData(newData);
            }
        }
        return ImmutableMap.<String, Object>builder()
                .put("data", newData)
                //.put("featureProvider", featureProvider)
                //.put("defaultTransformer", defaultTransformer)
                //.put("defaultReverseTransformer", defaultReverseTransformer)
                //.put("additionalTransformers", additionalTransformers)
                //.put("additionalReverseTransformers", additionalReverseTransformers)
                .build();
    }
}

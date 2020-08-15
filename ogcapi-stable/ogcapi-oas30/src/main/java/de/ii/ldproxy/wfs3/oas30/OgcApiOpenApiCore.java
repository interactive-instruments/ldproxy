/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.oas30;

import de.ii.ldproxy.ogcapi.domain.*;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.Comparator;

@Component
@Provides
@Instantiate
public class OgcApiOpenApiCore implements OpenApiExtension {

    private final OgcApiExtensionRegistry extensionRegistry;

    public OgcApiOpenApiCore(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public int getSortPriority() {
        return 0;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiCommonConfiguration.class);
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, OgcApiApiDataV2 apiData) {
        if (apiData != null) {

            extensionRegistry.getExtensionsForType(OgcApiEndpointExtension.class)
                    .stream()
                    .filter(endpoint -> endpoint.isEnabledForApi(apiData))
                    .map(endpoint -> endpoint.getDefinition(apiData))
                    .sorted(Comparator.comparing(OgcApiEndpointDefinition::getSortPriority))
                    .forEachOrdered(ogcApiEndpointDefinition -> {
                        ogcApiEndpointDefinition.updateOpenApiDefinition(apiData, openAPI);
                    });

            // TODO apply rename transformers for filter properties and values

        }

        return openAPI;
    }
}

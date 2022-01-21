/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.oas30.app;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.oas30.domain.Oas30Configuration;
import de.ii.ldproxy.ogcapi.oas30.domain.OpenApiExtension;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.Comparator;

@Component
@Provides
@Instantiate
public class OpenApiDefinition implements OpenApiExtension {

    private final ExtensionRegistry extensionRegistry;

    public OpenApiDefinition(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public int getSortPriority() {
        return 0;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return Oas30Configuration.class;
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, OgcApiDataV2 apiData) {
        if (apiData != null) {

            extensionRegistry.getExtensionsForType(EndpointExtension.class)
                    .stream()
                    .filter(endpoint -> endpoint.isEnabledForApi(apiData))
                    .map(endpoint -> endpoint.getDefinition(apiData))
                    .sorted(Comparator.comparing(ApiEndpointDefinition::getSortPriority))
                    .forEachOrdered(ogcApiEndpointDefinition -> {
                        ogcApiEndpointDefinition.updateOpenApiDefinition(apiData, openAPI);
                    });

            // TODO apply rename transformers for filter properties and values

        }

        return openAPI;
    }
}

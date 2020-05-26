/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiEndpointExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.wfs3.oas30.OpenApiExtension;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.Optional;

/**
 * extend API definition
 *
 * TODO this will go away once the OpenAPI module will get everything from the EndpointDefinitions
 */
@Component
@Provides
@Instantiate
public class ObservationProcessingOpenApi implements OpenApiExtension {

    final OgcApiExtensionRegistry extensionRegistry;

    public ObservationProcessingOpenApi(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public int getSortPriority() {
        return 4000;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<ObservationProcessingConfiguration> extension = getExtensionConfiguration(apiData, ObservationProcessingConfiguration.class);

        if (extension.isPresent() &&
            extension.get()
                     .getEnabled()) {
            return true;
        }
        return false;
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, OgcApiApiDataV2 apiData) {

        /* TODO moved to OpenAPI 3.0 module
        extensionRegistry.getExtensionsForType(OgcApiEndpointExtension.class)
                .stream()
                .filter(endpoint -> endpoint.isEnabledForApi(apiData))
                .map(endpoint -> endpoint.getDefinition(apiData))
                .forEach(ogcApiEndpointDefinition -> {
                    ogcApiEndpointDefinition.updateOpenApiDefinition(apiData, openAPI);
                });

         */

        return openAPI;
    }


}

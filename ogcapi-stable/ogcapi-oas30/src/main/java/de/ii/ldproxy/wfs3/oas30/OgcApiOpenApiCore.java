/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.oas30;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.xtraplatform.codelists.CodelistRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.*;
import java.util.stream.Collectors;

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

            /*
                           Optional<OgcApiFeaturesCoreConfiguration> coreConfiguration = ft.getExtension(OgcApiFeaturesCoreConfiguration.class);
                           Map<String, String> filterableFields = coreConfiguration.map(OgcApiFeaturesCoreConfiguration::getOtherFilterParameters)
                                                                                   .orElse(ImmutableMap.of());

                           //TODO: apply rename transformers
                           //Map<String, Optional<CodelistTransformation>> transformations = coreConfiguration.getTransformations().filter...;

                           filterableFields.keySet()
                                           .forEach(field -> {
                                               StringSchema schema = new StringSchema();
                                               //TODO
                                               /*transformations.get(field)
                                                              .map(codelistTransformation -> codelistTransformation.getCodelist())
                                                       .map(codelistName -> codelistRegistry.getCodelist(codelistName))
                                                       .ifPresent(codelist -> {
                                                           if (codelist.isPresent()) {
                                                               ImmutableList<String> values = ImmutableList.copyOf(codelist.get().getData().getEntries().keySet());
                                                               if (!values.isEmpty())
                                                                   schema._enum(values);
                                                           }
                                                       });
                                               clonedPathItem.getGet()
                                                             .addParametersItem(
                                                                     new Parameter()
                                                                             .name(field)
                                                                             .in("query")
                                                                             .description("Filter the collection by property '" + field + "'")
                                                                             .required(false)
                                                                             // TODO
                                                                             .schema(schema)
                                                                             .style(Parameter.StyleEnum.FORM)
                                                                             .explode(false)
                                                             );
                                           });
                            */

        }

        return openAPI;
    }
}

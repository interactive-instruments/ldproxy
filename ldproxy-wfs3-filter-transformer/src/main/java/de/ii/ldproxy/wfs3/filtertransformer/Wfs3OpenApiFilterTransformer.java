/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.filtertransformer;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.wfs3.oas30.Wfs3OpenApiExtension;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Comparator;
import java.util.Objects;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3OpenApiFilterTransformer implements Wfs3OpenApiExtension {
    @Override
    public int getSortPriority() {
        return 300;
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, OgcApiDatasetData datasetData) {

        datasetData.getFeatureTypes()
                   .values()
                   .stream()
                   .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                   .filter(ft -> datasetData.isFeatureTypeEnabled(ft.getId()) && ft.getExtension(FilterTransformersConfiguration.class)
                                                                                   .isPresent())
                   .forEach(ft -> {

                       final FilterTransformersConfiguration filterTransformersConfiguration = ft.getExtension(FilterTransformersConfiguration.class)
                                                                                                 .get();
                       if (!filterTransformersConfiguration.getTransformers()
                                                           .isEmpty()) {


                           PathItem pathItem2 = openAPI.getPaths()
                                                       .get(String.format("/collections/%s/items", ft.getId()));

                           filterTransformersConfiguration.getTransformers()
                                                          .forEach(filterTransformerConfiguration -> {
                                                              RequestGeoJsonBboxConfiguration requestGeoJsonBboxConfiguration = (RequestGeoJsonBboxConfiguration) filterTransformerConfiguration;

                                                              requestGeoJsonBboxConfiguration.getParameters()
                                                                                             .forEach(parameter -> {

                                                                                                 if (Objects.nonNull(pathItem2)) {
                                                                                                     Parameter param = new Parameter().name(parameter)
                                                                                                                                      .in("query")
                                                                                                                                      .required(false)
                                                                                                                                      .style(Parameter.StyleEnum.FORM)
                                                                                                                                      .explode(false)
                                                                                                                                      .description("Filter the collection by " + parameter)
                                                                                                                                      .schema(new StringSchema());


                                                                                                     pathItem2.getGet()
                                                                                                              .addParametersItem(param);
                                                                                                 }

                                                                                             });
                                                          });


                       }

                   });

        return openAPI;
    }
}

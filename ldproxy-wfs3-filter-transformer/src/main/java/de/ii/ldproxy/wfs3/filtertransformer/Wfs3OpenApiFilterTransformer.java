/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.filtertransformer;

import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.oas30.Wfs3OpenApiExtension;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;

import static de.ii.ldproxy.wfs3.filtertransformer.FilterTransformersConfiguration.EXTENSION_KEY;

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
    public OpenAPI process(OpenAPI openAPI, Wfs3ServiceData serviceData) {

        serviceData.getFeatureTypes()
                   .values()
                   .stream()
                   .sorted(Comparator.comparing(FeatureTypeConfigurationWfs3::getId))
                   .filter(ft -> serviceData.isFeatureTypeEnabled(ft.getId()) && ft.getExtensions()
                                                                                   .containsKey(EXTENSION_KEY))
                   .forEach(ft -> {

                       final FilterTransformersConfiguration filterTransformersConfiguration = (FilterTransformersConfiguration) ft.getExtensions()
                                                                                                                       .get(EXTENSION_KEY);
                       if (!filterTransformersConfiguration.getTransformers()
                                                       .isEmpty()) {


                           PathItem pathItem2 = openAPI.getPaths()
                                                       .get(String.format("/collections/%s/items", ft.getId()));

                           filterTransformersConfiguration.getTransformers().forEach(filterTransformerConfiguration -> {
                               RequestGeoJsonBboxConfiguration requestGeoJsonBboxConfiguration = (RequestGeoJsonBboxConfiguration) filterTransformerConfiguration;

                               requestGeoJsonBboxConfiguration.getParameters().forEach(parameter -> {

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

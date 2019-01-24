/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.projections;

import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.oas30.Wfs3OpenApiExtension;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3OpenApiProjections implements Wfs3OpenApiExtension {
    @Override
    public int getSortPriority() {
        return 400;
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, Wfs3ServiceData serviceData) {

        openAPI.getComponents()
               .addParameters("properties", new Parameter()
                       .name("properties")
                       .in("query")
                       .description("The properties that should be included for each feature. The parameter value is a comma-separated list of property names.")
                       .required(false)
                       .schema(new ArraySchema().items(new Schema().type("string")))
                       .style(Parameter.StyleEnum.FORM)
                       .explode(false)
               );

        serviceData.getFeatureTypes()
                   .values()
                   .stream()
                   .sorted(Comparator.comparing(FeatureTypeConfigurationWfs3::getId))
                   .filter(ft -> serviceData.isFeatureTypeEnabled(ft.getId()))
                   .forEach(ft -> {

                       PathItem pathItem = openAPI.getPaths()
                                                  .get(String.format("/collections/%s/items", ft.getId()));

                       if (Objects.nonNull(pathItem)) {
                           pathItem.getGet()
                                   .addParametersItem(new Parameter().$ref("#/components/parameters/properties"));
                       }

                       PathItem pathItem2 = openAPI.getPaths()
                                                  .get(String.format("/collections/%s/items/{featureId}", ft.getId()));

                       if (Objects.nonNull(pathItem2)) {
                           pathItem2.getGet()
                                   .addParametersItem(new Parameter().$ref("#/components/parameters/properties"));
                       }


                   });

        return openAPI;
    }
}

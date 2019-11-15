/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.filter;

import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.wfs3.oas30.OpenApiExtension;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import io.swagger.v3.oas.models.OpenAPI;

import java.util.Comparator;
import java.util.Objects;

@Component
@Provides
@Instantiate
public class OpenApiFilter implements OpenApiExtension {

    @Override
    public int getSortPriority() {
        return 500;
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, OgcApiDatasetData apiData) {
        if (isEnabledForApi(apiData)) {
            Parameter filter = new Parameter()
                    .name("filter")
                    .in("query")
                    .description("Filter the collection using CQL query specifiend in the parameter value.")
                    .required(false)
                    .schema(new ArraySchema().items(new Schema().type("String")))
                    .style(Parameter.StyleEnum.FORM)
                    .explode(false);
            openAPI.getComponents().addParameters("filter", filter);

            apiData.getFeatureTypes()
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(FeatureTypeConfiguration::getId))
                    .filter(ft -> apiData.isFeatureTypeEnabled(ft.getId()))
                    .forEach(ft -> {
                        PathItem pathItem = openAPI.getPaths().get(String.format("/collections/%s/items", ft.getId()));
                        if (Objects.nonNull(pathItem)) {
                            pathItem.getGet().addParametersItem(new Parameter().$ref("#/components/parameters/filter"));
                        }
                    });
        }

        return openAPI;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, FilterConfiguration.class);
    }
}

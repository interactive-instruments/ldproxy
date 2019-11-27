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
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
                    .description("Filter the features in the collection using the query expression in the parameter value.")
                    .required(false)
                    .schema(new StringSchema())
                    .style(Parameter.StyleEnum.FORM)
                    .explode(false);
            openAPI.getComponents().addParameters("filter", filter);
            List<String> fEnum = new ArrayList<>();
            fEnum.add("cql-text");
            Parameter filterLang = new Parameter()
                    .name("filter-lang")
                    .in("query")
                    .description("Language of the query expression in the 'filter' parameter.")
                    .required(false)
                    .schema(new StringSchema()._enum(fEnum)._default("cql-text"))
                    .style(Parameter.StyleEnum.FORM)
                    .explode(false);
            openAPI.getComponents().addParameters("filter-lang", filterLang);

            apiData.getFeatureTypes()
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(FeatureTypeConfiguration::getId))
                    .filter(ft -> apiData.isFeatureTypeEnabled(ft.getId()))
                    .forEach(ft -> {
                        PathItem pathItem = openAPI.getPaths().get(String.format("/collections/%s/items", ft.getId()));
                        if (Objects.nonNull(pathItem)) {
                            pathItem.getGet()
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/filter"))
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/filter-lang"));
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

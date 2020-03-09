/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collection.queryables;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.wfs3.oas30.OpenApiExtension;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;


@Component
@Provides
@Instantiate
public class OgcApiQueryablesOpenApi implements OpenApiExtension {
    @Override
    public int getSortPriority() {
        return 800;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, QueryablesConfiguration.class);
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, OgcApiApiDataV2 datasetData) {
        if (isEnabledForApi(datasetData)) {
            /* TODO add queryables to OpenAPI definition

            ImmutableSet<String> crsSet = ImmutableSet.<String>builder()
                    .add(datasetData.getFeatureProvider()
                                    .getNativeCrs()
                                    .getAsUri())
                    .add(DEFAULT_CRS_URI)
                    .addAll(datasetData.getAdditionalCrs()
                                       .stream()
                                       .map(EpsgCrs::getAsUri)
                                       .collect(Collectors.toList()))
                    .build();

            openAPI.getComponents()
                   .addParameters(CRS, new Parameter()
                           .name(CRS)
                           .in("query")
                           .description("The coordinate reference system of the response geometries. Default is WGS84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84).")
                           .required(false)
                           .style(Parameter.StyleEnum.FORM)
                           .schema(new StringSchema()
                                   ._enum(crsSet.asList())
                                   ._default(DEFAULT_CRS_URI))
                           .explode(false));

            openAPI.getComponents()
                   .addParameters(BBOX_CRS, new Parameter()
                           .name(BBOX_CRS)
                           .in("query")
                           .description("The coordinate reference system of the bbox parameter. Default is WGS84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84).")
                           .required(false)
                           .schema(new StringSchema()
                                   ._enum(crsSet.asList())
                                   ._default(DEFAULT_CRS_URI))
                           .style(Parameter.StyleEnum.FORM)
                           .explode(false)
                   );

            datasetData.getFeatureTypes()
                       .values()
                       .stream()
                       .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                       .filter(ft -> datasetData.isFeatureTypeEnabled(ft.getId()))
                       .forEach(ft -> {

                           PathItem pathItem = openAPI.getPaths()
                                                      .get(String.format("/collections/%s/items", ft.getId()));

                           if (Objects.nonNull(pathItem)) {
                               pathItem.getGet()
                                       .addParametersItem(new Parameter().$ref("#/components/parameters/crs"))
                                       .addParametersItem(new Parameter().$ref("#/components/parameters/bbox-crs"));
                           }

                           PathItem pathItem2 = openAPI.getPaths()
                                                       .get(String.format("/collections/%s/items/{featureId}", ft.getId()));

                           if (Objects.nonNull(pathItem2)) {
                               pathItem2.getGet()
                                        .addParametersItem(new Parameter().$ref("#/components/parameters/crs"));
                           }


                       });

             */
        }
        return openAPI;
    }
}

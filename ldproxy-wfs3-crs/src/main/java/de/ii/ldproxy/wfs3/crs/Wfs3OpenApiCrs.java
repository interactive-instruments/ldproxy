/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.crs;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.oas30.Wfs3OpenApiExtension;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS_URI;
import static de.ii.ldproxy.wfs3.crs.Wfs3ParameterCrs.BBOX_CRS;
import static de.ii.ldproxy.wfs3.crs.Wfs3ParameterCrs.CRS;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3OpenApiCrs implements Wfs3OpenApiExtension {
    @Override
    public int getSortPriority() {
        return 500;
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, Wfs3ServiceData serviceData) {

        ImmutableSet<String> crsSet = ImmutableSet.<String>builder()
                .add(serviceData.getFeatureProvider()
                                .getNativeCrs()
                                .getAsUri())
                .add(DEFAULT_CRS_URI)
                .addAll(serviceData.getAdditionalCrs()
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

        return openAPI;
    }
}

/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.aroundrelations;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.wfs3.oas30.OpenApiExtension;
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

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3OpenApiAroundRelations implements OpenApiExtension {
    @Override
    public int getSortPriority() {
        return 200;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, AroundRelationsConfiguration.class);
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, OgcApiDatasetData datasetData) {

        ObjectSchema collectionInfo = (ObjectSchema) openAPI.getComponents()
                                                            .getSchemas()
                                                            .get("collectionInfo");

        collectionInfo.getProperties()
                      .put("relations", new ObjectSchema().description("Related collections that may be retrieved for this collection")
                                                          .example("{\"id\": \"label\"}"));


        Parameter limitList = new Parameter().name("limit")
                                             .in("query")
                                             .required(false)
                                             .style(Parameter.StyleEnum.FORM)
                                             .explode(false)
                                             .description("Comma-separated list of limits for related collections")
                                             .schema(new ArraySchema().items(new IntegerSchema()._default(5)
                                                                                                .minimum(BigDecimal.valueOf(0))
                                                                                                .maximum(BigDecimal.valueOf(10000))));

        Parameter offsetList = new Parameter().name("offset")
                                              .in("query")
                                              .required(false)
                                              .style(Parameter.StyleEnum.FORM)
                                              .explode(false)
                                              .description("Comma-separated list of offsets for related collections")
                                              .schema(new ArraySchema().items(new IntegerSchema()._default(0)
                                                                                                 .minimum(BigDecimal.valueOf(0))));

        Parameter relations = new Parameter().name("relations")
                                             .in("query")
                                             .required(false)
                                             .style(Parameter.StyleEnum.FORM)
                                             .explode(false)
                                             .description("Comma-separated list of related collections that should be shown for this feature")
                                             .schema(new ArraySchema().items(new StringSchema()));

        Parameter resolve = new Parameter().name("resolve")
                                           .in("query")
                                           .required(false)
                                           .style(Parameter.StyleEnum.FORM)
                                           .explode(false)
                                           .description("Only provide links to related collections by default, resolve the links when true")
                                           .schema(new BooleanSchema()._default(false));

        openAPI.getComponents()
               .addParameters(relations.getName(), relations)
               .addParameters(resolve.getName(), resolve)
               .addParameters("limitList", limitList)
               .addParameters("offsetList", offsetList);

        datasetData.getFeatureTypes()
                   .values()
                   .stream()
                   .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                   .filter(ft -> datasetData.isFeatureTypeEnabled(ft.getId()) && ft.getExtension(AroundRelationsConfiguration.class)
                                                                                   .isPresent())
                   .forEach(ft -> {

                       final AroundRelationsConfiguration aroundRelationConfiguration = ft.getExtension(AroundRelationsConfiguration.class)
                                                                                          .get();
                       if (!aroundRelationConfiguration.getRelations()
                                                       .isEmpty()) {


                           PathItem pathItem2 = openAPI.getPaths()
                                                       .get(String.format("/collections/%s/items/{featureId}", ft.getId()));

                           if (Objects.nonNull(pathItem2)) {

                               ImmutableSet<String> relationSet = aroundRelationConfiguration.getRelations()
                                                                                             .stream()
                                                                                             .map(relation -> relation.getId())
                                                                                             .collect(ImmutableSet.toImmutableSet());

                               Parameter relationsForFeatureType = new Parameter().name("relations")
                                                                                  .in("query")
                                                                                  .required(false)
                                                                                  .style(Parameter.StyleEnum.FORM)
                                                                                  .explode(false)
                                                                                  .description("Comma-separated list of related collections that should be shown for this feature")
                                                                                  .schema(new ArraySchema().items(new StringSchema()._enum(relationSet.asList())));

                               pathItem2.getGet()
                                        //.addParametersItem(new Parameter().$ref(relations.getName()))
                                        .addParametersItem(relationsForFeatureType)
                                        .addParametersItem(new Parameter().$ref(resolve.getName()))
                                        .addParametersItem(new Parameter().$ref("limitList"))
                                        .addParametersItem(new Parameter().$ref("offsetList"));
                           }
                       }

                   });

        return openAPI;
    }
}

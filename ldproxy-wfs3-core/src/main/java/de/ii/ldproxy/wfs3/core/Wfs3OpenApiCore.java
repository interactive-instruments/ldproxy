/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.oas30.Wfs3OpenApiExtension;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS_URI;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3OpenApiCore implements Wfs3OpenApiExtension {
    @Override
    public int getSortPriority() {
        return 0;
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, Wfs3ServiceData serviceData) {
        if (serviceData != null) {

            PathItem featuresPathItem = openAPI.getPaths()
                                               .remove("/collections/{featureType}");
            PathItem featuresPathItems = openAPI.getPaths()
                                                .remove("/collections/{featureType}/items");
            PathItem featurePathItem = openAPI.getPaths()
                                              .remove("/collections/{featureType}/items/{featureId}");

            serviceData.getFeatureTypes()
                       .values()
                       .stream()
                       .sorted(Comparator.comparing(FeatureTypeConfigurationWfs3::getId))
                       .filter(ft -> serviceData.isFeatureTypeEnabled(ft.getId()))
                       .forEach(ft -> {

                           PathItem clonedPathItem1 = clonePathItem(featuresPathItem);
                           clonedPathItem1
                                   .get(clonedPathItem1.getGet()
                                                       .summary("describe the " + ft.getLabel() + " feature collection")
                                                       .description(null)
                                                       .operationId("describeCollection" + ft.getId())
                                   );
                           openAPI.getPaths()
                                  .addPathItem("/collections/" + ft.getId(), clonedPathItem1);


                           PathItem clonedPathItem = clonePathItem(featuresPathItems);
                           clonedPathItem
                                   .get(clonedPathItem.getGet()
                                                      .summary("retrieve features of " + ft.getLabel() + " feature collection")
                                                      .description(null)
                                                      .operationId("getFeatures" + ft.getId())
                                   );

                           //TODO: to crs module
                           ImmutableSet<String> crs = ImmutableSet.<String>builder()
                                   .add(serviceData.getFeatureProvider()
                                                   .getNativeCrs()
                                                   .getAsUri())
                                   .add(DEFAULT_CRS_URI)
                                   .addAll(serviceData.getAdditionalCrs()
                                                      .stream()
                                                      .map(EpsgCrs::getAsUri)
                                                      .collect(Collectors.toList()))
                                   .build();

                           openAPI.getComponents().addParameters("crs",new Parameter()
                                   .name("crs")
                                   .in("query")
                                   .description("The coordinate reference system of the response geometries. Default is WGS84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84).")
                                   .required(false)
                                   .style(Parameter.StyleEnum.FORM)
                                   .schema(new StringSchema()
                                           ._enum(crs.asList())
                                           ._default(DEFAULT_CRS_URI))
                                   .explode(false));

                           clonedPathItem.getGet().addParametersItem(new Parameter().$ref("#/components/parameters/crs"));

                           openAPI.getComponents().addParameters("bbox-crs",new Parameter()
                                   .name("bbox-crs")
                                   .in("query")
                                   .description("The coordinate reference system of the bbox parameter. Default is WGS84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84).")
                                   .required(false)
                                   .schema(new StringSchema()
                                           ._enum(crs.asList())
                                           ._default(DEFAULT_CRS_URI))
                                   .style(Parameter.StyleEnum.FORM)
                                   .explode(false)
                           );

                           clonedPathItem.getGet().addParametersItem(new Parameter().$ref("#/components/parameters/bbox-crs"));


                           openAPI.getComponents().addParameters("maxAllowableOffset",new Parameter()
                                                   .name("maxAllowableOffset")
                                                   .in("query")
                                                   .description("This option can be used to specify the maxAllowableOffset to be used for generalizing the response geometries.\n \nThe maxAllowableOffset is in the units of the response coordinate reference system.")
                                                   .required(false)
                                                   .schema(new NumberSchema()._default(BigDecimal.valueOf(0)))
                                                   .style(Parameter.StyleEnum.FORM)
                                                   .explode(false)
                                                   .example(0.05)
                                   );
                           clonedPathItem.getGet().addParametersItem(new Parameter().$ref("#/components/parameters/maxAllowableOffset"));

                           /*
                           clonedPathItem.getGet()
                                         .addParametersItem(
                                                 new Parameter()
                                                         .name("crs")
                                                         .in("query")
                                                         .description("The coordinate reference system of the response geometries. Default is WGS84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84).")
                                                         .required(false)
                                                         // TODO
                                                         .schema(new StringSchema()._enum(crs)
                                                                                   ._default(DEFAULT_CRS_URI))
                                                         .style(Parameter.StyleEnum.FORM)
                                                         .explode(false)
                                         );
                           clonedPathItem.getGet()
                                         .addParametersItem(
                                                 new Parameter()
                                                         .name("bbox-crs")
                                                         .in("query")
                                                         .description("The coordinate reference system of the bbox parameter. Default is WGS84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84).")
                                                         .required(false)
                                                         // TODO
                                                         .schema(new StringSchema()._enum(crs)
                                                                                   ._default(DEFAULT_CRS_URI))
                                                         .style(Parameter.StyleEnum.FORM)
                                                         .explode(false)
                                         );
                           clonedPathItem.getGet()
                                         .addParametersItem(
                                                 new Parameter()
                                                         .name("maxAllowableOffset")
                                                         .in("query")
                                                         .description("This option can be used to specify the maxAllowableOffset to be used for generalizing the response geometries.\n \nThe maxAllowableOffset is in the units of the response coordinate reference system.")
                                                         .required(false)
                                                         // TODO
                                                         .schema(new NumberSchema()._default(BigDecimal.valueOf(0)))
                                                         .style(Parameter.StyleEnum.FORM)
                                                         .explode(false)
                                                         .example(0.05)
                                         );
                            */
                           Map<String, String> filterableFields = serviceData.getFilterableFieldsForFeatureType(ft.getId(), true);
                           filterableFields.keySet()
                                           .forEach(field -> {
                                               clonedPathItem.getGet()
                                                             .addParametersItem(
                                                                     new Parameter()
                                                                             .name(field)
                                                                             .in("query")
                                                                             .description("Filter the collection by " + field)
                                                                             .required(false)
                                                                             // TODO
                                                                             .schema(new StringSchema())
                                                                             .style(Parameter.StyleEnum.FORM)
                                                                             .explode(false)
                                                             );
                                           });

                           openAPI.getPaths()
                                  .addPathItem("/collections/" + ft.getId() + "/items", clonedPathItem);


                           PathItem clonedPathItem2 = clonePathItem(featurePathItem);
                           clonedPathItem2 = clonedPathItem2
                                   .get(clonedPathItem2.getGet()
                                                       .summary("retrieve a " + ft.getLabel())
                                                       //.description("")
                                                       .operationId("getFeature" + ft.getId())
                                   );

                           //TODO: to crs module
                           /*clonedPathItem2.getGet()
                                          .addParametersItem(
                                                  new Parameter()
                                                          .name("crs")
                                                          .in("query")
                                                          .description("The coordinate reference system of the response geometries. Default is WGS84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84).")
                                                          .required(false)
                                                          // TODO
                                                          .schema(new StringSchema()._enum(crs)
                                                                                    ._default(DEFAULT_CRS_URI))
                                                          .style(Parameter.StyleEnum.FORM)
                                                          .explode(false)
                                          );
                           clonedPathItem2.getGet()
                                          .addParametersItem(
                                                  new Parameter()
                                                          .name("maxAllowableOffset")
                                                          .in("query")
                                                          .description("This option can be used to specify the maxAllowableOffset to be used for generalizing the response geometries.\n \nThe maxAllowableOffset is in the units of the response coordinate reference system.")
                                                          .required(false)
                                                          // TODO
                                                          .schema(new NumberSchema()._default(BigDecimal.valueOf(0)))
                                                          .style(Parameter.StyleEnum.FORM)
                                                          .explode(false)
                                                          .example(0.05)
                                          );*/
                           clonedPathItem2.getGet().addParametersItem(new Parameter().$ref("#/components/parameters/crs"));
                           clonedPathItem2.getGet().addParametersItem(new Parameter().$ref("#/components/parameters/maxAllowableOffset"));


                           openAPI.getPaths()
                                  .addPathItem("/collections/" + ft.getId() + "/items/{featureId}", clonedPathItem2);

                       });
        }

        return openAPI;
    }

    private PathItem clonePathItem(PathItem pathItem) {
        PathItem clonedPathItem = new PathItem();

        clonedPathItem.set$ref(pathItem.get$ref());
        clonedPathItem.setDescription(pathItem.getDescription());
        clonedPathItem.setSummary(pathItem.getSummary());
        clonedPathItem.setExtensions(pathItem.getExtensions());
        clonedPathItem.setParameters(pathItem.getParameters());
        clonedPathItem.setServers(pathItem.getServers());

        Map<PathItem.HttpMethod, Operation> ops = pathItem.readOperationsMap();


        for (PathItem.HttpMethod key : ops.keySet()) {
            Operation op = ops.get(key);
            final Set<String> tags;
            //op = filterOperation(filter, op, resourcePath, key.toString(), params, cookies, headers);
            clonedPathItem.operation(key, cloneOperation(op));
            /*if (op != null) {
                tags = allowedTags;
            } else {
                tags = filteredTags;
            }
            if (op != null && op.getTags() != null) {
                tags.addAll(op.getTags());
            }*/
        }
        /*if (!clonedPathItem.readOperations().isEmpty()) {
            clonedPaths.addPathItem(resourcePath, clonedPathItem);
        }*/

        return clonedPathItem;
    }

    private Operation cloneOperation(Operation operation) {
        Operation clonedOperation = new Operation();

        clonedOperation.setCallbacks(operation.getCallbacks());
        clonedOperation.setDeprecated(operation.getDeprecated());
        clonedOperation.setDescription(operation.getDescription());
        clonedOperation.setExtensions(operation.getExtensions());
        clonedOperation.setExternalDocs(operation.getExternalDocs());
        clonedOperation.setOperationId(operation.getOperationId());
        if (operation.getParameters() != null)
            clonedOperation.setParameters(Lists.newArrayList(operation.getParameters()));
        clonedOperation.setRequestBody(operation.getRequestBody());
        clonedOperation.setResponses(operation.getResponses());
        clonedOperation.setSecurity(operation.getSecurity());
        clonedOperation.setServers(operation.getServers());
        clonedOperation.setSummary(operation.getSummary());
        clonedOperation.setTags(operation.getTags());

        return clonedOperation;
    }
}

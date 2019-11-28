/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.oas30;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.ii.ldproxy.codelists.CodelistRegistry;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiFeaturesGenericMapping;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Component
@Provides
@Instantiate
public class OgcApiOpenApiCore implements OpenApiExtension {

    // TODO update

    @Requires
    private CodelistRegistry codelistRegistry;

    @Override
    public int getSortPriority() {
        return 0;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, OgcApiFeaturesCoreConfiguration.class);
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, OgcApiDatasetData datasetData) {
        if (datasetData != null) {

            // TODO dynamically add limit - min/max/default, feature formats, other formats, parameters, collectionId values


            PathItem collectionPathItem = openAPI.getPaths()
                                               .remove("/collections/{collectionId}");
            PathItem featuresPathItem = openAPI.getPaths()
                                                .remove("/collections/{collectionId}/items");
            PathItem featurePathItem = openAPI.getPaths()
                                              .remove("/collections/{collectionId}/items/{featureId}");

            datasetData.getFeatureTypes()
                       .values()
                       .stream()
                       .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                       .filter(ft -> datasetData.isFeatureTypeEnabled(ft.getId()))
                       .forEach(ft -> {

                           PathItem clonedPathItem1 = clonePathItem(collectionPathItem, true);
                           clonedPathItem1
                                   .get(clonedPathItem1.getGet()
                                                       .summary("describe the " + ft.getLabel() + " feature collection")
                                                       .description(null)
                                                       .operationId("describeCollection" + ft.getId())
                                   );

                           openAPI.getPaths()
                                  .addPathItem("/collections/" + ft.getId(), clonedPathItem1);

                           PathItem clonedPathItem = clonePathItem(featuresPathItem, true);
                           clonedPathItem
                                   .get(clonedPathItem.getGet()
                                                      .summary("retrieve features of " + ft.getLabel() + " feature collection")
                                                      .description(null)
                                                      .operationId("getFeatures" + ft.getId())
                                   );

                           Map<String, String> filterableFields = datasetData.getFilterableFieldsForFeatureType(ft.getId(), true);

                           FeatureTypeMapping featureTypeMapping = datasetData.getFeatureProvider().getMappings().get(ft.getId());


                           filterableFields.keySet()
                                           .forEach(field -> {
                                               StringSchema schema = new StringSchema();
                                               featureTypeMapping.getMappings()
                                                       .values()
                                                       .stream()
                                                       .map(value -> value.getMappingForType("general"))
                                                       .filter(mapping -> mapping.getName()!=null && mapping.getName().equals(field))
                                                       .filter(mapping -> mapping instanceof OgcApiFeaturesGenericMapping &&
                                                               ((OgcApiFeaturesGenericMapping) mapping).isFilterable() &&
                                                               ((OgcApiFeaturesGenericMapping) mapping).isValue())
                                                       .findFirst()
                                                       .map(mapping -> ((OgcApiFeaturesGenericMapping) mapping).getCodelist())
                                                       .filter(codelistName -> codelistName!=null)
                                                       .map(codelistName -> codelistRegistry.getCodelist(codelistName))
                                                       .ifPresent(codelist -> {
                                                               schema._enum(ImmutableList.copyOf(codelist.get().getData().getEntries().keySet()));
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

                           openAPI.getPaths()
                                  .addPathItem("/collections/" + ft.getId() + "/items", clonedPathItem);

                           PathItem clonedPathItem2 = clonePathItem(featurePathItem, true);
                           clonedPathItem2 = clonedPathItem2
                                   .get(clonedPathItem2.getGet()
                                                       .summary("retrieve a " + ft.getLabel())
                                                       //.description("")
                                                       .operationId("getFeature" + ft.getId())
                                   );

                           openAPI.getPaths()
                                  .addPathItem("/collections/" + ft.getId() + "/items/{featureId}", clonedPathItem2);

                       });
        }

        return openAPI;
    }

    private PathItem clonePathItem(PathItem pathItem, boolean withoutCollectionId) {
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
            clonedPathItem.operation(key, cloneOperation(op, withoutCollectionId));
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

    private Operation cloneOperation(Operation operation, boolean withoutCollectionId) {
        Operation clonedOperation = new Operation();

        clonedOperation.setCallbacks(operation.getCallbacks());
        clonedOperation.setDeprecated(operation.getDeprecated());
        clonedOperation.setDescription(operation.getDescription());
        clonedOperation.setExtensions(operation.getExtensions());
        clonedOperation.setExternalDocs(operation.getExternalDocs());
        clonedOperation.setOperationId(operation.getOperationId());
        if (operation.getParameters() != null)
            clonedOperation.setParameters(Lists.newArrayList(operation.getParameters()
                    .stream()
                    .filter(param -> !withoutCollectionId || param.get$ref()==null || !param.get$ref().equalsIgnoreCase("#/components/parameters/collectionId"))
                    .collect(Collectors.toList())));
        clonedOperation.setRequestBody(operation.getRequestBody());
        clonedOperation.setResponses(operation.getResponses());
        clonedOperation.setSecurity(operation.getSecurity());
        clonedOperation.setServers(operation.getServers());
        clonedOperation.setSummary(operation.getSummary());
        clonedOperation.setTags(operation.getTags());

        return clonedOperation;
    }
}

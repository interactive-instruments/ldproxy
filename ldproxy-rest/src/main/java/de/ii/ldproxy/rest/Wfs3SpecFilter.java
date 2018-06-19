/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import de.ii.ldproxy.service.LdProxyService;
import de.ii.ldproxy.wfs3.URICustomizer;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import de.ii.xtraplatform.ogc.api.wfs.client.GetCapabilities;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSOperation;
import de.ii.xtraplatform.ogc.api.wfs.parser.MultiWfsCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesParser;
import io.swagger.core.filter.AbstractSpecFilter;
import io.swagger.model.ApiDescription;
import io.swagger.oas.models.Components;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.Paths;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.IntegerSchema;
import io.swagger.oas.models.media.ObjectSchema;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.media.StringSchema;
import io.swagger.oas.models.media.XML;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.parameters.QueryParameter;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.servers.Server;
import io.swagger.oas.models.tags.Tag;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class Wfs3SpecFilter extends AbstractSpecFilter {

    private static Logger LOGGER = LoggerFactory.getLogger(OpenApiResource.class);

    private ObjectMapper objectMapper;
    private LdProxyService service;
    private String externalUrl;

    public Wfs3SpecFilter() {
        super();
    }

    public Wfs3SpecFilter(ObjectMapper objectMapper, String externalUrl) {
        super();
        this.objectMapper = objectMapper;
        this.externalUrl = externalUrl;
    }

    public void setService(LdProxyService service) {
        this.service = service;
    }

    public Response getOpenApi(String type, URICustomizer requestUriCustomizer) {

        boolean pretty = true;

        try {
            //TODO
            String resourceName = service.isSecured() ? "/wfs3-api-sec-todo.json" : "/wfs3-api.json";
            OpenAPI openAPI = objectMapper.readerFor(OpenAPI.class)
                                          .with(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                                          .readValue(Resources.asByteSource(Resources.getResource(Wfs3SpecFilter.class, resourceName))
                                                              .openBufferedStream());

            openAPI.getInfo()
                   .version("1.0.0");

            // working around swagger bugs
            openAPI.getComponents()
                   .getParameters()
                   .get("bbox")
                   .schema(new ArraySchema().minItems(4)
                                            .maxItems(6)
                   );
            openAPI.getComponents()
                   .getSchemas()
                   .get("root")
                   .getProperties()
                   .replace("links", new ArraySchema().items(new Schema().$ref("#/components/schemas/link")));
            openAPI.getComponents()
                   .getSchemas()
                   .get("req-classes")
                   .getProperties()
                   .replace("conformsTo", new ArraySchema().items(new StringSchema().example("http://www.opengis.net/spec/wfs-1/3.0/req/core")));
            openAPI.getComponents()
                   .getSchemas()
                   .get("content")
                   .getProperties()
                   .replace("links", new ArraySchema().items(new Schema().$ref("#/components/schemas/link")));
            openAPI.getComponents()
                   .getSchemas()
                   .get("content")
                   .getProperties()
                   .replace("collections", new ArraySchema().items(new Schema().$ref("#/components/schemas/collectionInfo")));
            openAPI.getComponents()
                   .getSchemas()
                   .get("collectionInfo")
                   .getProperties()
                   .replace("links", new ArraySchema().items(new Schema().$ref("#/components/schemas/link")));
            openAPI.getComponents()
                   .getSchemas()
                   .get("collectionInfo")
                   .getProperties()
                   .replace("crs", new ArraySchema().items(new StringSchema()));
            openAPI.getComponents()
                   .getSchemas()
                   .get("extent")
                   .getProperties()
                   .replace("spatial", new ArraySchema().minItems(4)
                                                        .maxItems(6)
                                                        .example(ImmutableList.of(-180, -90, 180, 90))
                                                        .description("West, north, east, south edges of the spatial extent. The minimum and maximum values apply to the coordinate reference system WGS84 longitude/latitude that is supported in the Core. If, for example, a projected coordinate reference system is used, the minimum and maximum values need to be adjusted."));
            openAPI.getComponents()
                   .getSchemas()
                   .get("featureCollectionGeoJSON")
                   .getProperties()
                   .replace("features", new ArraySchema().items(new Schema().$ref("#/components/schemas/featureGeoJSON")));
            openAPI.getComponents()
                   .getSchemas()
                   .get("featureCollectionGeoJSON")
                   .getProperties()
                   .replace("links", new ArraySchema().items(new Schema().$ref("#/components/schemas/link")));
            /*openAPI.getComponents()
                   .getSchemas()
                   .get("featureCollectionGML")
                   .getProperties()
                   .replace("features", new ArraySchema().items(new ComposedSchema().oneOf(ImmutableList.of(new Schema().$ref("#/components/schemas/referenceXlink"), new Schema().$ref("#/components/schemas/featureGML")))
                                                                                    .xml(new XML().name("featureMember")
                                                                                                  .namespace("http://www.opengis.net/wfs/3.0")
                                                                                                  .prefix("wfs"))));*/


            openAPI.servers(ImmutableList.of(new Server().url(requestUriCustomizer.copy().clearParameters().removeLastPathSegment("api").toString())));

            if (service != null) {
                WFSOperation operation = new GetCapabilities();

                WFSCapabilitiesAnalyzer analyzer = new MultiWfsCapabilitiesAnalyzer(
                        new GetCapabilities2OpenApi(openAPI)
                );

                WFSCapabilitiesParser wfsParser = new WFSCapabilitiesParser(analyzer, service.staxFactory);

                wfsParser.parse(service.getWfsAdapter()
                                       .request(operation));

                openAPI.getInfo()
                       .title(service.getName())
                       .description(service.getDescription());


                PathItem featuresPathItem = openAPI.getPaths()
                                                   .remove("/collections/{featureType}");
                PathItem featuresPathItems = openAPI.getPaths()
                                                   .remove("/collections/{featureType}/items");
                PathItem featurePathItem = openAPI.getPaths()
                                                  .remove("/collections/{featureType}/items/{featureId}");

                service.getFeatureTypes()
                       .values()
                       .stream()
                       .sorted(Comparator.comparing(FeatureTypeConfiguration::getName))
                       .forEach(ft -> {
                           if (ft.getMappings()
                                 .findMappings(ft.getNamespace() + ":" + ft.getName(), TargetMapping.BASE_TYPE)
                                 .get(0)
                                 .isEnabled()) {

                               PathItem clonedPathItem1 = clonePathItem(featuresPathItem);
                               clonedPathItem1
                                       .get(clonedPathItem1.getGet()
                                                          .summary("describe the " + ft.getDisplayName() + " feature collection")
                                                          .description(null)
                                                          .operationId("describeCollection")
                                       );
                               openAPI.getPaths()
                                      .addPathItem("/collections/" + ft.getName()
                                                           .toLowerCase(), clonedPathItem1);


                               PathItem clonedPathItem = clonePathItem(featuresPathItems);
                               clonedPathItem
                                       .get(clonedPathItem.getGet()
                                                          .summary("retrieve features of " + ft.getDisplayName() + " feature collection")
                                                          .description(null)
                                                          .operationId("getFeatures")
                                       );

                               Map<String, String> filterableFields = service.getFilterableFieldsForFeatureType(ft);
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
                                      .addPathItem("/collections/" + ft.getName()
                                                           .toLowerCase() + "/items", clonedPathItem);


                               PathItem clonedPathItem2 = clonePathItem(featurePathItem);
                               clonedPathItem2 = clonedPathItem2
                                       .get(clonedPathItem2.getGet()
                                                           .summary("retrieve a " + ft.getDisplayName())
                                                           //.description("")
                                                           .operationId("getFeature")
                                       );

                               openAPI.getPaths()
                                      .addPathItem("/collections/" + ft.getName()
                                                           .toLowerCase() + "/items/{featureId}", clonedPathItem2);
                           }
                       });
            }


            if (StringUtils.isNotBlank(type) && type.trim()
                                                    .equalsIgnoreCase("yaml")) {
                return Response.status(Response.Status.OK)
                               .entity(pretty ? Yaml.pretty(openAPI) : Yaml.mapper()
                                                                           .writeValueAsString(openAPI))
                               .type("application/yaml")
                               .build();
            } else {
                return Response.status(Response.Status.OK)
                               .entity(pretty ? Json.pretty(openAPI) : Json.mapper()
                                                                           .writeValueAsString(openAPI))
                               .type("application/openapi+json;version=3.0"/*MediaTypeCharset.APPLICATION_JSON_UTF8*/)
                               .header("Access-Control-Allow-Origin", "*")
                               .header("Access-Control-Allow-Methods", "GET")
                               .build();
            }
        } catch (IOException e) {
            // ignore
            LOGGER.debug("ERROR", e);
        }
        return Response.noContent()
                       .build();
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
        clonedOperation.setParameters(Lists.newArrayList(operation.getParameters()));
        clonedOperation.setRequestBody(operation.getRequestBody());
        clonedOperation.setResponses(operation.getResponses());
        clonedOperation.setSecurity(operation.getSecurity());
        clonedOperation.setServers(operation.getServers());
        clonedOperation.setSummary(operation.getSummary());
        clonedOperation.setTags(operation.getTags());

        return clonedOperation;
    }

    @Override
    public Optional<OpenAPI> filterOpenAPI(OpenAPI openAPI, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        Optional<OpenAPI> filteredOpenApi = super.filterOpenAPI(openAPI, params, cookies, headers);

        //LOGGER.debug("FILTER {}", filteredOpenApi);

        if (filteredOpenApi.isPresent()) {
            filteredOpenApi.get()
                           .paths(
                                   filteredOpenApi.get()
                                                  .getPaths()
                                                  .entrySet()
                                                  .stream()
                                                  .filter(path -> {
                                                      LOGGER.debug("PATH {} {}", path.getKey(), path.getValue());
                                                      return true;/*path.getKey().startsWith("/service/");*/
                                                  })
                                                  .collect(Collector.of(Paths::new,
                                                          (paths, path) -> paths.addPathItem(path.getKey(), path.getValue()),
                                                          (paths1, paths2) -> {
                                                              paths2.forEach(paths1::addPathItem);
                                                              return paths1;
                                                          })
                                                  )
                           );
        }

        return filteredOpenApi;
    }

    @Override
    public Optional<PathItem> filterPathItem(PathItem pathItem, ApiDescription api, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        Optional<PathItem> clonedPathItem = super.filterPathItem(pathItem, api, params, cookies, headers);

        if (clonedPathItem.isPresent()) {

        }

        return clonedPathItem;
    }

    @Override
    public Optional<Operation> filterOperation(Operation operation, ApiDescription api, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        return super.filterOperation(operation, api, params, cookies, headers);
    }

    @Override
    public Optional<Parameter> filterParameter(Parameter parameter, Operation operation, ApiDescription api, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        return super.filterParameter(parameter, operation, api, params, cookies, headers);
    }

    @Override
    public Optional<RequestBody> filterRequestBody(RequestBody requestBody, Operation operation, ApiDescription api, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        return super.filterRequestBody(requestBody, operation, api, params, cookies, headers);
    }

    @Override
    public Optional<ApiResponse> filterResponse(ApiResponse response, Operation operation, ApiDescription api, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        return super.filterResponse(response, operation, api, params, cookies, headers);
    }

    @Override
    public Optional<Schema> filterSchema(Schema schema, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        return super.filterSchema(schema, params, cookies, headers);
    }

    @Override
    public Optional<Schema> filterSchemaProperty(Schema property, Schema schema, String propName, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        return super.filterSchemaProperty(property, schema, propName, params, cookies, headers);
    }

    @Override
    public boolean isRemovingUnreferencedDefinitions() {
        return super.isRemovingUnreferencedDefinitions();
    }

    protected void doPostProcessing(OpenAPI openApiSpec) {
        openApiSpec.servers(ImmutableList.of(new Server().url("/rest")))
                   .tags(ImmutableList.<Tag>builder()
                           .add(new Tag()
                                   .name("Capabilities")
                                   .description("Essential characteristics of this API including information about the data."))
                           .add(new Tag()
                                   .name("Features")
                                   .description("Access to data (features)."))
                           .build())
                   .components(new Components()
                           .parameters(ImmutableMap.<String, Parameter>builder()
                                   .put("f", new QueryParameter()
                                           .description("The format of the response. If no value is provided, the standard http rules apply, i.e., the accept header shall be used to determine the format.\\nAccepted values are \"xml\", \"json\" and \"html\".")
                                           .schema(new StringSchema()._enum(ImmutableList.of("json", "xml", "html")))
                                           .style(Parameter.StyleEnum.FORM)
                                           .explode(false)
                                           .required(false)
                                           .name("f"))
                                   .put("startIndex", new QueryParameter()
                                           .description("The optional startIndex parameter indicates the index within the result set from which the server shall begin presenting results in the response document. The first element has an index of 0.\\\nMinimum = 0.\\\nDefault = 0.")
                                           .schema(new IntegerSchema()._default(0)
                                                                      .minimum(new BigDecimal(0)))
                                           .style(Parameter.StyleEnum.FORM)
                                           .explode(false)
                                           .required(false)
                                           .name("startIndex"))
                                   .put("limit", new QueryParameter()
                                           .description("The optional limit parameter limits the number of items that are presented in the response document.\\\nOnly items are counted that are on the first level of the collection in the response document. \n" +
                                                   "        Nested objects contained within the explicitly requested items shall not be counted.\\\nMinimum = 1.\\\nMaximum = 10000.\\\nDefault = 10.")
                                           .schema(new IntegerSchema()._default(0)
                                                                      .minimum(new BigDecimal(1))
                                                                      .maximum(new BigDecimal(10000)))
                                           .style(Parameter.StyleEnum.FORM)
                                           .explode(false)
                                           .required(false)
                                           .name("limit"))
                                   .build())
                           .schemas(ImmutableMap.<String, Schema>builder()
                                   .put("exception", new ObjectSchema()
                                           .xml(new XML().name("Exception")
                                                         .namespace("http://www.opengis.net/wfs/3.0")
                                                         .prefix("wfs"))
                                           .properties(ImmutableMap.<String, Schema>builder()
                                                   .put("code", new StringSchema().xml(new XML().name("code")
                                                                                                .attribute(true)))
                                                   .put("description", new StringSchema().xml(new XML().name("description")
                                                                                                       .namespace("http://www.opengis.net/wfs/3.0")
                                                                                                       .prefix("wfs")))
                                                   .build())
                                           .required(ImmutableList.of("code"))
                                   )
                                   .build())
                   );

        openApiSpec.getPaths()
                   .values()
                   .forEach(pathItem -> {
                       pathItem.readOperations()
                               .forEach(operation -> {
                                   operation.parameters(
                                           operation.getParameters()
                                                    .stream()
                                                    .map(parameter -> {
                                                        if (parameter.getName()
                                                                     .equals("f")) {
                                                            return new Parameter().$ref("#/components/parameters/f");
                                                        }
                                                        return parameter;
                                                    })
                                                    .collect(Collectors.toList())
                                   );
                               });
                   });
    }
}

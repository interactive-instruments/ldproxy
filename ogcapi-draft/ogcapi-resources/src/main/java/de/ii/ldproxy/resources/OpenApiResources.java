/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.wfs3.oas30.OpenApiExtension;
import de.ii.ldproxy.wfs3.styles.StylesConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * extend API definition with styles
 */
@Component
@Provides
@Instantiate
public class OpenApiResources implements OpenApiExtension {

    private static final String TAG = "Fetch resources";

    @Override
    public int getSortPriority() {
        return 35;
    }


    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<StylesConfiguration> stylesExtension = getExtensionConfiguration(apiData, StylesConfiguration.class);

        if (stylesExtension.isPresent() &&
                stylesExtension.get()
                        .getResourcesEnabled()) {
            return true;
        }
        return false;
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, OgcApiApiDataV2 datasetData) {

        if (isEnabledForApi(datasetData) &&
                getExtensionConfiguration(datasetData, StylesConfiguration.class).isPresent()) {

            StylesConfiguration stylesExtension = getExtensionConfiguration(datasetData, StylesConfiguration.class).get();

            openAPI.getTags()
                   .add(new Tag().name(TAG)
                                 .description("Fetch resources"));

            // styleId path parameter
            defineResourceIdParameter(openAPI);

            String summary;
            String description;

            Parameter fResources = new Parameter();
            fResources.setName("f");
            fResources.in("query");
            description = "The format of the response. If no value is provided, the standard http rules apply, " +
                    "i.e., the accept header shall be used to determine the format. Pre-defined values are: ";
            fResources.setRequired(false);
            fResources.setStyle(Parameter.StyleEnum.FORM);
            fResources.setExplode(false);
            List<String> fEnum = new ArrayList<>();

            fEnum.add("json");
            description += " 'json' (JSON);";

            if (stylesExtension.getHtmlEnabled()) {
                fEnum.add("html");
                description += " 'html' (HTML);";
            }

            description += " the response to other values is determined by the server.";
            fResources.description(description);

            fResources.setSchema(new StringSchema()._enum(fEnum));
            openAPI.getComponents()
                   .addParameters("f-resources", fResources);

            description = "the Resource resource";
            openAPI.getPaths()
                   .addPathItem("/resources", new PathItem().description(description));
            PathItem pathItemResources = openAPI.getPaths()
                                             .get("/resources");

            description = "the Resource resource";
            openAPI.getPaths()
                   .addPathItem("/resources/{resourceId}", new PathItem().description(description));
            PathItem pathItemResource = openAPI.getPaths()
                                            .get("/resources/{resourceId}");

            Content contentException = new Content().addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")));
            if (stylesExtension.getHtmlEnabled()) {
                contentException.addMediaType("text/html", new MediaType().schema(new StringSchema()));
            }
            ApiResponse status400 = new ApiResponse().description("Invalid or unknown query parameters or content.")
                                                     .content(contentException);
            ApiResponse status404 = new ApiResponse().description("Resource not found.");
            ApiResponse status406 = new ApiResponse().description("The media types accepted by the client are not supported for this resource.")
                                                     .content(contentException);

            if (Objects.nonNull(pathItemResources)) {
                summary = "information about the available resources (symbols, sprites, thumbnails)";
                description =  "This operation fetches the set of resources that have been " +
                    "created and that may be used by reference in stylesheets. Typical resources in this " +
                    "Styles API are symbols, sprites and thumbnails. For each resource the id and " +
                    "a link to the resource is provided.";
                Content contentResources = new Content().addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/styles")));
                if (stylesExtension.getHtmlEnabled()) {
                    contentResources.addMediaType("text/html", new MediaType().schema(new StringSchema()));
                }
                defineResourcesSchema(openAPI);

                pathItemResources.get(new Operation()
                        .addTagsItem(TAG)
                        .summary(summary)
                        .description(description)
                        .operationId("getResources")
                        .addParametersItem(new Parameter().$ref("#/components/parameters/f-resources"))
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse().description("The set of available resources.")
                                                                        .content(contentResources))
                                .addApiResponse("400", status400)
                                .addApiResponse("406", status406)));
            }

            if (Objects.nonNull(pathItemResource)) {
                summary = "fetch a symbol resource by id";
                description = "Fetches the resource with identifier `resourceId`. The set of \n" +
                        "available resources can be retrieved at `/resources`.";
                Content contentResource = new Content();
                contentResource.addMediaType("*/*", new MediaType());

                pathItemResource.get(new Operation()
                        .addTagsItem(TAG)
                        .summary(summary)
                        .description(description)
                        .operationId("getResource")
                        .addParametersItem(new Parameter().$ref("#/components/parameters/resourceId"))
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse().description("The resource.")
                                                                        .content(contentResource))
                                .addApiResponse("400", status400)
                                .addApiResponse("406", status406)));
            }
        }
        return openAPI;
    }

    private void defineResourcesSchema(OpenAPI openAPI) {
        Schema resources = new Schema();
        resources.setType("object");
        List<String> required = new ArrayList<>();
        required.add("resources");
        resources.setRequired(required);
        resources.addProperties("resources", new ArraySchema().items(new Schema().$ref("#/components/schemas/resourceEntry"))
                                                        .nullable(true));
        openAPI.getComponents()
               .addSchemas("resources", resources);

        defineResourceEntrySchema(openAPI);

    }

    private void defineResourceEntrySchema(OpenAPI openAPI) {
        Schema resourceEntry = new Schema();
        resourceEntry.setType("object");

        List<String> required = new ArrayList<>();
        required.add("id");
        required.add("link");
        resourceEntry.setRequired(required);

        resourceEntry.addProperties("id", new StringSchema());
        resourceEntry.addProperties("link", new Schema().$ref("#/components/schemas/link"));

        openAPI.getComponents()
               .addSchemas("resourceEntry", resourceEntry);

    }

    private void defineResourceIdParameter(OpenAPI openAPI) {
        Parameter resourceId = new Parameter();
        resourceId.setName("resourceId");
        resourceId.in("path");
        resourceId.description("Local identifier of a symbol resource. A list of all available resources can be found under the `/resources` path.");
        resourceId.setRequired(true);
        Schema styleIdSchema = new Schema();
        styleIdSchema.setType("string");
        resourceId.setSchema(styleIdSchema);
        openAPI.getComponents()
               .addParameters("resourceId", resourceId);
    }
}

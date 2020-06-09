/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles.manager;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.wfs3.oas30.OpenApiExtension;
import de.ii.ldproxy.wfs3.styles.StylesConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
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
 * Extend the API definition with style management
 */
@Component
@Provides
@Instantiate
public class OpenApiStylesManager implements OpenApiExtension {

    private static final String TAG = "Manage Styles";

    @Override
    public int getSortPriority() {
        return 30;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<StylesConfiguration> stylesExtension = getExtensionConfiguration(apiData, StylesConfiguration.class);

        if (stylesExtension.isPresent() &&
                stylesExtension.get()
                        .getManagerEnabled()) {
            return true;
        }
        return false;
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, OgcApiApiDataV2 datasetData) {

        /*
        if (!isEnabledForApi(datasetData)) {
            return openAPI;
        }

        StylesConfiguration stylesExtension = getExtensionConfiguration(datasetData, StylesConfiguration.class).get();

        if (!stylesExtension.getManagerEnabled()) {
            return openAPI;
        }

        openAPI.getTags()
               .add(new Tag().name(TAG)
                             .description("Create, update and delete styles and their metadata; only for style authors"));

        String summary;
        String description;

        PathItem pathItemStyles = openAPI.getPaths()
                                         .get("/styles");
        PathItem pathItemStyle = openAPI.getPaths()
                                        .get("/styles/{styleId}");
        PathItem pathItemMetadata = openAPI.getPaths()
                                           .get("/styles/{styleId}/metadata");

        Content contentException = new Content().addMediaType("application/json", new MediaType().schema(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/exception")));
        if (stylesExtension.getHtmlEnabled()) {
            contentException.addMediaType("text/html", new MediaType().schema(new StringSchema()));
        }
        ApiResponse status400 = new ApiResponse().description("Invalid or unknown query parameters or content.")
                                                 .content(contentException);
        ApiResponse status404 = new ApiResponse().description("Style not found.");
        ApiResponse status409 = new ApiResponse().description("A style with that id already exists.")
                                                 .content(contentException);
        ApiResponse status415 = new ApiResponse().description("The content sent by the client is in a media type that is not accepted by the server for this resource.")
                                                 .content(contentException);
        ApiResponse status422 = new ApiResponse().description("Unprocessable request, the patch document appears to be valid, but the server is incapable of processing the request")
                                                 .content(contentException);

        Parameter validate;
        if (stylesExtension.getValidationEnabled()) {
            validate = new Parameter();
            validate.setName("validate");
            validate.in("query");
            description = "'yes' creates a new style after successful validation and returns 400," +
                    "if validation fails, ’no' creates the style without validation and 'only' just " +
                    "validates the style without creating a new style and returns 400, if validation " +
                    "fails, otherwise 204.";
            validate.description(description);
            validate.setRequired(false);
            validate.setStyle(Parameter.StyleEnum.FORM);
            validate.setExplode(false);
            List<String> validateEnum = new ArrayList<>();
            validateEnum.add("yes");
            validateEnum.add("no");
            validateEnum.add("only");
            validate.setSchema(new StringSchema()._enum(validateEnum)
                                                 ._default("no"));
            openAPI.getComponents()
                   .addParameters("validate", validate);
        }

        Content contentStylesheet = pathItemStyle.getGet()
                                                 .getResponses()
                                                 .get("200")
                                                 .getContent();
        RequestBody requestBodyStyleheet =
                new RequestBody().description("A stylesheet in one of the supported formats.")
                                 .content(contentStylesheet);

        Content contentMetadata = new Content().addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/styleMetadata")));
        if (stylesExtension.getHtmlEnabled()) {
            contentMetadata.addMediaType("text/html", new MediaType().schema(new StringSchema()));
        }
        RequestBody requestBodyMetadata =
                new RequestBody().description("The style metadata object (PUT) or the parts that are updated (PATCH).")
                                 .content(contentMetadata);

        if (Objects.nonNull(pathItemStyles)) {
            summary = "adds a new style";
            description = "Adds a style to the style repository";
            if (stylesExtension.getValidationEnabled()) {
                description += " or just validates a style.\n" +
                        "If the parameter `validate` is set to `yes`, the style will be validated before adding " +
                        "the style to the server. If the parameter `validate` is set to `only`, the server will " +
                        "not be changed and only the validation result will be returned";
            }
            description += ".\n" +
                    "If a new style is created, the following rules apply:\n" +
                    "* If the style submitted in the request body includes an identifier (this depends on " +
                    "the style encoding), that identifier will be used. If a style with that identifier " +
                    "already exists, an error is returned.\n" +
                    "* If no identifier can be determined from the submitted style, the server will assign " +
                    "a new identifier to the style.\n" +
                    "* A minimal style metadata resource is created at `/styles/{styleId}/metadata`. Please " +
                    "update the metadata using a PUT request to keep the style metadata consistent with " +
                    "the style definition.\n" +
                    "* The URI of the new style is returned in the header `Location`.\n";
            // TODO: "* This operation is only available to registered style authors.";

            ApiResponses postStyles = new ApiResponses()
                    .addApiResponse("201", new ApiResponse().description("Style created")
                                                            .addHeaderObject("Location",
                                                                    new Header().description("URI of the new style")
                                                                                .schema(new StringSchema())))
                    .addApiResponse("400", status400)
                    .addApiResponse("409", status409)
                    .addApiResponse("415", status415);

            Operation opPostStyles = new Operation()
                    .summary(summary)
                    .description(description)
                    .operationId("addStyle")
                    .addTagsItem(TAG)
                    .requestBody(requestBodyStyleheet);

            if (stylesExtension.getValidationEnabled()) {
                postStyles.addApiResponse("204", new ApiResponse().description("Style validated successfully, no style has been created."));
                opPostStyles.addParametersItem(new Parameter().$ref("#/components/parameters/validate"));
            }

            pathItemStyles.post(opPostStyles.responses(postStyles));
        }

        if (Objects.nonNull(pathItemStyle)) {
            summary = "replace a style or add a new style";
            description = "Replace an existing style with the id `styleId`. If no such style exists, " +
                    "a new style with that id is added.\n";
            if (stylesExtension.getValidationEnabled()) {
                description +=
                        "If the parameter `validate` is set to `yes`, the style will be validated before adding " +
                                "the style to the server. If the parameter `validate` is set to `only`, the server will " +
                                "not be changed and only the validation result will be returned.\n";
            }
            description += "For updated styles, the style metadata resource at `/styles/{styleId}/metadata` " +
                    "is not updated. For new styles a minimal style metadata resource is created. Please " +
                    "update the metadata using a PUT request to keep the style metadata consistent with " +
                    "the style definition.";
            // TODO: "* This operation is only available to registered style authors.";

            ApiResponses putStyle = new ApiResponses()
                    .addApiResponse("204", new ApiResponse().description("Style updated, created or validated successfully."))
                    .addApiResponse("400", status400)
                    .addApiResponse("404", status404)
                    .addApiResponse("415", status415);

            Operation opPutStyle = new Operation()
                    .summary(summary)
                    .description(description)
                    .operationId("updateStyle")
                    .addTagsItem(TAG)
                    .addParametersItem(new Parameter().$ref("#/components/parameters/styleId"))
                    .requestBody(requestBodyStyleheet);

            if (stylesExtension.getValidationEnabled()) {
                opPutStyle.addParametersItem(new Parameter().$ref("#/components/parameters/validate"));
            }

            summary = "delete a style";
            description = "Delete an existing style with the id `styleId`. If no such style exists, " +
                    "an error is returned. Deleting a style also deletes the subordinate resources, " +
                    "i.e., the style metadata.";
            // TODO: "* This operation is only available to registered style authors.";

            ApiResponses deleteStyle = new ApiResponses()
                    .addApiResponse("204", new ApiResponse().description("Style deleted."))
                    .addApiResponse("404", status404);

            Operation opDeleteStyle = new Operation()
                    .summary(summary)
                    .description(description)
                    .operationId("deleteStyle")
                    .addTagsItem(TAG)
                    .addParametersItem(new Parameter().$ref("#/components/parameters/styleId"));

            pathItemStyle.put(opPutStyle.responses(putStyle))
                         .delete(opDeleteStyle.responses(deleteStyle));
        }

        if (Objects.nonNull(pathItemMetadata)) {
            summary = "update the metadata document of a style";
            description = "Update the style metadata for the style with the id `styleId`. This operation " +
                    "updates the complete metadata document.";
            // TODO: "* This operation is only available to registered style authors.";

            ApiResponses putMetadata = new ApiResponses()
                    .addApiResponse("204", new ApiResponse().description("Style metadata updated."))
                    .addApiResponse("400", status400)
                    .addApiResponse("404", status404)
                    .addApiResponse("415", status415);

            Operation opPutMetadata = new Operation()
                    .summary(summary)
                    .description(description)
                    .operationId("updateStyleMetadata")
                    .addTagsItem(TAG)
                    .addParametersItem(new Parameter().$ref("#/components/parameters/styleId"))
                    .requestBody(requestBodyMetadata);

            summary = "update parts of the style metadata";
            description = "Update selected elements of the style metadata for " +
                    "the style with the id `styleId`.\n" +
                    "The PATCH semantics in this operation are defined by " +
                    "RFC 7396 (JSON Merge Patch). From the specification:\n" +
                    "\n" +
                    "_'A JSON merge patch document describes changes to be " +
                    "made to a target JSON document using a syntax that " +
                    "closely mimics the document being modified. Recipients " +
                    "of a merge patch document determine the exact set of " +
                    "changes being requested by comparing the content of " +
                    "the provided patch against the current content of the " +
                    "target document. If the provided merge patch contains " +
                    "members that do not appear within the target, those " +
                    "members are added. If the target does contain the " +
                    "member, the value is replaced. Null values in the " +
                    "merge patch are given special meaning to indicate " +
                    "the removal of existing values in the target.'_\n" +
                    "\n" +
                    "Some examples:\n" +
                    "\n" +
                    "To add or update the point of contact, the access" +
                    "constraint and the revision date, just send\n" +
                    "\n" +
                    "```\n" +
                    "{\n" +
                    "  \"pointOfContact\": \"Jane Doe\",\n" +
                    "  \"accessConstraints\": \"restricted\",\n" +
                    "  \"dates\": {\n" +
                    "    \"revision\": \"2019-05-17T11:46:12Z\"\n" +
                    "  }\n" +
                    "}\n" +
                    "```\n" +
                    "\n" +
                    "To remove the point of contact, the access " +
                    "constraint and the revision date, send \n" +
                    "\n" +
                    "```\n" +
                    "{\n" +
                    "  \"pointOfContact\": null,\n" +
                    "  \"accessConstraints\": null,\n" +
                    "  \"dates\": {\n" +
                    "    \"revision\": null\n" +
                    "  }\n" +
                    "}\n" +
                    "```\n" +
                    "\n" +
                    "For arrays the complete array needs to be sent. " +
                    "To add a keyword to the example style metadata object, send\n" +
                    "\n" +
                    "```\n" +
                    "{\n" +
                    "  \"keywords\": [ \"basemap\", \"TDS\", \"TDS 6.1\", \"OGC API\", \"new keyword\" ]\n" +
                    "}\n" +
                    "```\n" +
                    "\n" +
                    "To remove the \"TDS\" keyword, send\n" +
                    "\n" +
                    "```\n" +
                    "{\n" +
                    "  \"keywords\": [ \"basemap\", \"TDS 6.1\", \"OGC API\", \"new keyword\" ]\n" +
                    "}\n" +
                    "```\n" +
                    "\n" +
                    "To remove the keywords, send\n" +
                    "\n" +
                    "```\n" +
                    "{\n" +
                    "  \"keywords\": null\n" +
                    "}\n" +
                    "```\n" +
                    "\n" +
                    "The same applies to `stylesheets` and `layers`. To update " +
                    "these members, you have to send the complete new array value.";
            // TODO: "* This operation is only available to registered style authors.";

            ApiResponses patchMetadata = new ApiResponses()
                    .addApiResponse("204", new ApiResponse().description("Style metadata updated."))
                    .addApiResponse("400", status400)
                    .addApiResponse("404", status404)
                    .addApiResponse("415", new ApiResponse().description("The content sent by the client is in a media type that is not accepted by the server for this resource.")
                                                            .content(contentException)
                                                            .addHeaderObject("Accept-Patch",
                                                                    new Header().description("Supported patch document media types.")
                                                                                .schema(new StringSchema())))
                    .addApiResponse("422", status422);

            Operation opPatchMetadata = new Operation()
                    .summary(summary)
                    .description(description)
                    .operationId("patchStyleMetadata")
                    .addTagsItem(TAG)
                    .addParametersItem(new Parameter().$ref("#/components/parameters/styleId"))
                    .requestBody(requestBodyMetadata);

            pathItemMetadata.put(opPutMetadata.responses(putMetadata))
                            .patch(opPatchMetadata.responses(patchMetadata));
        }
         */

        return openAPI;
    }
}

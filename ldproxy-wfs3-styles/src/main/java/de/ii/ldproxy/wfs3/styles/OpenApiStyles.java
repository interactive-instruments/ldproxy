/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.wfs3.oas30.Wfs3OpenApiExtension;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
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

/**
 * extend API definition with styles
 */
@Component
@Provides
@Instantiate
public class OpenApiStyles implements Wfs3OpenApiExtension {

    private static final String TAG = "Use styles";

    @Override
    public int getSortPriority() {
        return 25;
    }


    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData dataset) {
        return isExtensionEnabled(dataset, StylesConfiguration.class);
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, OgcApiDatasetData datasetData) {

        if (isEnabledForDataset(datasetData) &&
                getExtensionConfiguration(datasetData, StylesConfiguration.class).isPresent()) {

            StylesConfiguration stylesExtension = getExtensionConfiguration(datasetData, StylesConfiguration.class).get();

            openAPI.getTags()
                   .add(new Tag().name(TAG)
                                 .description("Discover and fetch styles"));

            // styleId path parameter
            defineStyleIdParameter(openAPI);

            String summary;
            String description;

            Parameter fStylesheets = new Parameter();
            fStylesheets.setName("f");
            fStylesheets.in("query");
            description = "The format of the response. If no value is provided, the standard http rules apply, " +
                    "i.e., the accept header shall be used to determine the format. Pre-defined values are: ";
            fStylesheets.setRequired(false);
            fStylesheets.setStyle(Parameter.StyleEnum.FORM);
            fStylesheets.setExplode(false);
            List<String> fEnum = new ArrayList<>();

            if (stylesExtension.getMbStyleEnabled()) {
                defineMbStyleSchema(openAPI);
                fEnum.add("mbs");
                description += " 'mbs' (Mapbox Style version 8);";
            }

            if (stylesExtension.getSld10Enabled()) {
                defineSld10Schema(openAPI);
                fEnum.add("sld10");
                description += " 'sld10' (OGC Styled Layer Descriptor 1.0);";
            }

            if (stylesExtension.getSld11Enabled()) {
                defineSld11Schema(openAPI);
                fEnum.add("sld11");
                description += " 'sld11' (OGC Styled Layer Descriptor 1.1);";
            }

            description += " the response to other values is determined by the server.";
            fStylesheets.description(description);

            fStylesheets.setSchema(new StringSchema()._enum(fEnum));
            openAPI.getComponents()
                   .addParameters("f-stylesheets", fStylesheets);

            Parameter fStyles = new Parameter();
            fStyles.setName("f");
            fStyles.in("query");
            description = "The format of the response. If no value is provided, the standard http rules apply, " +
                    "i.e., the accept header shall be used to determine the format. Pre-defined values are: ";
            fStyles.setRequired(false);
            fStyles.setStyle(Parameter.StyleEnum.FORM);
            fStyles.setExplode(false);
            List<String> fEnum2 = new ArrayList<>();

            fEnum2.add("json");
            description += " 'json' (JSON);";

            if (stylesExtension.getHtmlEnabled()) {
                fEnum2.add("html");
                description += " 'html' (HTML);";
            }

            description += " the response to other values is determined by the server.";
            fStyles.description(description);

            fStyles.setSchema(new StringSchema()._enum(fEnum2));
            openAPI.getComponents()
                   .addParameters("f-styles", fStyles);

            description = "the Styles resource";
            openAPI.getPaths()
                   .addPathItem("/styles", new PathItem().description(description));
            PathItem pathItemStyles = openAPI.getPaths()
                                             .get("/styles");

            description = "the Style resource";
            openAPI.getPaths()
                   .addPathItem("/styles/{styleId}", new PathItem().description(description));
            PathItem pathItemStyle = openAPI.getPaths()
                                            .get("/styles/{styleId}");

            description = "the Style metadata resource";
            openAPI.getPaths()
                   .addPathItem("/styles/{styleId}/metadata", new PathItem().description(description));
            PathItem pathItemMetadata = openAPI.getPaths()
                                               .get("/styles/{styleId}/metadata");

            Content contentException = new Content().addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")));
            if (stylesExtension.getHtmlEnabled()) {
                contentException.addMediaType("text/html", new MediaType().schema(new StringSchema()));
            }
            ApiResponse status400 = new ApiResponse().description("Invalid or unknown query parameters or content.")
                                                     .content(contentException);
            ApiResponse status404 = new ApiResponse().description("Resource not found.");
            ApiResponse status406 = new ApiResponse().description("The media types accepted by the client are not supported for this resource.")
                                                     .content(contentException);

            if (Objects.nonNull(pathItemStyles)) {
                summary = "information about the available styles";
                description = "This operation fetches the set of styles available. For " +
                        "each style the id, a title, links to the stylesheet of " +
                        "the style in each supported encoding, and the link to the " +
                        "metadata is provided.";
                Content contentStyles = new Content().addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/styles")));
                if (stylesExtension.getHtmlEnabled()) {
                    contentStyles.addMediaType("text/html", new MediaType().schema(new StringSchema()));
                }
                defineStylesSchema(openAPI);

                pathItemStyles.get(new Operation()
                        .addTagsItem(TAG)
                        .summary(summary)
                        .description(description)
                        .operationId("getStyles")
                        .addParametersItem(new Parameter().$ref("#/components/parameters/f-styles"))
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse().description("The set of available styles.")
                                                                        .content(contentStyles))
                                .addApiResponse("400", status400)
                                .addApiResponse("406", status406)));
            }

            if (Objects.nonNull(pathItemStyle)) {
                summary = "fetch a style by id";
                description = "Fetches the style with identifier `styleId`. The set of available styles can be " +
                        "retrieved at `/styles`. Not all styles are available in all style encodings.";
                Content contentStylesheet = new Content();
                if (stylesExtension.getMbStyleEnabled()) {
                    contentStylesheet.addMediaType("application/vnd.mapbox.style+json", new MediaType().schema(new Schema().$ref("#/components/schemas/mbStyle")));
                }
                if (stylesExtension.getSld10Enabled()) {
                    contentStylesheet.addMediaType("application/vnd.ogc.sld+xml;version=1.0", new MediaType().schema(new Schema().$ref("#/components/schemas/sld10")));
                }
                if (stylesExtension.getSld11Enabled()) {
                    contentStylesheet.addMediaType("application/vnd.ogc.sld+xml;version=1.1", new MediaType().schema(new Schema().$ref("#/components/schemas/sld11")));
                }

                pathItemStyle.get(new Operation()
                        .addTagsItem(TAG)
                        .summary(summary)
                        .description(description)
                        .operationId("getStyle")
                        .addParametersItem(new Parameter().$ref("#/components/parameters/styleId"))
                        .addParametersItem(new Parameter().$ref("#/components/parameters/f-stylesheets"))
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse().description("The stylesheet in the requested style encoding.")
                                                                        .content(contentStylesheet))
                                .addApiResponse("400", status400)
                                .addApiResponse("406", status406)));
            }

            if (Objects.nonNull(pathItemMetadata)) {
                summary = "fetch the metadata about a style";
                description = "Style metadata is essential information about a style in order to support users " +
                        "to discover and select styles for rendering their data and for visual style editors to " +
                        "create user interfaces for editing a style.\n" +
                        "This operations returns the metadata for the requested style as a single document.\n" +
                        "The stylesheet of the style will typically include some the metadata, too.";
                Content contentStyleMetadata = new Content().addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/styleMetadata")));
                if (stylesExtension.getHtmlEnabled()) {
                    contentStyleMetadata.addMediaType("text/html", new MediaType().schema(new StringSchema()));
                }

                pathItemMetadata.get(new Operation()
                        .addTagsItem(TAG)
                        .summary(summary)
                        .description(description)
                        .operationId("getStyleMetadata")
                        .addParametersItem(new Parameter().$ref("#/components/parameters/styleId"))
                        .addParametersItem(new Parameter().$ref("#/components/parameters/f-styles"))
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse().description("The metadata for the style.")
                                                                        .content(contentStyleMetadata))
                                .addApiResponse("400", status400)
                                .addApiResponse("404", status404)
                                .addApiResponse("406", status406)));
            }
        }
        return openAPI;
    }

    private void defineStylesSchema(OpenAPI openAPI) {
        Schema styles = new Schema();
        styles.setType("object");
        List<String> required = new ArrayList<>();
        required.add("styles");
        styles.setRequired(required);
        styles.addProperties("styles", new ArraySchema().items(new Schema().$ref("#/components/schemas/styleEntry"))
                                                        .nullable(true));
        openAPI.getComponents()
               .addSchemas("styles", styles);

        defineStyleEntrySchema(openAPI);

    }

    private void defineStyleEntrySchema(OpenAPI openAPI) {
        Schema styleEntry = new Schema();
        styleEntry.setType("object");

        List<String> required = new ArrayList<>();
        required.add("id");
        required.add("links");
        styleEntry.setRequired(required);

        styleEntry.addProperties("id", new StringSchema().nullable(true));
        styleEntry.addProperties("title", new StringSchema().nullable(true));
        styleEntry.addProperties("links", new ArraySchema().items(new Schema().$ref("#/components/schemas/link"))
                                                           .minItems(1)
                                                           .nullable(true));

        openAPI.getComponents()
               .addSchemas("styleEntry", styleEntry);

    }

    private void defineMbStyleSchema(OpenAPI openAPI) {
        Schema mbStyle = new Schema();
        mbStyle.setType("object");

        /* TODO: add complete schema based on https://docs.mapbox.com/mapbox-gl-js/style-spec

        List<String> requiredStyle = new LinkedList<String>();
        requiredStyle.add("version");
        requiredStyle.add("sources");
        requiredStyle.add("layers");
        mbStyle.setRequired(requiredStyle);

        mbStyle.addProperties("version", new Schema().type("number"));
        mbStyle.addProperties("name", new Schema().type("string"));
        mbStyle.addProperties("sources",
                new Schema().type("object")
                        .addProperties(serviceId,
                                new Schema().type("object")
                                        .addProperties("type", new Schema().type("string"))
                                        .addProperties("url", new Schema().type("string"))));
        mbStyle.addProperties("sprite", new Schema().type("string"));
        mbStyle.addProperties("glyphs", new Schema().type("string"));
        mbStyle.addProperties("layers", new ArraySchema().items(new Schema().$ref("#/components/schemas/layersArray")));

        Schema layersArray = new Schema();
        layersArray.setType("object");

        List<String> requiredLayers = new LinkedList<String>();
        requiredLayers.add("id");
        requiredLayers.add("type");
        layersArray.setRequired(requiredLayers);

        List<String> typeEnum = new ArrayList<String>();
        typeEnum.add("fill");
        typeEnum.add("line");
        typeEnum.add("symbol");
        typeEnum.add("circle");
        typeEnum.add("heatmap");
        typeEnum.add("fill-extrusion");
        typeEnum.add("raster");
        typeEnum.add("hillshade");
        typeEnum.add("background");

        layersArray.addProperties("id", new Schema().type("string"));
        layersArray.addProperties("type", new StringSchema()._enum(typeEnum));
        layersArray.addProperties("source", new Schema().type("string"));
        layersArray.addProperties("source-layer", new Schema().type("string"));
        layersArray.addProperties("layout", new Schema().type("object"));
        layersArray.addProperties("paint", new Schema().type("object")
                .addProperties("fill-color", new StringSchema()));

        openAPI.getComponents()
                .addSchemas("layersArray", layersArray);
         */

        openAPI.getComponents()
               .addSchemas("mbStyle", mbStyle);

    }

    private void defineSld10Schema(OpenAPI openAPI) {
        Schema string = new StringSchema();
        openAPI.getComponents()
               .addSchemas("sld10", string);
    }

    private void defineSld11Schema(OpenAPI openAPI) {
        Schema string = new StringSchema();
        openAPI.getComponents()
               .addSchemas("sld11", string);
    }

    private void defineStyleIdParameter(OpenAPI openAPI) {
        Parameter styleId = new Parameter();
        styleId.setName("styleId");
        styleId.in("path");
        styleId.description("Local identifier of a style. A list of all available styles can be found under the /styles path.");
        styleId.setRequired(true);
        Schema styleIdSchema = new Schema();
        styleIdSchema.setType("string");
        styleId.setSchema(styleIdSchema);
        openAPI.getComponents()
               .addParameters("styleId", styleId);
    }
}

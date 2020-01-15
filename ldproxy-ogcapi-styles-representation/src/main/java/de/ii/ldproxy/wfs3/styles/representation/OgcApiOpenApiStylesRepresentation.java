/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/*Not very useful
package de.ii.ldproxy.wfs3.styles.representation;


import de.ii.ldproxy.ogcapi.features.core.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.oas30.Wfs3OpenApiExtension;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
*/
/**
 * extend API definition with styles Representation
 *
 *//*
@Component
@Provides
@Instantiate
public class Wfs3OpenApiStylesRepresentation implements Wfs3OpenApiExtension {
    @Override
    public int getSortPriority() {
        return 40;
    }


    @Override
    public OpenAPI process(OpenAPI openAPI, Wfs3ServiceData serviceData) {
        Parameter fOnlyHtml = new Parameter();
        fOnlyHtml.setName("f");
        fOnlyHtml.in("query");
        fOnlyHtml.description("\\\n" +
                "        The format of the response. If no value is provided, the standard http rules apply, i.e., the accept header shall be used to determine the format.\\\n" +
                "        The only pre-defined value is \"html\". The response to other values is determined by the server.");
        fOnlyHtml.setRequired(false);
        fOnlyHtml.setStyle(Parameter.StyleEnum.FORM);
        fOnlyHtml.setExplode(false);
        List<String> fOnlyHtmlEnum = new ArrayList<String>();
        fOnlyHtmlEnum.add("html");
        fOnlyHtml.setSchema(new StringSchema()._enum(fOnlyHtmlEnum));
        fOnlyHtml.example("html");

        openAPI.getComponents().addParameters("fOnlyHtml", fOnlyHtml);


        openAPI.getPaths().addPathItem("/styleRepresentation/{styleId}", new PathItem().description("something"));
        PathItem pathItem = openAPI.getPaths().get("/styleRepresentation/{styleId}");
        ApiResponse success = new ApiResponse().description("A representation of the style from the dataset")
                .content(new Content()
                        .addMediaType("application/json", new MediaType().schema(new StringSchema())) //TODO - what to do here?
                );
        ApiResponse exception = new ApiResponse().description("An error occured.")
                .content(new Content()
                        .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                );
        if (Objects.nonNull(pathItem)) {
            pathItem
                    .get(new Operation()
                            .addTagsItem("Styles")
                            .summary("get a representation of the dataset with the specified styleId")
                            .operationId("getStyleRepresentation")
                            .addParametersItem(new Parameter().$ref("#/components/parameters/styleIdentifierDataset"))
                            .addParametersItem(new Parameter().$ref("#/components/parameters/fOnlyHtml"))

                            //.requestBody(requestBody)
                            .responses(new ApiResponses()
                                    .addApiResponse("200", success)
                                    .addApiResponse("default", exception))
                    );
        }

        openAPI.getPaths()
                .addPathItem("/styleRepresentation/{styleId}", pathItem);
        return openAPI;
    }
}
*/
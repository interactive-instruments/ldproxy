/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package ii.de.ldproxy.resources.manager; /**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.wfs3.oas30.OpenApiExtension;
import de.ii.ldproxy.wfs3.styles.StylesConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
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

import java.util.Objects;
import java.util.Optional;

/**
 * Extend the API definition with symbol resource management
 */
@Component
@Provides
@Instantiate
public class OpenApiResourcesManager implements OpenApiExtension {

    private static final String TAG = "Manage Resource";

    @Override
    public int getSortPriority() {
        return 40;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        Optional<StylesConfiguration> stylesExtension = getExtensionConfiguration(apiData, StylesConfiguration.class);

        if (stylesExtension.isPresent() &&
                stylesExtension.get()
                        .getResourceManagerEnabled()) {
            return true;
        }
        return false;
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, OgcApiDatasetData datasetData) {

        if (!isEnabledForApi(datasetData)) {
            return openAPI;
        }

        StylesConfiguration stylesExtension = getExtensionConfiguration(datasetData, StylesConfiguration.class).get();

        if (!stylesExtension.getManagerEnabled()) {
            return openAPI;
        }

        openAPI.getTags()
               .add(new Tag().name(TAG)
                             .description("Create, update and delete symbols/sprites; only for style authors"));

        String summary;
        String description;

        PathItem pathItemResource = openAPI.getPaths()
                                        .get("/resources/{resourceId}");

        Content contentException = new Content().addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")));
        if (stylesExtension.getHtmlEnabled()) {
            contentException.addMediaType("text/html", new MediaType().schema(new StringSchema()));
        }
        ApiResponse status400 = new ApiResponse().description("Invalid or unknown query parameters or content.")
                                                 .content(contentException);
        ApiResponse status404 = new ApiResponse().description("Resource not found.");

        Content contentResource = pathItemResource.getGet()
                .getResponses()
                .get("200")
                .getContent();
        RequestBody requestBodyResource =
                new RequestBody().content(contentResource);

        if (Objects.nonNull(pathItemResource)) {
            summary = "replace a symbol resource or add a new one";
            description = "Replace an existing resource with the id `resourceId`. If no " +
                    "such resource exists, a new resource with that id is added. " +
                    "A sprite used in a Mapbox Style stylesheet consists of " +
                    "three resources. Each of the resources needs to be created " +
                    "(and eventually deleted) separately.\n" +
                    "The PNG bitmap image (resourceId ends in '.png'), the JSON " +
                    "index file (resourceId of the same name, but ends in '.json' " +
                    "instead of '.png') and the PNG  bitmap image for " +
                    "high-resolution displays (the file ends in '.@2x.png').\n" +
                    "The resource will only by available in the native format in " +
                    "which the resource is posted. There is no support for" +
                    "automated conversions to other representations.";
            // TODO: "* This operation is only available to registered style authors.";

            ApiResponses putResource = new ApiResponses()
                    .addApiResponse("204", new ApiResponse().description("Resource updated or created successfully."))
                    .addApiResponse("400", status400)
                    .addApiResponse("404", status404);

            Operation opPutResource = new Operation()
                    .summary(summary)
                    .description(description)
                    .operationId("updateResource")
                    .addTagsItem(TAG)
                    .addParametersItem(new Parameter().$ref("#/components/parameters/resourceId"))
                    .requestBody(requestBodyResource);

            summary = "delete a symbol resource";
            description = "Delete an existing resource with the id `resourceId`. If no " +
                    "such resource exists, an error is returned.";
            // TODO: "* This operation is only available to registered style authors.";

            ApiResponses deleteResource = new ApiResponses()
                    .addApiResponse("204", new ApiResponse().description("Resource deleted."))
                    .addApiResponse("404", status404);

            Operation opDeleteResource = new Operation()
                    .summary(summary)
                    .description(description)
                    .operationId("deleteResource")
                    .addTagsItem(TAG)
                    .addParametersItem(new Parameter().$ref("#/components/parameters/resourceId"));

            pathItemResource.put(opPutResource.responses(putResource))
                         .delete(opDeleteResource.responses(deleteResource));
        }

        return openAPI;
    }
}

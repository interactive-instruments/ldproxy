package de.ii.ldproxy.wfs3.styles.manager;

import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.oas30.Wfs3OpenApiExtension;
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
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Comparator;
import java.util.Objects;

import static de.ii.ldproxy.wfs3.styles.StylesConfiguration.EXTENSION_KEY;

/**
 * extend API definition with styles
 *
 */
@Component
@Provides
@Instantiate
public class Wfs3OpenApiStylesManager implements Wfs3OpenApiExtension {

    @Override
    public int getSortPriority() {
        return 30;
    }


    @Override
    public OpenAPI process(OpenAPI openAPI, Wfs3ServiceData serviceData) {


        if (serviceData != null && isExtensionEnabled(serviceData,EXTENSION_KEY)) {

            StylesConfiguration stylesExtension = (StylesConfiguration) getExtensionConfiguration(serviceData,EXTENSION_KEY).get();

            if (stylesExtension.getManagerEnabled()) {
                PathItem pathItem1 = openAPI.getPaths().get("/styles");


                PathItem pathItem2 = openAPI.getPaths().get("/styles/{styleId}");


                RequestBody requestBody = new RequestBody().description("A single style.")
                        .content(new Content().addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/style"))));
                ApiResponse exception = new ApiResponse().description("An error occured.")
                        .content(new Content().addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception"))));

                if (Objects.nonNull(pathItem1)) {
                    pathItem1
                            .post(new Operation()
                                    .summary("add styles to the dataset ")
                                    //.description("")
                                    .operationId("addStyles")
                                    .addTagsItem("Styles")
                                    .requestBody(requestBody)
                                    .responses(new ApiResponses().addApiResponse("201", new ApiResponse().description("Styles were created.")
                                            .addHeaderObject("location", new Header().description("The URL of the first created style")
                                                    .schema(new StringSchema())))
                                            .addApiResponse("default", exception))
                            );
                }

                if (Objects.nonNull(pathItem2)) {
                    pathItem2
                            .put(new Operation()
                                    .summary("replace a style from the dataset")
                                    //.description("")
                                    .operationId("replaceStyle")
                                    .addTagsItem("Styles")
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/styleIdentifier"))
                                    .requestBody(requestBody)
                                    .responses(new ApiResponses().addApiResponse("204", new ApiResponse().description("Style was replaced."))
                                            .addApiResponse("default", exception))
                            )
                            .delete(new Operation()
                                    .summary("delete a Style from the dataset")
                                    .operationId("deleteStyle")
                                    .addTagsItem("Styles")
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/styleIdentifier"))
                                    .responses(new ApiResponses().addApiResponse("204", new ApiResponse().description("Style was deleted."))
                                            .addApiResponse("default", exception))
                            );
                }

                serviceData.getFeatureTypes()
                        .values()
                        .stream()
                        .sorted(Comparator.comparing(FeatureTypeConfigurationWfs3::getId))
                        .filter(ft -> serviceData.isFeatureTypeEnabled(ft.getId()))
                        .forEach(ft -> {

                            PathItem pathItem3 = openAPI.getPaths()
                                    .get(String.format("/collections/%s/styles", ft.getId()));
                            PathItem pathItem4 = openAPI.getPaths()
                                    .get(String.format("/collections/%s/styles/{styleId}", ft.getId()));


                            if (Objects.nonNull(pathItem3)) {
                                pathItem3
                                        .post(new Operation()
                                                .summary("add styles to the collection " + ft.getLabel())
                                                //.description("")
                                                .operationId("addStyle" + ft.getLabel())
                                                .addTagsItem("Styles")
                                                .requestBody(requestBody)
                                                .responses(new ApiResponses().addApiResponse("201", new ApiResponse().description("Style was created.")
                                                        .addHeaderObject("location", new Header().description("The URL of the created style")
                                                                .schema(new StringSchema())))
                                                        .addApiResponse("default", exception))
                                        );
                            }


                            if (Objects.nonNull(pathItem4)) {
                                pathItem4
                                        .put(new Operation()
                                                .summary("replace a style from the collection " + ft.getLabel())
                                                //.description("")
                                                .operationId("replaceStyle" + ft.getId())
                                                .addTagsItem("Styles")
                                                .addParametersItem(new Parameter().$ref("#/components/parameters/styleIdentifier"))
                                                .requestBody(requestBody)
                                                .responses(new ApiResponses().addApiResponse("204", new ApiResponse().description("Style was replaced."))
                                                        .addApiResponse("default", exception))
                                        )
                                        .delete(new Operation()
                                                .summary("delete a Style from the collection  " + ft.getLabel())
                                                .operationId("deleteStyle" + ft.getId())
                                                .addTagsItem("Styles")
                                                .addParametersItem(new Parameter().$ref("#/components/parameters/styleIdentifier"))
                                                .responses(new ApiResponses().addApiResponse("204", new ApiResponse().description("Style was deleted."))
                                                        .addApiResponse("default", exception))
                                        );
                            }

                        });
            }
        }

        return openAPI;
    }


}

package de.ii.ldproxy.wfs3.transactional;

import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.oas30.Wfs3OpenApiExtension;
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

/**
 * @author zahnen
 */
@Component
@Provides
//@Instantiate
public class Wfs3OpenApiTransactional implements Wfs3OpenApiExtension {
    @Override
    public int getSortPriority() {
        return 10;
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, Wfs3ServiceData serviceData) {
        if (serviceData != null) {

            serviceData.getFeatureTypes()
                       .values()
                       .stream()
                       .sorted(Comparator.comparing(FeatureTypeConfigurationWfs3::getId))
                       .filter(ft -> serviceData.isFeatureTypeEnabled(ft.getId()))
                       .forEach(ft -> {

                           PathItem pathItem = openAPI.getPaths()
                                                      .get(String.format("/collections/%s/items", ft.getId()));

                           RequestBody requestBody = new RequestBody().description("A single feature.")
                                                                  .content(new Content().addMediaType("application/geo+json", new MediaType().schema(new Schema().$ref("#/components/schemas/featureGeoJSON"))));
                           ApiResponse exception = new ApiResponse().description("An error occured.")
                                                                  .content(new Content().addMediaType("application/geo+json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception"))));

                           if (Objects.nonNull(pathItem)) {
                               pathItem
                                       .post(new Operation()
                                               .summary("add features to the " + ft.getLabel() + " feature collection")
                                               //.description("")
                                               .operationId("addFeatures" + ft.getId())
                                               .tags(pathItem.getGet()
                                                             .getTags())
                                               .requestBody(requestBody)
                                               .responses(new ApiResponses().addApiResponse("201", new ApiResponse().description("Features were created.")
                                                                                                                    .addHeaderObject("location", new Header().description("The URL of the first created feature")
                                                                                                                                                             .schema(new StringSchema())))
                                                                            .addApiResponse("default", exception))
                                       );
                           }

                           PathItem pathItem2 = openAPI.getPaths()
                                                       .get(String.format("/collections/%s/items/{featureId}", ft.getId()));

                           if (Objects.nonNull(pathItem2)) {
                               pathItem2
                                       .put(new Operation()
                                               .summary("replace a " + ft.getLabel())
                                               //.description("")
                                               .operationId("replaceFeature" + ft.getId())
                                               .tags(pathItem2.getGet()
                                                              .getTags())
                                               .addParametersItem(new Parameter().$ref("#/components/parameters/featureId"))
                                               .requestBody(requestBody)
                                               .responses(new ApiResponses().addApiResponse("204", new ApiResponse().description("Feature was replaced."))
                                                                            .addApiResponse("default", exception))
                                       )
                                       .delete(new Operation()
                                               .summary("delete a " + ft.getLabel())
                                               //.description("")
                                               .operationId("deleteFeature" + ft.getId())
                                               .tags(pathItem2.getGet()
                                                              .getTags())
                                               .addParametersItem(new Parameter().$ref("#/components/parameters/featureId"))
                                               .responses(new ApiResponses().addApiResponse("204", new ApiResponse().description("Feature was deleted."))
                                                                            .addApiResponse("default", exception))
                                       );
                           }

                       });
        }

        return openAPI;
    }
}

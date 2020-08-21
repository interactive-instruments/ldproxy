package de.ii.ldproxy.resources;

import de.ii.ldproxy.ogcapi.common.domain.OgcApiCommonConfiguration;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGenerator;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Provides
@Instantiate
public class ResourcesFormatJson implements ResourcesFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schemaResources;
    public final static String SCHEMA_REF_RESOURCES = "#/components/schemas/Resources";

    public ResourcesFormatJson() {
        schemaResources = schemaGenerator.getSchema(Resources.class);
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {

        // TODO add examples
        if (path.equals("/resources"))
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(schemaResources)
                    .schemaRef(SCHEMA_REF_RESOURCES)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new RuntimeException("Unexpected path: " + path);
    }

    @Override
    public Response getResourcesResponse(Resources resources, OgcApi api, ApiRequestContext requestContext) {
        boolean includeLinkHeader = api.getData().getExtension(OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeLinkHeader)
                .orElse(false);

        return Response.ok(resources)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .links(includeLinkHeader ? resources.getLinks().stream().map(link -> link.getLink()).toArray(Link[]::new) : null)
                .build();
    }
}

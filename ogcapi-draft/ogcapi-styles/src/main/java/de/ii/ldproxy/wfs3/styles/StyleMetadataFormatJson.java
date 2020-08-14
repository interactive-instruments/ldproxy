package de.ii.ldproxy.wfs3.styles;

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
public class StyleMetadataFormatJson implements StyleMetadataFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schemaStyleMetadata;
    public final static String SCHEMA_REF_STYLE_METADATA = "#/components/schemas/StyleMetadata";

    public StyleMetadataFormatJson() {
        schemaStyleMetadata = schemaGenerator.getSchema(StyleMetadata.class);
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean canSupportTransactions() {
        return true;
    }

    @Override
    public Response getStyleMetadataResponse(StyleMetadata metadata, OgcApiApi api, OgcApiRequestContext requestContext) {
        boolean includeLinkHeader = api.getData().getExtension(OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeLinkHeader)
                .orElse(false);

        return Response.ok()
                .entity(metadata)
                .links(includeLinkHeader ? metadata.getLinks().stream().map(link -> link.getLink()).toArray(Link[]::new) : null)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {

        // TODO add examples
        if (path.equals("/styles/{styleId}/metadata"))
            return new ImmutableOgcApiMediaTypeContent.Builder()
                    .schema(schemaStyleMetadata)
                    .schemaRef(SCHEMA_REF_STYLE_METADATA)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new RuntimeException("Unexpected path: " + path);
    }

    @Override
    public OgcApiMediaTypeContent getRequestContent(OgcApiApiDataV2 apiData, String path, OgcApiContext.HttpMethods method) {
        if (path.equals("/styles/{styleId}/metadata") && (method==OgcApiContext.HttpMethods.PUT || method==OgcApiContext.HttpMethods.PATCH))
            return new ImmutableOgcApiMediaTypeContent.Builder()
                    .schema(schemaStyleMetadata)
                    .schemaRef(SCHEMA_REF_STYLE_METADATA)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new RuntimeException("Unexpected path: " + path);
    }
}

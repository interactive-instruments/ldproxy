package de.ii.ldproxy.wfs3.styles;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGenerator;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGeneratorImpl;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Provides
@Instantiate
public class StylesFormatJson implements StylesFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schemaStyles;
    public final static String SCHEMA_REF_STYLES = "#/components/schemas/Styles";
    private final Schema schemaStyleMetadata;
    public final static String SCHEMA_REF_STYLE_METADATA = "#/components/schemas/StyleMetadata";

    public StylesFormatJson() {
        schemaStyles = schemaGenerator.getSchema(Styles.class);
        schemaStyleMetadata = schemaGenerator.getSchema(StyleMetadata.class);
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Response getStylesResponse(Styles styles, OgcApiApi api, OgcApiRequestContext requestContext) {
        boolean includeLinkHeader = api.getData().getExtension(OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeLinkHeader)
                .orElse(false);

        return Response.ok(styles)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .links(includeLinkHeader ? styles.getLinks().stream().map(link -> link.getLink()).toArray(Link[]::new) : null)
                .build();
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {

        // TODO add examples
        if (path.equals("/styles"))
            return new ImmutableOgcApiMediaTypeContent.Builder()
                    .schema(schemaStyles)
                    .schemaRef(SCHEMA_REF_STYLES)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();
        else if (path.equals("/styles/{styleId}/metadata"))
            return new ImmutableOgcApiMediaTypeContent.Builder()
                    .schema(schemaStyleMetadata)
                    .schemaRef(SCHEMA_REF_STYLE_METADATA)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new ServerErrorException("Unexpected path "+path,500);
    }
}

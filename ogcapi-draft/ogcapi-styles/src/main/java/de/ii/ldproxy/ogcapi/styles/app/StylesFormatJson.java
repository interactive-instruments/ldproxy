package de.ii.ldproxy.ogcapi.styles.app;

import de.ii.ldproxy.ogcapi.common.domain.CommonConfiguration;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.SchemaGenerator;
import de.ii.ldproxy.ogcapi.styles.domain.Styles;
import de.ii.ldproxy.ogcapi.styles.domain.StylesFormatExtension;
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
public class StylesFormatJson implements StylesFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schemaStyles;
    public final static String SCHEMA_REF_STYLES = "#/components/schemas/Styles";

    public StylesFormatJson() {
        schemaStyles = schemaGenerator.getSchema(Styles.class);
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Response getStylesResponse(Styles styles, OgcApi api, ApiRequestContext requestContext) {
        boolean includeLinkHeader = api.getData().getExtension(CommonConfiguration.class)
                .map(CommonConfiguration::getIncludeLinkHeader)
                .orElse(false);

        return Response.ok(styles)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .links(includeLinkHeader ? styles.getLinks().stream().map(link -> link.getLink()).toArray(Link[]::new) : null)
                .build();
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {

        // TODO add examples
        if (path.equals("/styles"))
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(schemaStyles)
                    .schemaRef(SCHEMA_REF_STYLES)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new RuntimeException("Unexpected path: " + path);
    }
}

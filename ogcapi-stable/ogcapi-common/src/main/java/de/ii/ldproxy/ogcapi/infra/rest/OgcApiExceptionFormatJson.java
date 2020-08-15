package de.ii.ldproxy.ogcapi.infra.rest;

import de.ii.ldproxy.ogcapi.domain.*;
import io.dropwizard.jersey.errors.ErrorEntityWriter;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;

@Component
@Provides
@Instantiate
public class OgcApiExceptionFormatJson extends ErrorEntityWriter<OgcApiErrorMessage, OgcApiErrorMessage> implements OgcApiExceptionFormatExtension {

    private final Schema schema;

    public static final String SCHEMA_REF_EXCEPTIONS = "#/components/schemas/Exceptions";

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(MediaType.valueOf("application/problem+json"))
            .build();

    public OgcApiExceptionFormatJson() {
        super(MEDIA_TYPE.type(), OgcApiErrorMessage.class);
        // cannot yet use SchemaGenerator to generate the schema from the OgcApiErrorMessage class, so we generate the schema manually
        schema = new ObjectSchema().addProperties("title", new StringSchema())
                                   .addProperties("detail", new StringSchema())
                                   .addProperties("status", new IntegerSchema().minimum(BigDecimal.valueOf(100)))
                                   .addProperties("instance", new StringSchema().format("uri"));
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(SCHEMA_REF_EXCEPTIONS)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public Object getExceptionEntity(OgcApiErrorMessage errorMessage) {
        return errorMessage;
    }

    @Override
    protected OgcApiErrorMessage getRepresentation(OgcApiErrorMessage entity) {
        return entity;
    }
}

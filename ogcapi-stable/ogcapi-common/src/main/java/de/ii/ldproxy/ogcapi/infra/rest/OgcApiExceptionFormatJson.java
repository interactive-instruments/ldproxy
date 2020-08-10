package de.ii.ldproxy.ogcapi.infra.rest;

import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGenerator;
import io.dropwizard.jersey.errors.ErrorEntityWriter;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

@Component
@Provides
@Instantiate
public class OgcApiExceptionFormatJson extends ErrorEntityWriter<OgcApiErrorMessage, OgcApiErrorMessage> implements OgcApiExceptionFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    private final Schema schemaExceptions;

    public static final String SCHEMA_REF_EXCEPTIONS = "#/components/schemas/Exceptions";

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(MediaType.valueOf("application/problem+json"))
            .build();

    public OgcApiExceptionFormatJson(MediaType contentType, Class<OgcApiErrorMessage> representation) {
        super(MEDIA_TYPE.type(), OgcApiErrorMessage.class);
        schemaExceptions = schemaGenerator.getSchema(OgcApiErrorMessage.class);

    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(schemaExceptions)
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

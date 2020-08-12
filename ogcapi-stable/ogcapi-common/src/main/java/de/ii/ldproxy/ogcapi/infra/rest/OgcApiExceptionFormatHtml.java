package de.ii.ldproxy.ogcapi.infra.rest;

import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaTypeContent;
import io.dropwizard.jersey.errors.ErrorEntityWriter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.core.MediaType;

@Component
@Provides
@Instantiate
public class OgcApiExceptionFormatHtml extends ErrorEntityWriter<OgcApiErrorMessage, OgcApiErrorView> implements OgcApiExceptionFormatExtension {

    static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .build();


    public OgcApiExceptionFormatHtml() {
        super(MediaType.TEXT_HTML_TYPE, OgcApiErrorView.class);
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(new StringSchema().example("<html>...</html>"))
                .schemaRef("#/components/schemas/htmlSchema")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public Object getExceptionEntity(OgcApiErrorMessage errorMessage) {
        return getRepresentation(errorMessage);
    }

    @Override
    protected OgcApiErrorView getRepresentation(OgcApiErrorMessage errorMessage) {
        return new OgcApiErrorView(errorMessage);
    }
}

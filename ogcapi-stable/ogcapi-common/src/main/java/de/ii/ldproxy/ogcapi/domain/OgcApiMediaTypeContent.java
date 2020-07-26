package de.ii.ldproxy.ogcapi.domain;

import io.swagger.v3.oas.models.media.Schema;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
public interface OgcApiMediaTypeContent {
        String getSchemaRef();
        Schema getSchema();
        List<OgcApiExample> getExamples();
        OgcApiMediaType getOgcApiMediaType();
}

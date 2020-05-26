package de.ii.ldproxy.ogcapi.domain;

import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Schema;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
public interface OgcApiMediaTypeContent {
        String getSchemaRef();
        Schema getSchema();
        Optional<Object> getExample();
        OgcApiMediaType getOgcApiMediaType();
}

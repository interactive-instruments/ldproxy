package de.ii.ldproxy.ogcapi.domain;

import io.swagger.v3.oas.models.headers.Header;
import org.immutables.value.Value;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
public interface OgcApiRequestBody {
    default String getStatusCode() { return "200"; } ;
    Optional<String> getId(); // TODO set for reusable responses
    String getDescription();
    Map<MediaType, OgcApiMediaTypeContent> getContent();
    default boolean getRequired() { return false; }
}

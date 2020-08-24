package de.ii.ldproxy.ogcapi.domain;

import org.immutables.value.Value;

import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
public interface ApiRequestBody {
    default String getStatusCode() { return "200"; } ;
    Optional<String> getId(); // TODO set for reusable responses
    String getDescription();
    Map<MediaType, ApiMediaTypeContent> getContent();
    default boolean getRequired() { return false; }
}

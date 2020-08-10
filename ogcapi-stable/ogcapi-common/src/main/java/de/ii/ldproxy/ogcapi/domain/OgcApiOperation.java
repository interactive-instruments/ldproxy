package de.ii.ldproxy.ogcapi.domain;

import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Value.Immutable
public interface OgcApiOperation {
    String getSummary();
    Optional<String> getDescription();
    Optional<OgcApiExternalDocumentation> getExternalDocs();
    Set<String> getTags();
    Optional<String> getOperationId();
    List<OgcApiQueryParameter> getQueryParameters();
    Optional<OgcApiRequestBody> getRequestBody();
    Optional<OgcApiResponse> getSuccess();
    @Value.Default
    default boolean getHideInOpenAPI() { return false; }
}

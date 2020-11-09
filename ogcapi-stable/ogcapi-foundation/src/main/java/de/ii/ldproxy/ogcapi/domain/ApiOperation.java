package de.ii.ldproxy.ogcapi.domain;

import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Value.Immutable
public interface ApiOperation {
    String getSummary();
    Optional<String> getDescription();
    Optional<ExternalDocumentation> getExternalDocs();
    Set<String> getTags();
    Optional<String> getOperationId();
    List<OgcApiQueryParameter> getQueryParameters();
    Optional<ApiRequestBody> getRequestBody();
    Optional<ApiResponse> getSuccess();
    @Value.Default
    default boolean getHideInOpenAPI() { return false; }
}

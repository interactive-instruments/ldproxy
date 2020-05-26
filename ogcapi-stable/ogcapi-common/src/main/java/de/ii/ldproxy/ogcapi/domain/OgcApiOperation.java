package de.ii.ldproxy.ogcapi.domain;

import io.swagger.v3.oas.models.parameters.RequestBody;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.Set;

@Value.Immutable
public interface OgcApiOperation {
    String getSummary();
    Optional<String> getDescription();
    Set<String> getTags();
    Optional<String> getOperationId();
    Set<OgcApiQueryParameter> getQueryParameters();
    Optional<OgcApiRequestBody> getRequestBody();
    Optional<OgcApiResponse> getSuccess();
}

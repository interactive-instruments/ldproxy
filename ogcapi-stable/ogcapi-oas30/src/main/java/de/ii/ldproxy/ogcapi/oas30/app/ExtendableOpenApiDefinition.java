package de.ii.ldproxy.ogcapi.oas30.app;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;

import javax.ws.rs.core.Response;

public interface ExtendableOpenApiDefinition {
    Response getOpenApi(String type, URICustomizer requestUriCustomizer, OgcApiDataV2 apiData);
}

package de.ii.ldproxy.wfs3.oas30;

import de.ii.ldproxy.wfs3.api.Wfs3Extension;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * @author zahnen
 */
public interface Wfs3OpenApiExtension extends Wfs3Extension {
    int getSortPriority();

    OpenAPI process(OpenAPI openAPI, Wfs3ServiceData serviceData);
}

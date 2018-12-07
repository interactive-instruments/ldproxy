package de.ii.ldproxy.wfs3.styles.manager;

import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.oas30.Wfs3OpenApiExtension;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 * extend API definition with styles
 *
 */
@Component
@Provides
@Instantiate
public class Wfs3OpenApiStylesManager implements Wfs3OpenApiExtension {

    @Override
    public int getSortPriority() {
        return 30;
    }


    @Override
    public OpenAPI process(OpenAPI openAPI, Wfs3ServiceData serviceData) {


        return openAPI;
    }


}

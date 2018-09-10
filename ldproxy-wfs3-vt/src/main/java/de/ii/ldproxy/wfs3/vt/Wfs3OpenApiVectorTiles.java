/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.oas30.Wfs3OpenApiExtension;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 * TODO: this is just a placeholder.
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3OpenApiVectorTiles implements Wfs3OpenApiExtension {
    @Override
    public int getSortPriority() {
        return 20;
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, Wfs3ServiceData serviceData) {

        //TODO: extend openapi definition, see e.g. de.ii.ldproxy.wfs3.transactional.Wfs3OpenApiTransactional

        return openAPI;
    }
}

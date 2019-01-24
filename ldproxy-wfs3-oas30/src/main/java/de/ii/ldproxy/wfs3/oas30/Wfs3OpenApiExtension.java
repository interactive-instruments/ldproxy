/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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

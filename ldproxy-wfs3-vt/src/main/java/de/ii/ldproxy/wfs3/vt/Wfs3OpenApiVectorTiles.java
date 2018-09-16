/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.oas30.Wfs3OpenApiExtension;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Comparator;
import java.util.Objects;

/**
 * extend API definition with tile resources
 *
 * @author portele
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

        // TODO: extend API definition

        // /tilingSchemes
        // /tilingSchemes/{tilingSchemeId}

        // /tiles/{tilingSchemeId}/{level}/{row}/{col}
        // f
        // properties
        // collections

        // /collections/{collectionId}
        // add tilingSchemes

        // /collections/{collectionId}/tiles/{tilingSchemeId}

        // /collections/{collectionId}/tiles/{tilingSchemeId}/{level}/{row}/{col}
        // f
        // properties
        // collections

        return openAPI;
    }
}

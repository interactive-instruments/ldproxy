/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.oas30;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Provides
@Instantiate
public class OpenApiYaml implements ApiDefinitionFormatExtension {

    private static Logger LOGGER = LoggerFactory.getLogger(OpenApiYaml.class);
    private static OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "vnd.oai.openapi", ImmutableMap.of("version", "3.0")))
            .parameter("yaml")
            .label("YAML")
            .build();

    @Requires
    private ExtendableOpenApiDefinition openApiDefinition;

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, Oas30Configuration.class);
    }

    @Override
    public Response getApiDefinitionResponse(OgcApiDatasetData apiData,
                                             OgcApiRequestContext wfs3Request) {
        LOGGER.debug("MIME {}", "YAML");
        return openApiDefinition.getOpenApi("yaml", wfs3Request.getUriCustomizer().copy(), apiData);
    }
}

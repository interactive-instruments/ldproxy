/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.processing.Processing;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGenerator;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingOutputFormatProcessing;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Provides
@Instantiate
public class ObservationProcessingJson implements ObservationProcessingOutputFormatProcessing {

    @Requires
    SchemaGenerator schemaGenerator;

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schema;
    private static final String DAPA_PATH_ELEMENT = "dapa";
    private final static String schemaRef = "#/components/schemas/"+DAPA_PATH_ELEMENT;

    public ObservationProcessingJson() {
        this.schema = schemaGenerator.getSchema(Processing.class);
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return getExtensionConfiguration(apiData, ObservationProcessingConfiguration.class)
                .map(ObservationProcessingConfiguration::getEnabled)
                .orElse(false);
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        // get the collectionId from the path, [0] is empty, [1] is "collections"
        String collectionId = path.split("/", 4)[2];

        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(schemaRef)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public boolean contentPerApi() {
        return true;
    }

    @Override
    public Response getResponse(Processing processList, String collectionId, OgcApiApi api, OgcApiRequestContext requestContext) {
        return Response.ok()
                .entity(processList)
                .build();
    }
}

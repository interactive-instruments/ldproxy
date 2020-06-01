/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGenerator;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGeneratorImpl;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingOutputFormatVariables;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class ObservationProcessingVariablesJson implements ObservationProcessingOutputFormatVariables {

    @Requires
    SchemaGenerator schemaGenerator;

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schema;
    private final static String schemaRef = "#/components/schemas/Variables";

    public ObservationProcessingVariablesJson() {
        this.schema = schemaGenerator.getSchema(Variables.class);
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

        // get the variables from the API
        List<Variable> variables = (collectionId.equals("{collectionId}") ?
                getExtensionConfiguration(apiData, ObservationProcessingConfiguration.class) :
                getExtensionConfiguration(apiData, apiData.getCollections().get(collectionId),
                        ObservationProcessingConfiguration.class))
                .map(ObservationProcessingConfiguration::getVariables)
                .orElse(ImmutableList.of());
        ObjectNode json = null;
        if (!variables.isEmpty()) {
            // reduce to the first three items
            ImmutableVariables example = ImmutableVariables.builder()
                    .variables(variables.stream().limit(3).collect(Collectors.toList()))
                    .build();

            // convert to JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new Jdk8Module());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            json = mapper.convertValue(example, ObjectNode.class);
        }


        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(schemaRef)
                .ogcApiMediaType(MEDIA_TYPE)
                .example(Optional.ofNullable(json))
                .build();
    }

    @Override
    public boolean contentPerApi() {
        return true;
    }

    @Override
    public Response getResponse(Variables variables, String collectionId, OgcApiApi api, OgcApiRequestContext requestContext) {
        return Response.ok()
                .entity(variables)
                .build();
    }

}

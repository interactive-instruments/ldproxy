/**
 * Copyright 2021 interactive instruments GmbH
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
import de.ii.ldproxy.ogcapi.domain.SchemaGenerator;
import de.ii.ldproxy.ogcapi.observation_processing.api.DapaVariablesFormatExtension;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class DapaVariablesFormatJson implements DapaVariablesFormatExtension {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schema;
    private final static String schemaRef = "#/components/schemas/Variables";

    public DapaVariablesFormatJson(@Requires SchemaGenerator schemaGenerator) {
        this.schema = schemaGenerator.getSchema(Variables.class);
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        // get the collectionId from the path, [0] is empty, [1] is "collections"
        String collectionId = path.split("/", 4)[2];

        Optional<FeatureTypeConfigurationOgcApi> collectionData = Optional.ofNullable(apiData.getCollections()
                                                                                             .get(collectionId));

        // get the variables from the API
        List<Variable> variables = (collectionData.isPresent() ?
                collectionData.get().getExtension(ObservationProcessingConfiguration.class) :
                apiData.getExtension(ObservationProcessingConfiguration.class))
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

        return new ImmutableApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(schemaRef)
                .ogcApiMediaType(MEDIA_TYPE)
                .addExamples(new ImmutableExample.Builder().value(Optional.ofNullable(json)).build())
                .build();
    }

    @Override
    public boolean contentPerApi() {
        return true;
    }

    @Override
    public Object getEntity(Variables variables, String collectionId, OgcApi api, ApiRequestContext requestContext) {
        return variables;
    }

}

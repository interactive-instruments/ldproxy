/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.*;
import de.ii.ldproxy.ogcapi.domain.SchemaValidator;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

// TODO convert to a component
public class SchemaValidatorImpl implements SchemaValidator {

    public Optional<String> validate(String schemaContent, String jsonContent) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchemaFactory validatorFactory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance()).objectMapper(mapper).build();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setTypeLoose(true);
        config.setFailFast(true);
        config.setHandleNullableField(true);
        JsonSchema schema = validatorFactory.getSchema(schemaContent,config);
        JsonNode jsonNode = mapper.readTree(jsonContent);
        Set<ValidationMessage> result;
        try {
            result = schema.validate(jsonNode);
        } catch (JsonSchemaException e) {
            result = e.getValidationMessages();
        }
        if (result.size() > 0) {
            return Optional.of(result.toString());
        }

        return Optional.empty();
    }

    /*

    public Optional<String> validate(String schemaContent, String jsonContent) throws IOException {
        return Optional.empty();
    }

    public Optional<String> validate(String schemaContent, String jsonContent) throws IOException {
        JsonValidationService service = JsonValidationService.newInstance();

        JsonSchema schema = service.readSchema(new CharSequenceReader(schemaContent));

        ProblemHandler handler = service.createProblemPrinter(System.out::println);

        try (JsonReader reader = service.createReader(new CharSequenceReader(jsonContent), schema, handler)) {
            JsonValue value = reader.readValue();
            String dbg = value.toString();
            // Do something useful here
        }

        return Optional.empty();
    }
     */

    /*
    In general, this library would be a better solution since it uses Jackson,
    but it has a dependency to a different version - 2.10 instead of 2.9.

     */
}
/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.immutables.value.Value;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
public interface ApiResponse {
    @Value.Default
    default String getStatusCode() { return "200"; }
    Optional<String> getId();
    String getDescription();
    List<ApiHeader> getHeaders();
    Map<MediaType, ApiMediaTypeContent> getContent();

    default void updateOpenApiDefinition(OgcApiDataV2 apiData, OpenAPI openAPI, Operation op) {
        io.swagger.v3.oas.models.responses.ApiResponse response = new io.swagger.v3.oas.models.responses.ApiResponse();
        response.description(getDescription());
        getHeaders()
            .forEach(header -> response.addHeaderObject(header.getId(), newHeader(apiData, header)));
        Content content = new Content();
        getContent()
            .forEach((key, value) -> content.addMediaType(key.toString(), newMediaType(openAPI, value)));
        response.content(content);

        getId().ifPresentOrElse(id -> {
            if (!openAPI.getComponents().getResponses().containsKey(id)) {
                openAPI.getComponents().getResponses().put(id, response);
            }
            op.responses(new ApiResponses().addApiResponse(getStatusCode(), new io.swagger.v3.oas.models.responses.ApiResponse().$ref("#/components/responses/"+id)));
        }, () -> {
            op.responses(new ApiResponses().addApiResponse(getStatusCode(), response));
        });
    }

    private Header newHeader(OgcApiDataV2 apiData, ApiHeader header) {
        return new Header().description(header.getDescription())
            .schema(header.getSchema(apiData));
    }

    private io.swagger.v3.oas.models.media.MediaType newMediaType(OpenAPI openAPI, ApiMediaTypeContent content) {
        io.swagger.v3.oas.models.media.MediaType mediaType = new io.swagger.v3.oas.models.media.MediaType();
        String schemaRef = content.getSchemaRef();
        mediaType.schema(new Schema<>().$ref(schemaRef));
        if (schemaRef.startsWith("#/components/schemas/")) {
            String schemaId = schemaRef.substring(21);
            if (!openAPI.getComponents().getSchemas().containsKey(schemaId)) {
                openAPI.getComponents().getSchemas().put(schemaId, content.getSchema());
            }
            content.referencedSchemas().forEach((refSchemaId, refSchema) -> {
                if (!openAPI.getComponents().getSchemas().containsKey(refSchemaId)) {
                    openAPI.getComponents().getSchemas().put(refSchemaId, refSchema);
                }
            });
        }

        // Just set the first "example" as Swagger UI does not show "examples"
        content.getExamples()
            .stream()
            .map(Example::getValue)
            .flatMap(Optional::stream)
            .findFirst()
            .ifPresent(mediaType::setExample);

        return mediaType;
    }
}

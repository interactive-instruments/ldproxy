/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.google.common.base.Splitter;
import io.swagger.v3.oas.models.OpenAPI;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * # Resource paths
 * @langEn ll resource paths in this documentation are relative to the base URI of the deployment. For example given the
 * base URI `https://example.com/pfad/zu/apis` and the resource path `/{apiId}/collections`, the full path would be
 * `https://example.com/pfad/zu/apis/{apiId}/collections`.
 * @langDe Alle Pfadangaben in dieser Dokumentation sind relativ zur Basis-URI des Deployments.
 * Ist dies zum Beispiel `https://example.com/pfad/zu/apis` und lautet der Pfad einer Ressource
 * `/{apiId}/collections` dann ist die URI der Ressource `https://example.com/pfad/zu/apis/{apiId}/collections`.
 */

/**
 * A resource is any path/URI supported by the API. Through path parameters any instance may represent
 * multiple resources of the API.
 */
public interface OgcApiResource {

    Logger LOGGER = LoggerFactory.getLogger(OgcApiResource.class);

    String getPath();

    List<OgcApiPathParameter> getPathParameters();
    Map<String, ApiOperation> getOperations();

    default boolean isSubCollectionWithExplicitId(OgcApiDataV2 apiData) {
        return getPathParameters().stream().anyMatch(param -> "collectionId".equals(param.getName()) && param.isExplodeInOpenApi(apiData));
    }

    default Optional<String> getCollectionId(OgcApiDataV2 apiData) {
        return isSubCollectionWithExplicitId(apiData)? Optional.ofNullable(Splitter.on("/").limit(3).omitEmptyStrings().splitToList(getPath()).get(1)) :Optional.empty();
    }

    @Value.Lazy
    default String getPathPattern() {
        String path = getPath();
        for (OgcApiPathParameter param : getPathParameters()) {
            path = path.replace("{"+param.getName()+"}", param.getPattern());
        }
        if (path.contains("{")) {
            LOGGER.error("Could not resolve a path parameter to a pattern. This should not occur and will lead to errors (requires a fix in the code). Using '.*' as a fallback. Resource path: {}", path);
            path = path.replaceAll("\\{\\w+\\}",".*");
        }
        return "^(?:"+path+")/?$";
    }

    @Value.Lazy
    default Pattern getPathPatternCompiled() {
        return Pattern.compile(getPathPattern());
    }

    default void updateOpenApiDefinition(OgcApiDataV2 apiData, OpenAPI openAPI) {
        String path = getPath();
        if (path.startsWith("/api")) {
            // skip the API definition
            return;
        }

        Optional<String> collectionId = getCollectionId(apiData);

        for (Map.Entry<String, ApiOperation> operationEntry : getOperations().entrySet()) {
            String method = operationEntry.getKey();
            Set<Integer> errorCodes = initErrorStatusCodes(method);
            ApiOperation operation = operationEntry.getValue();
            if (operation.getSuccess().isEmpty() || operation.hideInOpenAPI()) {
                // skip
                continue;
            }
            operation.updateOpenApiDefinition(apiData, collectionId, openAPI, this, method, errorCodes);
        }
    }

    private Set<Integer> initErrorStatusCodes(String method) {
        HashSet<Integer> errorCodes = new HashSet<>();
        switch (method) {
            case "GET":
                errorCodes.add(406);
                break;
            case "POST":
                errorCodes.add(415);
                errorCodes.add(422);
                break;
            case "PUT":
            case "PATCH":
                errorCodes.add(415);
                break;
            case "DELETE":
            default:
        }
        return errorCodes;
    }
}

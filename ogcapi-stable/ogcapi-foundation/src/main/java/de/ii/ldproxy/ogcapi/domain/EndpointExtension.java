/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.auth.domain.User;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition.SORT_PRIORITY_DUMMY;

public interface EndpointExtension extends ApiExtension {

    default ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
        return new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("")
                .sortPriority(SORT_PRIORITY_DUMMY)
                .build();
    }

    default ImmutableSet<ApiMediaType> getMediaTypes(OgcApiDataV2 apiData, String requestSubPath) {
        return getMediaTypes(apiData, requestSubPath, "GET");
    }

    default ImmutableSet<ApiMediaType> getMediaTypes(OgcApiDataV2 apiData, String requestSubPath, String method) {
        ApiEndpointDefinition apiDef = getDefinition(apiData);
        if (apiDef.getResources().isEmpty())
            return ImmutableSet.of();

        OgcApiResource resource = apiDef.getResource(apiDef.getPath(requestSubPath))
                                        .orElse(null);
        if (resource!=null) {
            ApiOperation operation = resource.getOperations().get(method);
            if (operation!=null && operation.getSuccess().isPresent()) {
                return operation.getSuccess().get()
                        .getContent()
                        .values()
                        .stream()
                        .map(content -> content.getOgcApiMediaType())
                        .collect(ImmutableSet.toImmutableSet());
            }
            return ImmutableSet.of();
        }

        throw new ServerErrorException("Invalid sub path: "+requestSubPath, 500);
    }

    default List<OgcApiQueryParameter> getParameters(OgcApiDataV2 apiData, String requestSubPath) {
        ApiEndpointDefinition apiDef = getDefinition(apiData);
        if (apiDef.getResources().isEmpty())
            return ImmutableList.of();

        OgcApiResource resource = apiDef.getResource(apiDef.getPath(requestSubPath))
                                        .orElse(null);
        if (resource != null) {
            ApiOperation operation = resource.getOperations().get("GET");
            if (operation != null && operation.getSuccess().isPresent()) {
                return operation.getQueryParameters();
            }
        }
        return ImmutableList.of();
    }

    default ImmutableList<OgcApiPathParameter> getPathParameters(ExtensionRegistry extensionRegistry, OgcApiDataV2 apiData, String definitionPath) {
        return extensionRegistry.getExtensionsForType(OgcApiPathParameter.class)
                .stream()
                .filter(param -> param.isApplicable(apiData, definitionPath))
                .sorted(Comparator.comparing(ParameterExtension::getName))
                .collect(ImmutableList.toImmutableList());
    }

    default ImmutableList<OgcApiQueryParameter> getQueryParameters(ExtensionRegistry extensionRegistry, OgcApiDataV2 apiData, String definitionPath) {
        return getQueryParameters(extensionRegistry, apiData, definitionPath, HttpMethods.GET);
    }

    default ImmutableList<OgcApiQueryParameter> getQueryParameters(ExtensionRegistry extensionRegistry, OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return extensionRegistry.getExtensionsForType(OgcApiQueryParameter.class)
                .stream()
                .filter(param -> param.isApplicable(apiData, definitionPath, method))
                .sorted(Comparator.comparing(ParameterExtension::getName))
                .collect(ImmutableList.toImmutableList());
    }

    default void checkAuthorization(OgcApiDataV2 apiData, Optional<User> optionalUser) {
        if (apiData.getSecured() && !optionalUser.isPresent()) {
            throw new NotAuthorizedException("Bearer realm=\"ldproxy\"");
            //throw new ClientErrorException(Response.Status.UNAUTHORIZED);
        }
    }

    default void checkPathParameter(ExtensionRegistry extensionRegistry, OgcApiDataV2 apiData, String definitionPath, String parameterName, String parameterValue) {
        getPathParameters(extensionRegistry, apiData, definitionPath).stream()
                .filter(param -> param.getName().equalsIgnoreCase(parameterName))
                .map(param -> param.validate(apiData, Optional.empty(), ImmutableList.of(parameterValue)).orElse(null))
                .filter(Objects::nonNull)
                .anyMatch(message -> {
                    // unknown value, return 404
                    throw new NotFoundException(message);
                });
    }
}
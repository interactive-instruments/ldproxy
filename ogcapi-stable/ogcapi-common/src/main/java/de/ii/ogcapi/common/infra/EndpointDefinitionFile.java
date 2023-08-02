/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.infra;

import static de.ii.ogcapi.foundation.domain.ApiSecurity.SCOPE_PUBLIC_READ;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.common.app.ImmutableQueryInputFile;
import de.ii.ogcapi.common.app.QueriesHandlerCommonImpl;
import de.ii.ogcapi.common.app.QueriesHandlerCommonImpl.Query;
import de.ii.ogcapi.common.domain.ApiDefinitionAuxiliaryFormatExtension;
import de.ii.ogcapi.common.domain.CommonConfiguration;
import de.ii.ogcapi.common.domain.QueriesHandlerCommon;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.xtraplatform.auth.domain.User;
import io.dropwizard.auth.Auth;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @title Auxiliary API Definition File
 * @path api/*
 * @langEn Provides auxiliary files referenced from the OpenAPI definition.
 * @langDe Stellt zusätzliche Dateien zur OpenAPI-Definition bereit.
 * @ref:formats {@link de.ii.ogcapi.common.domain.ApiDefinitionAuxiliaryFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointDefinitionFile extends Endpoint {

  private final QueriesHandlerCommon queryHandler;

  @Inject
  public EndpointDefinitionFile(
      ExtensionRegistry extensionRegistry, QueriesHandlerCommon queryHandler) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return CommonConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getResourceFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(ApiDefinitionAuxiliaryFormatExtension.class);
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("api")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_API_DEFINITION);

    String operationSummary = "support files for the API definition in HTML";
    List<OgcApiPathParameter> pathParameters =
        getPathParameters(extensionRegistry, apiData, "/api/{resource}");
    String path = "/api/{resource}";
    ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder2 =
        new ImmutableOgcApiResourceAuxiliary.Builder().path(path).pathParameters(pathParameters);
    ApiOperation.getResource(
            apiData,
            path,
            false,
            ImmutableList.of(),
            ImmutableList.of(),
            getResponseContent(apiData),
            operationSummary,
            Optional.empty(),
            Optional.empty(),
            getOperationId("getApiDefinitionSubResource"),
            SCOPE_PUBLIC_READ,
            ImmutableList.of())
        .ifPresent(operation -> resourceBuilder2.putOperations("GET", operation));
    definitionBuilder.putResources(path, resourceBuilder2.build());

    return definitionBuilder.build();
  }

  @GET
  @Path("/{file}")
  public Response getApiDefinition(
      @Auth Optional<User> optionalUser,
      @Context OgcApi api,
      @Context ApiRequestContext ogcApiContext,
      @PathParam("file") Optional<String> file) {

    QueriesHandlerCommonImpl.QueryInputFile queryInputApiDefinition =
        new ImmutableQueryInputFile.Builder()
            .from(getGenericQueryInput(api.getData()))
            .subPath(file)
            .build();

    return queryHandler.handle(Query.API_DEFINITION_FILE, queryInputApiDefinition, ogcApiContext);
  }
}

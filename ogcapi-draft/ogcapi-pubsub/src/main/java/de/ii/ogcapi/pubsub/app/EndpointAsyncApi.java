/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.app;

import static de.ii.ogcapi.foundation.domain.ApiSecurity.GROUP_DISCOVER_READ;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiExtensionHealth;
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
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SpecificationMaturity;
import de.ii.ogcapi.pubsub.domain.AsyncApiDefinitionFormatExtension;
import de.ii.ogcapi.pubsub.domain.QueriesHandlerPubSub;
import de.ii.ogcapi.pubsub.domain.QueriesHandlerPubSub.Query;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.base.domain.resiliency.Volatile2;
import io.dropwizard.auth.Auth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @title AsyncAPI Definition
 * @path asyncapi
 * @langEn Provides the AsyncAPI definition.
 * @langDe Stellt die AsyncAPI-Definition bereit.
 * @ref:formats {@link de.ii.ogcapi.pubsub.domain.AsyncApiDefinitionFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointAsyncApi extends Endpoint implements ApiExtensionHealth {

  private static final List<String> TAGS = ImmutableList.of("Capabilities");

  private final QueriesHandlerPubSub queryHandler;

  @Inject
  public EndpointAsyncApi(ExtensionRegistry extensionRegistry, QueriesHandlerPubSub queryHandler) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return PubSubConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getResourceFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(AsyncApiDefinitionFormatExtension.class);
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("asyncapi")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_ASYNC_API_DEFINITION);
    List<OgcApiQueryParameter> queryParameters =
        getQueryParameters(extensionRegistry, apiData, "/asyncapi");
    String operationSummary = "AsyncAPI definition";
    String path = "/asyncapi";
    ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder1 =
        new ImmutableOgcApiResourceAuxiliary.Builder().path(path);
    ApiOperation.getResource(
            apiData,
            path,
            false,
            queryParameters,
            ImmutableList.of(),
            getResponseContent(apiData),
            operationSummary,
            Optional.empty(),
            Optional.empty(),
            getOperationId("getAsyncApiDefinition"),
            GROUP_DISCOVER_READ,
            TAGS,
            Optional.of(SpecificationMaturity.DRAFT_LDPROXY),
            Optional.empty())
        .ifPresent(operation -> resourceBuilder1.putOperations("GET", operation));
    definitionBuilder.putResources(path, resourceBuilder1.build());

    return definitionBuilder.build();
  }

  @GET
  public Response getApiDefinition(
      @Auth Optional<User> optionalUser,
      @Context OgcApi api,
      @Context ApiRequestContext ogcApiContext) {

    return queryHandler.handle(
        Query.ASYNC_API_DEFINITION, getGenericQueryInput(api.getData()), ogcApiContext);
  }

  @Override
  public Set<Volatile2> getVolatiles(OgcApiDataV2 apiData) {
    return Set.of(queryHandler);
  }
}

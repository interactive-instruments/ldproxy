/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.common.app.ImmutableQueryInputLandingPage.Builder;
import de.ii.ogcapi.common.app.QueriesHandlerCommonImpl.Query;
import de.ii.ogcapi.common.app.QueriesHandlerCommonImpl.QueryInputLandingPage;
import de.ii.ogcapi.common.domain.CommonConfiguration;
import de.ii.ogcapi.common.domain.CommonFormatExtension;
import de.ii.ogcapi.common.domain.QueriesHandlerCommon;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.FoundationValidator;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import io.dropwizard.auth.Auth;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @langEn The landing page provides links to the API definition (link relations `service-desc` and
 *     `service-doc`), the Conformance declaration (path `/conformance`, link relation
 *     `conformance`), and other resources in the API.
 * @langDe TODO
 * @name Landing Page
 * @path /{apiId}/
 * @formats {@link de.ii.ogcapi.common.domain.CommonFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointLandingPage extends Endpoint implements ConformanceClass {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointLandingPage.class);
  private static final List<String> TAGS = ImmutableList.of("Capabilities");

  private final QueriesHandlerCommon queryHandler;

  @Inject
  public EndpointLandingPage(
      ExtensionRegistry extensionRegistry, QueriesHandlerCommon queryHandler) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return CommonConfiguration.class;
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    ValidationResult result = super.onStartup(api, apiValidation);

    if (apiValidation == MODE.NONE) {
      return result;
    }

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().from(result).mode(apiValidation);

    Optional<CommonConfiguration> config = api.getData().getExtension(CommonConfiguration.class);
    if (config.isPresent()) {
      FoundationValidator.validateLinks(builder, config.get().getAdditionalLinks(), "/");
    }

    return builder.build();
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null) {
      formats = extensionRegistry.getExtensionsForType(CommonFormatExtension.class);
    }
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_LANDING_PAGE);
    List<OgcApiQueryParameter> queryParameters =
        getQueryParameters(extensionRegistry, apiData, "/");
    String operationSummary = "landing page";
    Optional<String> operationDescription =
        Optional.of(
            "The landing page provides links to the API definition "
                + "(link relations `service-desc` and `service-doc`), the Conformance declaration (path `/conformance`, "
                + "link relation `conformance`), and other resources in the API.");
    String path = "/";
    ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder =
        new ImmutableOgcApiResourceAuxiliary.Builder().path(path);
    ApiOperation.getResource(
            apiData,
            path,
            false,
            queryParameters,
            ImmutableList.of(),
            getContent(apiData, path),
            operationSummary,
            operationDescription,
            Optional.empty(),
            getOperationId("getLandingPage"),
            TAGS)
        .ifPresent(operation -> resourceBuilder.putOperations("GET", operation));
    definitionBuilder.putResources(path, resourceBuilder.build());
    return definitionBuilder.build();
  }

  @GET
  public Response getLandingPage(
      @Auth Optional<User> optionalUser,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext) {

    List<Link> additionalLinks =
        api.getData()
            .getExtension(CommonConfiguration.class)
            .map(CommonConfiguration::getAdditionalLinks)
            .orElse(ImmutableList.of());

    QueryInputLandingPage queryInput =
        new Builder()
            .from(getGenericQueryInput(api.getData()))
            .additionalLinks(additionalLinks)
            .build();

    return queryHandler.handle(Query.LANDING_PAGE, queryInput, requestContext);
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of("http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/core");
  }
}

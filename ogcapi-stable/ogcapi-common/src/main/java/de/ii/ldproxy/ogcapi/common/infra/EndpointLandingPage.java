/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.common.app.ImmutableQueryInputLandingPage;
import de.ii.ldproxy.ogcapi.common.app.QueriesHandlerCommonImpl;
import de.ii.ldproxy.ogcapi.common.app.QueriesHandlerCommonImpl.Query;
import de.ii.ldproxy.ogcapi.common.domain.CommonConfiguration;
import de.ii.ldproxy.ogcapi.common.domain.CommonFormatExtension;
import de.ii.ldproxy.ogcapi.common.domain.QueriesHandlerCommon;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.foundation.domain.Endpoint;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.FoundationValidator;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ldproxy.ogcapi.foundation.domain.Link;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiQueryParameter;
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


@Singleton
@AutoBind
public class EndpointLandingPage extends Endpoint implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointLandingPage.class);
    private static final List<String> TAGS = ImmutableList.of("Capabilities");

    private final QueriesHandlerCommon queryHandler;

    @Inject
    public EndpointLandingPage(ExtensionRegistry extensionRegistry,
                               QueriesHandlerCommon queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CommonConfiguration.class;
    }

    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, MODE apiValidation) {
        ValidationResult result = super.onStartup(apiData, apiValidation);

        if (apiValidation== MODE.NONE)
            return result;

        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
                .from(result)
                .mode(apiValidation);

        Optional<CommonConfiguration> config = apiData.getExtension(CommonConfiguration.class);
        if (config.isPresent()) {
            builder = FoundationValidator.validateLinks(builder, config.get().getAdditionalLinks(), "/");
        }

        return builder.build();
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(CommonFormatExtension.class);
        return formats;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_LANDING_PAGE);
        List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, "/");
        String operationSummary = "landing page";
        Optional<String> operationDescription = Optional.of("The landing page provides links to the API definition " +
                "(link relations `service-desc` and `service-doc`), the Conformance declaration (path `/conformance`, " +
                "link relation `conformance`), and other resources in the API.");
        String path = "/";
        ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                .path(path);
        ApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
        if (operation!=null)
            resourceBuilder.putOperations("GET", operation);
        definitionBuilder.putResources(path, resourceBuilder.build());

        return definitionBuilder.build();
    }

    @GET
    public Response getLandingPage(@Auth Optional<User> optionalUser, @Context OgcApi api,
                                   @Context ApiRequestContext requestContext) {

        List<Link> additionalLinks = api.getData().getExtension(CommonConfiguration.class)
                                        .map(CommonConfiguration::getAdditionalLinks)
                                        .orElse(ImmutableList.of());

        QueriesHandlerCommonImpl.QueryInputLandingPage queryInput = new ImmutableQueryInputLandingPage.Builder()
                .from(getGenericQueryInput(api.getData()))
                .additionalLinks(additionalLinks)
                .build();

        return queryHandler.handle(Query.LANDING_PAGE, queryInput, requestContext);
    }

    @Override
    public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-common-1/0.0/conf/core");
    }
}

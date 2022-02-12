/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.infra;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ldproxy.ogcapi.routes.domain.HtmlForm;
import de.ii.ldproxy.ogcapi.routes.domain.HtmlFormDefaults;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableQueryInputComputeRoute;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableQueryInputRoutes;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRouteDefinition;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRouteDefinitionInfo;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRouteDefinitionInputs;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableWaypoints;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableWaypointsValue;
import de.ii.ldproxy.ogcapi.routes.domain.QueryHandlerRoutes;
import de.ii.ldproxy.ogcapi.routes.domain.RouteDefinition;
import de.ii.ldproxy.ogcapi.routes.domain.RouteFormatExtension;
import de.ii.ldproxy.ogcapi.routes.domain.Routes;
import de.ii.ldproxy.ogcapi.routes.domain.RoutesFormatExtension;
import de.ii.ldproxy.ogcapi.routes.domain.RoutingConfiguration;
import de.ii.ldproxy.ogcapi.routes.domain.RoutingFlag;
import de.ii.ldproxy.ogcapi.routes.domain.WaypointsValue;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.routes.sql.domain.RoutesConfiguration;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.routes.app.CapabilityRouting.CORE;
import static de.ii.ldproxy.ogcapi.routes.app.CapabilityRouting.MODE;

/**
 * computes routes
 */
@Component
@Provides
@Instantiate
public class EndpointRoutesGet extends Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointRoutesGet.class);
    private static final List<String> TAGS = ImmutableList.of("Routing");

    private final QueryHandlerRoutes queryHandler;
    private final FeaturesCoreProviders providers;

    public EndpointRoutesGet(@Requires ExtensionRegistry extensionRegistry,
                             @Requires QueryHandlerRoutes queryHandler,
                             @Requires FeaturesCoreProviders providers) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
        this.providers = providers;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<RoutingConfiguration> extension = apiData.getExtension(RoutingConfiguration.class);

        return extension.filter(RoutingConfiguration::isEnabled).isPresent() &&
                (extension.map(RoutingConfiguration::getHtml).map(HtmlForm::getEnabled).orElse(false) ||
                 extension.map(RoutingConfiguration::getManageRoutes).orElse(false));
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return RoutingConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(RoutesFormatExtension.class);
        return formats;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        Optional<RoutingConfiguration> config = apiData.getExtension(RoutingConfiguration.class);
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("routes")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_ROUTES_GET);
        String path = "/routes";
        HttpMethods method = HttpMethods.GET;
        List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path, method);
        List<ApiHeader> headers = getHeaders(extensionRegistry, apiData, path, method);
        String operationSummary = config.map(RoutingConfiguration::getManageRoutes).orElse(false)
            ? "fetch the list of routes"
            : (config.map(RoutingConfiguration::getHtml).map(HtmlForm::getEnabled).orElse(false) ? "provide a HTML form to compute a route" : "");
        Optional<String> operationDescription = Optional.of(
            (config.map(RoutingConfiguration::getManageRoutes).orElse(false) ? "This operation returns a list of routes that are currently available on the server. " : "") +
            (config.map(RoutingConfiguration::getHtml).map(HtmlForm::getEnabled).orElse(false) ? "The HTML representation also includes a HTML form to compute a route." : "")
        );
        ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
            .path(path);
        Map<MediaType, ApiMediaTypeContent> responseContent = getContent(apiData, "/routes", method);
        ApiOperation operation = addOperation(apiData, method, responseContent, queryParameters, headers, path, operationSummary, operationDescription, Optional.empty(), TAGS);
        if (operation!=null)
            resourceBuilder.putOperations(method.toString(), operation);

        definitionBuilder.putResources(path, resourceBuilder.build());

        return definitionBuilder.build();
    }

    /**
     * lists all stored routes and prepares the necessary information to create a form for requesting a new route
     *
     * @return a route according to the RouteExchangeModel
     */
    @Path("/")
    @GET
    public Response getRoutes(@Auth Optional<User> optionalUser,
                            @Context OgcApi api,
                            @Context ApiRequestContext requestContext,
                            @Context UriInfo uriInfo,
                            @Context HttpServletRequest request) {

        OgcApiDataV2 apiData = api.getData();
        checkAuthorization(apiData, optionalUser);

        FeatureProvider2 featureProvider = providers.getFeatureProviderOrThrow(api.getData());
        ensureFeatureProviderSupportsRouting(featureProvider);

        Map<String, String> preferences = featureProvider.getData().getExtension(RoutesConfiguration.class)
            .map(RoutesConfiguration::getPreferences)
            .map(map -> map.entrySet()
                .stream()
                .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue().getLabel()))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(ImmutableMap.of());

        Map<String, String> modes = featureProvider.getData().getExtension(RoutesConfiguration.class)
            .map(RoutesConfiguration::getModes)
            .map(map -> map.entrySet()
                .stream()
                .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(ImmutableMap.of());

        Map<String, RoutingFlag> additionalFlags = apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getAdditionalFlags)
            .map(map -> map.entrySet()
                .stream()
                .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(ImmutableMap.of());

        QueryHandlerRoutes.QueryInputRoutes queryInput = new ImmutableQueryInputRoutes.Builder()
            .templateInfo(new ImmutableRouteDefinitionInfo.Builder()
                              .preferences(preferences)
                              .defaultPreference(apiData.getExtension(RoutingConfiguration.class)
                                                     .map(RoutingConfiguration::getDefaultPreference)
                                                     .orElse(preferences.keySet()
                                                                 .stream()
                                                                 .findFirst()
                                                                 .orElseThrow()))
                              .modes(modes)
                              .defaultMode(apiData.getExtension(RoutingConfiguration.class)
                                               .map(RoutingConfiguration::getDefaultMode)
                                               .orElse(preferences.keySet()
                                                           .stream()
                                                           .findFirst()
                                                           .orElseThrow()))
                              .additionalFlags(additionalFlags)
                              .build())
            .build();
        return queryHandler.handle(QueryHandlerRoutes.Query.GET_ROUTES, queryInput, requestContext);
    }

    private static void ensureFeatureProviderSupportsRouting(FeatureProvider2 featureProvider) {
        if (!featureProvider.supportsQueries()) {
            throw new IllegalStateException("Feature provider does not support queries.");
        }
        featureProvider.getData().getExtension(RoutesConfiguration.class)
            .orElseThrow(() -> new IllegalStateException("Feature provider does not support routing."));
    }
}

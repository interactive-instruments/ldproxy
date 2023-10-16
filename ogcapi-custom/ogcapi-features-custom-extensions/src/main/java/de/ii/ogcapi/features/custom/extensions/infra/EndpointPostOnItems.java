/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.custom.extensions.infra;

import static de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.GROUP_DATA_READ;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ogcapi.features.core.domain.ImmutableQueryInputFeatures;
import de.ii.ogcapi.features.custom.extensions.app.FeaturesExtensionsBuildingBlock;
import de.ii.ogcapi.features.custom.extensions.domain.FeaturesExtensionsConfiguration;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.EndpointExtension;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiResource;
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @title Features
 * @path collections/{collectionId}/items
 * @langEn The difference to calling with GET is that the query parameters are passed as content in
 *     the request.
 * @langDe Der Unterschied zum Aufruf mit GET ist, dass die Query-Parameter als Content im Aufruf
 *     übergeben werden.
 * @ref:formats {@link de.ii.ogcapi.features.core.domain.FeatureFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointPostOnItems extends EndpointSubCollection {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointPostOnItems.class);
  private static final List<String> TAGS = ImmutableList.of("Access data");

  private final FeaturesCoreProviders providers;
  private final FeaturesQuery ogcApiFeaturesQuery;
  private final FeaturesCoreQueriesHandler queryHandler;

  @Inject
  public EndpointPostOnItems(
      ExtensionRegistry extensionRegistry,
      FeaturesCoreProviders providers,
      FeaturesQuery ogcApiFeaturesQuery,
      FeaturesCoreQueriesHandler queryHandler) {
    super(extensionRegistry);
    this.providers = providers;
    this.ogcApiFeaturesQuery = ogcApiFeaturesQuery;
    this.queryHandler = queryHandler;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return FeaturesExtensionsConfiguration.class;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return super.isEnabledForApi(apiData, collectionId)
        && apiData.getCollections().get(collectionId).getEnabled()
        && apiData
            .getExtension(FeaturesExtensionsConfiguration.class, collectionId)
            .filter(ExtensionConfiguration::isEnabled)
            .filter(FeaturesExtensionsConfiguration::shouldSupportPostOnItems)
            .isPresent();
  }

  @Override
  public List<? extends FormatExtension> getResourceFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(FeatureFormatExtension.class);
    return formats;
  }

  public Map<MediaType, ApiMediaTypeContent> getFeatureContent(
      List<? extends FormatExtension> formats,
      OgcApiDataV2 apiData,
      Optional<String> collectionId,
      boolean featureCollection) {
    return formats.stream()
        .filter(f -> f instanceof FeatureFormatExtension)
        .map(f -> (FeatureFormatExtension) f)
        .filter(
            f ->
                collectionId
                    .map(s -> f.isEnabledForApi(apiData, s))
                    .orElseGet(() -> f.isEnabledForApi(apiData)))
        .map(f -> f.getFeatureContent(apiData, collectionId, featureCollection))
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(), c -> c));
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ApiEndpointDefinition endpointFeaturesDefinition =
        extensionRegistry.getExtensionsForType(EndpointExtension.class).stream()
            .filter(endpoint -> "EndpointFeatures".equals(endpoint.getClass().getSimpleName()))
            .map(endpoint -> endpoint.getDefinition(apiData))
            .findFirst()
            .orElse(null);

    if (Objects.isNull(endpointFeaturesDefinition)) return null;

    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint(endpointFeaturesDefinition.getApiEntrypoint())
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_FEATURES_EXTENSIONS);
    endpointFeaturesDefinition.getResources().entrySet().stream()
        .filter(
            entry ->
                entry.getKey().equals("/collections/{collectionId}/items")
                    || entry
                        .getKey()
                        .matches(
                            "/collections/"
                                + AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN
                                + "/items"))
        .forEach(
            entry -> {
              OgcApiResource resource = entry.getValue();
              ImmutableOgcApiResourceData.Builder builder =
                  new ImmutableOgcApiResourceData.Builder()
                      .path(resource.getPath())
                      .pathParameters(resource.getPathParameters());
              ApiOperation get = resource.getOperations().get("GET");
              if (Objects.nonNull(get)) {
                String collectionId =
                    entry.getKey().replace("/collections/", "").replace("/items", "");
                Map<MediaType, ApiMediaTypeContent> responseContent =
                    getFeatureContent(
                        getResourceFormats(),
                        apiData,
                        collectionId.startsWith("{") ? Optional.empty() : Optional.of(collectionId),
                        true);
                ApiOperation.getResource(
                        apiData,
                        entry.getKey(),
                        true,
                        get.getQueryParameters(),
                        get.getHeaders(),
                        responseContent,
                        get.getSummary(),
                        get.getDescription(),
                        Optional.empty(),
                        getOperationId("queryItems", collectionId),
                        GROUP_DATA_READ,
                        TAGS,
                        FeaturesExtensionsBuildingBlock.MATURITY,
                        FeaturesExtensionsBuildingBlock.SPEC)
                    .ifPresent(operation -> builder.putOperations("POST", operation));
                definitionBuilder.putResources(entry.getKey(), builder.build());
              }
            });

    return definitionBuilder.build();
  }

  @POST
  @Path("/{collectionId}/items")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response getItems(
      @Auth Optional<User> optionalUser,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @PathParam("collectionId") String collectionId,
      @RequestBody MultivaluedMap<String, String> parameters) {
    checkCollectionExists(api.getData(), collectionId);

    FeatureTypeConfigurationOgcApi collectionData =
        api.getData().getCollections().get(collectionId);

    FeaturesCoreConfiguration coreConfiguration =
        collectionData
            .getExtension(FeaturesCoreConfiguration.class)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        MessageFormat.format(
                            "Features are not supported in API ''{0}'', collection ''{1}''.",
                            api.getId(), collectionId)));

    int defaultPageSize = coreConfiguration.getDefaultPageSize();
    boolean showsFeatureSelfLink = coreConfiguration.getShowsFeatureSelfLink();

    QueryParameterSet queryParameterSet = requestContext.getQueryParameterSet();

    FeatureQuery query =
        ogcApiFeaturesQuery.requestToFeatureQuery(
            api.getData(),
            collectionData,
            coreConfiguration.getDefaultEpsgCrs(),
            coreConfiguration.getCoordinatePrecision(),
            defaultPageSize,
            queryParameterSet);

    FeaturesCoreQueriesHandler.QueryInputFeatures queryInput =
        new ImmutableQueryInputFeatures.Builder()
            .from(getGenericQueryInput(api.getData()))
            .collectionId(collectionId)
            .query(query)
            .featureProvider(providers.getFeatureProviderOrThrow(api.getData(), collectionData))
            .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
            .defaultPageSize(Optional.of(defaultPageSize))
            .showsFeatureSelfLink(showsFeatureSelfLink)
            .build();

    return queryHandler.handle(
        FeaturesCoreQueriesHandler.Query.FEATURES, queryInput, requestContext);
  }
}

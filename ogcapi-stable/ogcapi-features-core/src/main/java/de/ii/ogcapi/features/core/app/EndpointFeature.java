/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ogcapi.features.core.domain.ImmutableQueryInputFeature;
import de.ii.ogcapi.features.core.domain.SchemaGeneratorOpenApi;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.ParameterExtension;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import de.ii.xtraplatform.web.domain.ETag.Type;
import io.dropwizard.auth.Auth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @title Feature
 * @path collections/{collectionId}/items/{featureId}
 * @langEn The response is a document representing the feature with the requested feature
 *     identifier.
 * @langDe Die Antwort ist ein Dokument, welches das Feature mit dem angeforderten Identifikator
 *     repräsentiert.
 * @ref:formats {@link de.ii.ogcapi.features.core.domain.FeatureFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointFeature extends EndpointFeaturesDefinition {

  private final FeaturesQuery ogcApiFeaturesQuery;
  private final FeaturesCoreQueriesHandler queryHandler;

  @Inject
  public EndpointFeature(
      ExtensionRegistry extensionRegistry,
      FeaturesCoreProviders providers,
      FeaturesQuery ogcApiFeaturesQuery,
      FeaturesCoreQueriesHandler queryHandler,
      SchemaGeneratorOpenApi schemaGeneratorFeature,
      SchemaValidator schemaValidator) {
    super(extensionRegistry, schemaGeneratorFeature, providers, schemaValidator);
    this.ogcApiFeaturesQuery = ogcApiFeaturesQuery;
    this.queryHandler = queryHandler;
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    // EndpointFeatures validates the configuration
    return super.onStartup(api, apiValidation);
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("collections")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_FEATURES);
    ImmutableList<OgcApiQueryParameter> allQueryParameters =
        extensionRegistry.getExtensionsForType(OgcApiQueryParameter.class).stream()
            .sorted(Comparator.comparing(ParameterExtension::getName))
            .collect(ImmutableList.toImmutableList());

    generateDefinition(
        apiData,
        definitionBuilder,
        allQueryParameters,
        "/items/{featureId}",
        "retrieve a feature in the feature collection '",
        "Fetch the feature with id `{featureId}`.",
        "FEATURE");

    return definitionBuilder.build();
  }

  @GET
  @Path("/{collectionId}/items/{featureId}")
  public Response getItem(
      @Auth Optional<User> optionalUser,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context UriInfo uriInfo,
      @PathParam("collectionId") String collectionId,
      @PathParam("featureId") String featureId) {
    checkCollectionExists(api.getData(), collectionId);

    FeatureTypeConfigurationOgcApi collectionData =
        api.getData().getCollections().get(collectionId);

    FeaturesCoreConfiguration coreConfiguration =
        collectionData
            .getExtension(FeaturesCoreConfiguration.class)
            .filter(ExtensionConfiguration::isEnabled)
            .filter(
                cfg ->
                    cfg.getItemType().orElse(FeaturesCoreConfiguration.ItemType.feature)
                        != FeaturesCoreConfiguration.ItemType.unknown)
            .orElseThrow(() -> new NotFoundException("Features are not supported for this API."));

    List<OgcApiQueryParameter> allowedParameters =
        getQueryParameters(
            extensionRegistry,
            api.getData(),
            "/collections/{collectionId}/items/{featureId}",
            collectionId);
    FeatureQuery query =
        ogcApiFeaturesQuery.requestToFeatureQuery(
            api.getData(),
            collectionData,
            coreConfiguration.getDefaultEpsgCrs(),
            coreConfiguration.getCoordinatePrecision(),
            toFlatMap(uriInfo.getQueryParameters()),
            allowedParameters,
            featureId,
            Optional.of(Type.STRONG));

    ImmutableQueryInputFeature.Builder queryInputBuilder =
        new ImmutableQueryInputFeature.Builder()
            .from(getGenericQueryInput(api.getData()))
            .collectionId(collectionId)
            .featureId(featureId)
            .query(query)
            .featureProvider(providers.getFeatureProviderOrThrow(api.getData(), collectionData))
            .defaultCrs(coreConfiguration.getDefaultEpsgCrs());

    if (Objects.nonNull(coreConfiguration.getCaching())
        && Objects.nonNull(coreConfiguration.getCaching().getCacheControlItems()))
      queryInputBuilder.cacheControl(coreConfiguration.getCaching().getCacheControlItems());

    return queryHandler.handle(
        FeaturesCoreQueriesHandler.Query.FEATURE, queryInputBuilder.build(), requestContext);
  }
}

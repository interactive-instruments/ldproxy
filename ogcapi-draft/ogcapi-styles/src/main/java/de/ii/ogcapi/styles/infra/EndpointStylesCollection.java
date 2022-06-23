/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceSet;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.styles.domain.ImmutableQueryInputStyles.Builder;
import de.ii.ogcapi.styles.domain.QueriesHandlerStyles;
import de.ii.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ogcapi.styles.domain.StylesFormatExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** fetch list of styles associated with a collection */
@Singleton
@AutoBind
public class EndpointStylesCollection extends EndpointSubCollection implements ConformanceClass {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStylesCollection.class);

  private static final List<String> TAGS = ImmutableList.of("Discover and fetch styles");

  private final QueriesHandlerStyles queryHandler;

  @Inject
  public EndpointStylesCollection(
      ExtensionRegistry extensionRegistry, QueriesHandlerStyles queryHandler) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of("http://www.opengis.net/spec/ogcapi-styles-1/0.0/conf/core");
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return StylesConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(StylesFormatExtension.class);
    return formats;
  }

  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("collections")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_STYLES_COLLECTION);
    final String subSubPath = "/styles";
    final String path = "/collections/{collectionId}" + subSubPath;
    final List<OgcApiPathParameter> pathParameters =
        getPathParameters(extensionRegistry, apiData, path);
    final Optional<OgcApiPathParameter> optCollectionIdParam =
        pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
    if (!optCollectionIdParam.isPresent()) {
      LOGGER.error(
          "Path parameter 'collectionId' missing for resource at path '"
              + path
              + "'. The GET method will not be available.");
    } else {
      final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
      boolean explode = collectionIdParam.isExplodeInOpenApi(apiData);
      final List<String> collectionIds =
          (explode) ? collectionIdParam.getValues(apiData) : ImmutableList.of("{collectionId}");
      for (String collectionId : collectionIds) {
        List<OgcApiQueryParameter> queryParameters =
            getQueryParameters(extensionRegistry, apiData, path, collectionId);
        String operationSummary =
            "retrieve a list of the available styles for collection `" + collectionId + "`";
        Optional<String> operationDescription =
            Optional.of(
                "This operation fetches the set of styles available for this collection. "
                    + "For each style the id, a title, links to the stylesheet of the style in each supported encoding, "
                    + "and the link to the metadata is provided.");
        String resourcePath = path.replace("{collectionId}", collectionId);
        ImmutableOgcApiResourceSet.Builder resourceBuilder =
            new ImmutableOgcApiResourceSet.Builder()
                .path(resourcePath)
                .pathParameters(pathParameters)
                .subResourceType("Style");
        Map<MediaType, ApiMediaTypeContent> responseContent =
            collectionId.startsWith("{")
                ? getContent(apiData, Optional.empty(), subSubPath, HttpMethods.GET)
                : getContent(apiData, Optional.of(collectionId), subSubPath, HttpMethods.GET);
        ApiOperation.getResource(
                apiData,
                resourcePath,
                false,
                queryParameters,
                ImmutableList.of(),
                responseContent,
                operationSummary,
                operationDescription,
                Optional.empty(),
                TAGS)
            .ifPresent(
                operation -> resourceBuilder.putOperations(HttpMethods.GET.name(), operation));
        definitionBuilder.putResources(resourcePath, resourceBuilder.build());
      }
    }

    return definitionBuilder.build();
  }

  /**
   * fetch all available styles for the service
   *
   * @return all styles in a JSON styles object or an HTML page
   */
  @Path("/{collectionId}/styles")
  @GET
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
  public Response getStyles(
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @PathParam("collectionId") String collectionId) {
    OgcApiDataV2 apiData = api.getData();
    checkPathParameter(
        extensionRegistry,
        apiData,
        "/collections/{collectionId}/styles",
        "collectionId",
        collectionId);
    checkCollectionExists(apiData, collectionId);

    QueriesHandlerStyles.QueryInputStyles queryInput =
        new Builder().from(getGenericQueryInput(api.getData())).collectionId(collectionId).build();

    return queryHandler.handle(QueriesHandlerStyles.Query.STYLES, queryInput, requestContext);
  }
}

/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.crs.domain.CrsSupport;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiHeader;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.html.domain.MapClient.Type;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.services.domain.ServicesContext;
import io.dropwizard.auth.Auth;
import io.dropwizard.views.View;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @langEn TODO
 * @langDe TODO
 * @name CRUD Editor
 * @path {apiId}/collection/{collectionId}/crud
 */
@Singleton
@AutoBind
public class EndpointEditor extends EndpointSubCollection {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointEditor.class);
  private static final List<String> TAGS = ImmutableList.of("Mutate data");

  private final URI servicesUri;
  private final CrsSupport crsSupport;

  @Inject
  public EndpointEditor(
      ExtensionRegistry extensionRegistry, ServicesContext servicesContext, CrsSupport crsSupport) {
    super(extensionRegistry);
    this.servicesUri = servicesContext.getUri();
    this.crsSupport = crsSupport;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return CrudConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null)
      formats =
          extensionRegistry.getExtensionsForType(FormatExtension.class).stream()
              .filter(ext -> ext instanceof CrudFormatHtml)
              .collect(Collectors.toList());
    return formats;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    // TODO the editor client currently requires support for EPSG:25832
    Optional<FeatureTypeConfigurationOgcApi> optCollectionData =
        apiData.getCollectionData(collectionId);
    if (optCollectionData.isEmpty()
        || !crsSupport.isSupported(apiData, optCollectionData.get(), EpsgCrs.of(25832))) {
      return false;
    }
    return super.isEnabledForApi(apiData, collectionId);
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    Optional<CrudConfiguration> config = apiData.getExtension(CrudConfiguration.class);
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("collections")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_CRUD);
    String path = "/collections/{collectionId}/crud";
    HttpMethods method = HttpMethods.GET;
    List<OgcApiPathParameter> pathParameters =
        getPathParameters(extensionRegistry, apiData, "/collections/{collectionId}");
    List<OgcApiQueryParameter> queryParameters = List.of();
    List<ApiHeader> headers = List.of();
    String operationSummary = "map client with feature editor";
    Optional<String> operationDescription = Optional.empty();
    ImmutableOgcApiResourceData.Builder resourceBuilder =
        new ImmutableOgcApiResourceData.Builder().path(path).pathParameters(pathParameters);
    ApiOperation.getResource(
            apiData,
            path,
            false,
            queryParameters,
            headers,
            getContent(apiData, path),
            operationSummary,
            operationDescription,
            Optional.empty(),
            TAGS)
        .ifPresent(operation -> resourceBuilder.putOperations(method.name(), operation));
    definitionBuilder.putResources(path, resourceBuilder.build());

    return definitionBuilder.build();
  }

  @Path("/{collectionId}/crud")
  @GET
  @Produces("text/html")
  public View getEditor(
      @Auth Optional<User> optionalUser,
      @PathParam("collectionId") String collectionId,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context HttpServletRequest request) {

    checkAuthorization(api.getData(), optionalUser);

    Optional<HtmlConfiguration> htmlConfig =
        api.getData().getExtension(HtmlConfiguration.class, collectionId);

    MapClient.Type mapClientType = Type.OPEN_LAYERS;

    String serviceUrl =
        new URICustomizer(servicesUri)
            .ensureLastPathSegments(api.getData().getSubPath().toArray(String[]::new))
            .toString();

    String styleUrl =
        htmlConfig
            .map(cfg -> cfg.getStyle(Optional.empty(), Optional.of(collectionId), serviceUrl))
            .orElse(null);
    boolean removeZoomLevelConstraints = false;

    String attribution = "";

    return new EditorView(
        api.getData(),
        collectionId,
        api.getSpatialExtent(collectionId),
        URI.create(requestContext.getUriCustomizer().toString()),
        attribution,
        requestContext.getStaticUrlPrefix(),
        htmlConfig.orElseThrow(),
        mapClientType,
        styleUrl,
        removeZoomLevelConstraints);
  }
}

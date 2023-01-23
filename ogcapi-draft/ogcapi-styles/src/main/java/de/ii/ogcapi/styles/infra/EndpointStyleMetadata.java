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
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.styles.domain.ImmutableQueryInputStyle.Builder;
import de.ii.ogcapi.styles.domain.QueriesHandlerStyles;
import de.ii.ogcapi.styles.domain.StyleMetadataFormatExtension;
import de.ii.ogcapi.styles.domain.StylesConfiguration;
import java.util.List;
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

/**
 * @title Style Metadata
 * @path styles/{styleId}/metadata
 * @langEn Style metadata is essential information about a style in order to support users to
 *     discover and select styles for rendering their data and for visual style editors to create
 *     user interfaces for editing a style. This operations returns the metadata for the requested
 *     style as a single document. The stylesheet of the style will typically include some the
 *     metadata, too.
 * @langDe Style-Metadaten sind wichtige Informationen über einen Style, damit Benutzer Styles für
 *     die Darstellung ihrer Daten erkennen und auswählen können und visuelle Style-Editoren
 *     Benutzeroberflächen für die Bearbeitung eines Styles bereitstellen können. Diese Operation
 *     gibt die Metadaten für den angeforderten Style als ein einziges Dokument zurück. Das
 *     Stylesheet des Styles enthält in der Regel auch einige der Metadaten.
 * @ref:formats {@link de.ii.ogcapi.styles.domain.StyleMetadataFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointStyleMetadata extends Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStyleMetadata.class);

  protected static final List<String> TAGS = ImmutableList.of("Discover and fetch styles");

  private final QueriesHandlerStyles queryHandler;

  @Inject
  public EndpointStyleMetadata(
      ExtensionRegistry extensionRegistry, QueriesHandlerStyles queryHandler) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return StylesConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(StyleMetadataFormatExtension.class);
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("styles")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_STYLE_METADATA);
    String path = "/styles/{styleId}/metadata";
    ImmutableList<OgcApiQueryParameter> queryParameters =
        getQueryParameters(extensionRegistry, apiData, path);
    List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
    if (!pathParameters.stream()
        .filter(param -> param.getName().equals("styleId"))
        .findAny()
        .isPresent()) {
      LOGGER.error(
          "Path parameter 'styleId' missing for resource at path '"
              + path
              + "'. The GET method will not be available.");
    } else {
      String operationSummary = "fetch metadata about the style `{styleId}`";
      Optional<String> operationDescription =
          Optional.of(
              "Style metadata is essential information about a style in order to "
                  + "support users to discover and select styles for rendering their data and for visual style editors "
                  + "to create user interfaces for editing a style. This operations returns the metadata for the "
                  + "requested style as a single document. The stylesheet of the style will typically include some "
                  + "the metadata, too.");
      ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder =
          new ImmutableOgcApiResourceAuxiliary.Builder().path(path).pathParameters(pathParameters);
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
              getOperationId("getStyleMetadata"),
              TAGS)
          .ifPresent(operation -> resourceBuilder.putOperations("GET", operation));
      definitionBuilder.putResources(path, resourceBuilder.build());
    }

    return definitionBuilder.build();
  }

  /**
   * Fetch metadata for a style
   *
   * @param styleId the local identifier of a specific style
   * @return the style in a json file
   */
  @Path("/{styleId}/metadata")
  @GET
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
  public Response getStyleMetadata(
      @PathParam("styleId") String styleId,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext) {

    OgcApiDataV2 apiData = api.getData();
    checkPathParameter(
        extensionRegistry,
        apiData,
        "/collections/{collectionId}/styles/{styleId}/metadata",
        "styleId",
        styleId);

    QueriesHandlerStyles.QueryInputStyle queryInput =
        new Builder().from(getGenericQueryInput(api.getData())).styleId(styleId).build();

    return queryHandler.handle(
        QueriesHandlerStyles.Query.STYLE_METADATA, queryInput, requestContext);
  }
}

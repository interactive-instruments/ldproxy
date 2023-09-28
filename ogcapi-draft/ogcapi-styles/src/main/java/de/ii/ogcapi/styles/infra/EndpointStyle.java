/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.infra;

import static de.ii.ogcapi.styles.domain.QueriesHandlerStyles.GROUP_STYLES_READ;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.styles.app.StylesBuildingBlock;
import de.ii.ogcapi.styles.domain.ImmutableQueryInputStyle;
import de.ii.ogcapi.styles.domain.QueriesHandlerStyles;
import de.ii.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ogcapi.styles.domain.StyleRepository;
import de.ii.ogcapi.styles.domain.StylesConfiguration;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @title Style
 * @path styles/{styleId}
 * @langEn This operation fetches the list of styles. For each style the id, a title, links to the
 *     stylesheet of the style in each supported encoding, and the link to the metadata is provided.
 * @langDe Mit dieser Operation wird die Liste der Stylee abgerufen. Für jeden Style werden die ID,
 *     ein Titel, Links zum Stylesheet des Styles in jeder unterstützten Kodierung und der Link zu
 *     den Metadaten bereitgestellt.
 * @ref:formats {@link de.ii.ogcapi.styles.domain.StyleFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointStyle extends Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStyle.class);

  private static final List<String> TAGS = ImmutableList.of("Discover and fetch styles");

  private final StyleRepository styleRepository;
  private final QueriesHandlerStyles queryHandler;
  private final I18n i18n;

  @Inject
  public EndpointStyle(
      ExtensionRegistry extensionRegistry,
      StyleRepository styleRepository,
      I18n i18n,
      QueriesHandlerStyles queryHandler) {
    super(extensionRegistry);
    this.styleRepository = styleRepository;
    this.i18n = i18n;
    this.queryHandler = queryHandler;
  }

  private Stream<StyleFormatExtension> getStyleFormatStream(OgcApiDataV2 apiData) {
    return extensionRegistry.getExtensionsForType(StyleFormatExtension.class).stream()
        .filter(styleFormatExtension -> styleFormatExtension.isEnabledForApi(apiData));
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return StylesConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getResourceFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(StyleFormatExtension.class);
    return formats;
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    ValidationResult result = super.onStartup(api, apiValidation);

    if (apiValidation == MODE.NONE) return result;

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().from(result).mode(apiValidation);

    builder = styleRepository.validate(builder, api.getData(), Optional.empty());

    return builder.build();
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("styles")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_STYLESHEET);
    String path = "/styles/{styleId}";
    List<OgcApiQueryParameter> queryParameters =
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
      String operationSummary = "fetch a style";
      Optional<String> operationDescription =
          Optional.of(
              "Fetches the style with identifier `styleId`. "
                  + "The set of available styles can be retrieved at `/styles`. Not all styles are available in "
                  + "all style encodings.");
      ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder =
          new ImmutableOgcApiResourceAuxiliary.Builder().path(path).pathParameters(pathParameters);
      ApiOperation.getResource(
              apiData,
              path,
              false,
              queryParameters,
              ImmutableList.of(),
              getResponseContent(apiData),
              operationSummary,
              operationDescription,
              Optional.empty(),
              getOperationId("getStyle"),
              GROUP_STYLES_READ,
              TAGS,
              StylesBuildingBlock.MATURITY,
              StylesBuildingBlock.SPEC)
          .ifPresent(operation -> resourceBuilder.putOperations("GET", operation));
      definitionBuilder.putResources(path, resourceBuilder.build());
    }

    return definitionBuilder.build();
  }

  /**
   * Fetch a style by id
   *
   * @param styleId the local identifier of a specific style
   * @return the style in a json file
   */
  @Path("/{styleId}")
  @GET
  public Response getStyle(
      @PathParam("styleId") String styleId,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext) {

    OgcApiDataV2 apiData = api.getData();
    checkPathParameter(
        extensionRegistry,
        apiData,
        "/collections/{collectionId}/styles/{styleId}",
        "styleId",
        styleId);

    QueriesHandlerStyles.QueryInputStyle queryInput =
        new ImmutableQueryInputStyle.Builder()
            .from(getGenericQueryInput(api.getData()))
            .styleId(styleId)
            .build();

    return queryHandler.handle(QueriesHandlerStyles.Query.STYLE, queryInput, requestContext);
  }
}

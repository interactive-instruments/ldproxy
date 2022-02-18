/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.Endpoint;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.FoundationConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.I18n;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.styles.domain.StyleRepository;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableQueryInputStyle;
import de.ii.ldproxy.ogcapi.styles.domain.QueriesHandlerStyles;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * fetch the stylesheet of a style
 */
@Component
@Provides
@Instantiate
public class EndpointStyle extends Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStyle.class);

    private static final List<String> TAGS = ImmutableList.of("Discover and fetch styles");

    private final StyleRepository styleRepository;
    private final QueriesHandlerStyles queryHandler;
    private final I18n i18n;

    public EndpointStyle(@Requires ExtensionRegistry extensionRegistry,
                         @Requires StyleRepository styleRepository,
                         @Requires I18n i18n,
                         @Requires QueriesHandlerStyles queryHandler) {
        super(extensionRegistry);
        this.styleRepository = styleRepository;
        this.i18n = i18n;
        this.queryHandler = queryHandler;
    }

    private Stream<StyleFormatExtension> getStyleFormatStream(OgcApiDataV2 apiData) {
        return extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                .stream()
                .filter(styleFormatExtension -> styleFormatExtension.isEnabledForApi(apiData));
    }

    private List<ApiMediaType> getStylesheetMediaTypes(OgcApiDataV2 apiData, File apiDir, String styleId) {
        return getStyleFormatStream(apiData)
                .filter(styleFormat -> new File(apiDir + File.separator + styleId + "." + styleFormat.getFileExtension()).exists())
                .map(StyleFormatExtension::getMediaType)
                .collect(Collectors.toList());
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(StyleFormatExtension.class);
        return formats;
    }

    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, MODE apiValidation) {
        ValidationResult result = super.onStartup(apiData, apiValidation);

        if (apiValidation== MODE.NONE)
            return result;

        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
                .from(result)
                .mode(apiValidation);

        builder = styleRepository.validate(builder, apiData, Optional.empty());

        return builder.build();
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("styles")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_STYLESHEET);
        String path = "/styles/{styleId}";
        List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
        List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
        if (!pathParameters.stream().filter(param -> param.getName().equals("styleId")).findAny().isPresent()) {
            LOGGER.error("Path parameter 'styleId' missing for resource at path '" + path + "'. The GET method will not be available.");
        } else {
            String operationSummary = "fetch a style";
            Optional<String> operationDescription = Optional.of("Fetches the style with identifier `styleId`. " +
                    "The set of available styles can be retrieved at `/styles`. Not all styles are available in " +
                    "all style encodings.");
            ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                    .path(path)
                    .pathParameters(pathParameters);
            ApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilder.putOperations("GET", operation);
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
    public Response getStyle(@PathParam("styleId") String styleId, @Context OgcApi api,
                             @Context ApiRequestContext requestContext) {

        OgcApiDataV2 apiData = api.getData();
        checkPathParameter(extensionRegistry, apiData, "/collections/{collectionId}/styles/{styleId}", "styleId", styleId);

        QueriesHandlerStyles.QueryInputStyle queryInput = new ImmutableQueryInputStyle.Builder()
                .from(getGenericQueryInput(api.getData()))
                .styleId(styleId)
                .build();

        return queryHandler.handle(QueriesHandlerStyles.Query.STYLE, queryInput, requestContext);
    }
}

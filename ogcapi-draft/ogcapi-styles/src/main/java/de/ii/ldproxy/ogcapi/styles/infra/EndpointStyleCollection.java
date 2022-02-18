/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.foundation.domain.I18n;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableQueryInputStyle;
import de.ii.ldproxy.ogcapi.styles.domain.QueriesHandlerStyles;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleRepository;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.util.List;
import java.util.Optional;
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
 * fetch the stylesheet of a style
 */
@Singleton
@AutoBind
public class EndpointStyleCollection extends EndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStyleCollection.class);

    private static final List<String> TAGS = ImmutableList.of("Discover and fetch styles");

    private final StyleRepository styleRepository;
    private final QueriesHandlerStyles queryHandler;
    private final I18n i18n;

    @Inject
    public EndpointStyleCollection(ExtensionRegistry extensionRegistry,
                                   I18n i18n,
                                   StyleRepository styleRepository,
                                   QueriesHandlerStyles queryHandler) {
        super(extensionRegistry);
        this.i18n = i18n;
        this.styleRepository = styleRepository;
        this.queryHandler = queryHandler;
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

        for (String collectionId : apiData.getCollections().keySet()) {
            builder = styleRepository.validate(builder, apiData, Optional.of(collectionId));
        }

        return builder.build();
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("collections")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_STYLESHEET_COLLECTION);
        final String subSubPath = "/styles/{styleId}";
        final String path = "/collections/{collectionId}" + subSubPath;
        final HttpMethods method = HttpMethods.GET;
        final List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
        final Optional<OgcApiPathParameter> optCollectionIdParam = pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
        if (!optCollectionIdParam.isPresent()) {
            LOGGER.error("Path parameter 'collectionId' missing for resource at path '" + path + "'. The GET method will not be available.");
        } else {
            final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
            boolean explode = collectionIdParam.getExplodeInOpenApi(apiData);
            final List<String> collectionIds = (explode) ?
                    collectionIdParam.getValues(apiData) :
                    ImmutableList.of("{collectionId}");
            for (String collectionId : collectionIds) {
                List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path, collectionId);
                String operationSummary = "fetch a style for collection `"+collectionId+"`";
                Optional<String> operationDescription = Optional.of("Fetches the style with identifier `styleId`. " +
                                                                            "The set of available styles can be retrieved at `/styles`. Not all styles are available in " +
                                                                            "all style encodings.");
                String resourcePath = path.replace("{collectionId}", collectionId);
                ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                        .path(resourcePath)
                        .pathParameters(pathParameters);
                ApiOperation operation = addOperation(apiData, HttpMethods.GET, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, TAGS);
                if (operation != null)
                    resourceBuilder.putOperations(method.name(), operation);
                definitionBuilder.putResources(resourcePath, resourceBuilder.build());
            }
        }

        return definitionBuilder.build();
    }

    /**
     * Fetch a style by id
     *
     * @param styleId the local identifier of a specific style
     * @return the style in a json file
     */
    @Path("/{collectionId}/styles/{styleId}")
    @GET
    public Response getStyle(@Context OgcApi api,
                             @Context ApiRequestContext requestContext,
                             @PathParam("collectionId") String collectionId,
                             @PathParam("styleId") String styleId) {

        OgcApiDataV2 apiData = api.getData();
        checkPathParameter(extensionRegistry, apiData, "/collections/{collectionId}/styles/{styleId}", "collectionId", collectionId);
        checkPathParameter(extensionRegistry, apiData, "/collections/{collectionId}/styles/{styleId}", "styleId", styleId);
        checkCollectionExists(apiData, collectionId);

        QueriesHandlerStyles.QueryInputStyle queryInput = new ImmutableQueryInputStyle.Builder()
                .from(getGenericQueryInput(api.getData()))
                .collectionId(collectionId)
                .styleId(styleId)
                .build();

        return queryHandler.handle(QueriesHandlerStyles.Query.STYLE, queryInput, requestContext);
    }
}

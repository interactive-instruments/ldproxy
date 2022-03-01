/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.ld.app;

import static de.ii.ogcapi.foundation.domain.FoundationConfiguration.API_RESOURCES_DIR;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.features.geojson.ld.domain.GeoJsonLdConfiguration;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.base.domain.AppContext;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class EndpointJsonLdContext extends EndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointJsonLdContext.class);
    private static final List<String> TAGS = ImmutableList.of("Discover data collections");

    private final java.nio.file.Path contextDirectory;

    @Inject
    EndpointJsonLdContext(AppContext appContext, ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
        this.contextDirectory = appContext.getDataDir()
            .resolve(API_RESOURCES_DIR)
            .resolve("json-ld-contexts");
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return GeoJsonLdConfiguration.class;
    }

    private java.nio.file.Path getContextPath(String apiId, String collectionId, String contextFileName, String extension) {
        return Objects.nonNull(contextFileName)
                ? contextDirectory.resolve(apiId)
                                  .resolve(contextFileName)
                : contextDirectory.resolve(apiId)
                                  .resolve(collectionId + "." + extension);
    }

    @Path("/{collectionId}/context")
    @GET
    @Produces("application/ld+json")
    public Response getContext(@Context ApiRequestContext apiRequestContext, @Context OgcApi api,
                               @PathParam("collectionId") String collectionId) throws IOException {

        ContextFormatExtension format = extensionRegistry.getExtensionsForType(ContextFormatExtension.class)
                                                         .stream()
                                                         .filter(f -> f.isEnabledForApi(api.getData()))
                                                         .filter(f -> f.getMediaType()
                                                                       .matches(apiRequestContext.getMediaType()
                                                                                                 .type()))
                                                         .findFirst()
                                                         .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", apiRequestContext.getMediaType())));

        Optional<String> contextFileName = api.getData().getCollections().get(collectionId).getExtension(GeoJsonLdConfiguration.class).map(cfg -> cfg.getContextFileName());
        java.nio.file.Path context = getContextPath(api.getId(), collectionId, contextFileName.orElse(null), format.getMediaType().parameter());

        if (!Files.isRegularFile(context)) {
            throw new NotFoundException(String.format("The %s context was not found.", format.getMediaType().label()));
        }

        // TODO validate, that it is a valid JSON-LD Context document

        return Response.ok(format.getInputStream(context),"application/ld+json")
                       .build();
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(ContextFormatExtension.class);
        return formats;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("collections")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_FEATURES_JSONLD_CONTEXT);
        String subSubPath = "/context";
        String path = "/collections/{collectionId}" + subSubPath;
        List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
        Optional<OgcApiPathParameter> optCollectionIdParam = pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
        if (!optCollectionIdParam.isPresent()) {
            LOGGER.error("Path parameter 'collectionId' missing for resource at path '" + path + "'. The resource will not be available.");
        } else {
            final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
            final boolean explode = collectionIdParam.getExplodeInOpenApi(apiData);
            final List<String> collectionIds = (explode) ?
                    collectionIdParam.getValues(apiData) :
                    ImmutableList.of("{collectionId}");
            for (String collectionId : collectionIds) {
                if (explode && !apiData.getCollections()
                                       .get(collectionId)
                                       .getExtension(GeoJsonLdConfiguration.class)
                                       .map(ExtensionConfiguration::isEnabled)
                                       .orElse(false))
                    // skip, if disabled for the collection
                    continue;
                final List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path, collectionId);
                final String operationSummary = "retrieve the JSON-LD context for the feature collection '" + collectionId + "'";
                Optional<String> operationDescription = Optional.empty();
                String resourcePath = "/collections/" + collectionId + subSubPath;
                ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                        .path(resourcePath)
                        .pathParameters(pathParameters);
                ApiOperation operation = addOperation(apiData, queryParameters, resourcePath, operationSummary, operationDescription, TAGS);
                if (operation!=null)
                    resourceBuilder.putOperations("GET", operation);
                definitionBuilder.putResources(resourcePath, resourceBuilder.build());
            }
        }

        return definitionBuilder.build();
    }
}

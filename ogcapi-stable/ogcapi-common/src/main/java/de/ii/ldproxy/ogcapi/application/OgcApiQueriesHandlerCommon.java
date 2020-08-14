/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.application;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.immutables.value.Value;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Instantiate
@Provides(specifications = {OgcApiQueriesHandlerCommon.class})
public class OgcApiQueriesHandlerCommon implements OgcApiQueriesHandler<OgcApiQueriesHandlerCommon.Query> {

    public enum Query implements OgcApiQueryIdentifier {LANDING_PAGE, CONFORMANCE_DECLARATION, API_DEFINITION}

    @Requires
    I18n i18n;

    @Value.Immutable
    public interface OgcApiQueryInputLandingPage extends OgcApiQueryInput {
        boolean getIncludeLinkHeader();
    }

    @Value.Immutable
    public interface OgcApiQueryInputConformance extends OgcApiQueryInput {
        boolean getIncludeHomeLink();
        boolean getIncludeLinkHeader();
    }

    @Value.Immutable
    public interface OgcApiQueryInputApiDefinition extends OgcApiQueryInput {
        Optional<String> getSubPath();
    }

    private final OgcApiExtensionRegistry extensionRegistry;
    private final Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> queryHandlers;


    public OgcApiQueriesHandlerCommon(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;

        this.queryHandlers = ImmutableMap.of(
                Query.LANDING_PAGE, OgcApiQueryHandler.with(OgcApiQueryInputLandingPage.class, this::getLandingPageResponse),
                Query.CONFORMANCE_DECLARATION, OgcApiQueryHandler.with(OgcApiQueryInputConformance.class, this::getConformanceResponse),
                Query.API_DEFINITION, OgcApiQueryHandler.with(OgcApiQueryInputApiDefinition.class, this::getApiDefinitionResponse)
        );
    }

    @Override
    public Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    private Response getLandingPageResponse(OgcApiQueryInputLandingPage queryInput, OgcApiRequestContext requestContext) {
        final LandingPageLinksGenerator linksGenerator = new LandingPageLinksGenerator();
        OgcApiApi api = requestContext.getApi();
        OgcApiApiDataV2 apiData = api.getData();

        List<OgcApiLink> ogcApiLinks = linksGenerator.generateLinks(requestContext.getUriCustomizer()
                                                                                             .copy(),
                Optional.empty(), // TODO: support schema links, e.g. for WFS provider new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType()).getAsUrl()
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                i18n,
                requestContext.getLanguage());

        BoundingBox bbox = apiData.getSpatialExtent();
        OgcApiExtent spatialExtent = Objects.nonNull(bbox) ? new OgcApiExtent(bbox.getXmin(), bbox.getYmin(), bbox.getXmax(), bbox.getYmax()) : null;

        ImmutableLandingPage.Builder apiLandingPage = new ImmutableLandingPage.Builder()
                .title(apiData.getLabel())
                .description(apiData.getDescription().orElse(""))
                .externalDocs(apiData.getExternalDocs())
                .extent(Optional.ofNullable(spatialExtent))
                .links(ogcApiLinks);

        for (OgcApiLandingPageExtension ogcApiLandingPageExtension : getDatasetExtenders()) {
            apiLandingPage = ogcApiLandingPageExtension.process(apiLandingPage,
                    apiData,
                    requestContext.getUriCustomizer()
                                  .copy(),
                    requestContext.getMediaType(),
                    requestContext.getAlternateMediaTypes(),
                    requestContext.getLanguage());
        }

        CommonFormatExtension outputFormatExtension = api.getOutputFormat(CommonFormatExtension.class,
                                                                          requestContext.getMediaType(),"/")
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated.", requestContext.getMediaType().type())));

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? ogcApiLinks : null)
                .entity(outputFormatExtension.getLandingPageEntity(apiLandingPage.build(),
                                                                   requestContext.getApi(),
                                                                   requestContext))
                .build();
    }

    private Response getConformanceResponse(OgcApiQueryInputConformance queryInput,
                                            OgcApiRequestContext requestContext) {
        List<ConformanceClass> conformanceClasses = getConformanceClasses().stream()
                                                                           .filter(conformanceClass -> conformanceClass.isEnabledForApi(requestContext.getApi().getData()))
                                                                           .collect(Collectors.toList());

        List<OgcApiLink> ogcApiLinks = new ConformanceDeclarationLinksGenerator().generateLinks(
                requestContext.getUriCustomizer().copy(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                queryInput.getIncludeHomeLink(),
                i18n,
                requestContext.getLanguage());

        CommonFormatExtension outputFormatExtension = requestContext.getApi().getOutputFormat(CommonFormatExtension.class, requestContext.getMediaType(), "/conformance")
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        ImmutableConformanceDeclaration.Builder conformanceDeclaration = new ImmutableConformanceDeclaration.Builder()
                .links(ogcApiLinks)
                .conformsTo(conformanceClasses.stream()
                                              .map(ConformanceClass::getConformanceClassUris)
                                              .flatMap(List::stream)
                                              .collect(Collectors.toList()));

        for (OgcApiConformanceDeclarationExtension ogcApiConformanceDeclarationExtension : getConformanceExtenders()) {
            conformanceDeclaration = ogcApiConformanceDeclarationExtension.process(conformanceDeclaration,
                    requestContext.getApi().getData(),
                    requestContext.getUriCustomizer()
                            .copy(),
                    requestContext.getMediaType(),
                    requestContext.getAlternateMediaTypes(),
                    requestContext.getLanguage());
        }

        return prepareSuccessResponse(requestContext.getApi(), requestContext, queryInput.getIncludeLinkHeader() ? ogcApiLinks : null)
                .entity(outputFormatExtension.getConformanceEntity(conformanceDeclaration.build(),
                        requestContext.getApi(),
                        requestContext))
                .build();
    }

    private Response getApiDefinitionResponse(OgcApiQueryInputApiDefinition queryInput,
                                            OgcApiRequestContext requestContext) {

        String subPath = queryInput.getSubPath().orElse("");
        ApiDefinitionFormatExtension outputFormatExtension =
                requestContext.getApi()
                              .getOutputFormat(ApiDefinitionFormatExtension.class,
                                               requestContext.getMediaType(),
                                         "/api"+(subPath.isEmpty() ? "" : subPath.startsWith("/") ? subPath : "/"+subPath ))
                              .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        if (subPath.matches("/?[^/]+")) {
            return outputFormatExtension.getApiDefinitionFile(requestContext.getApi().getData(), requestContext, subPath);
        }

        return outputFormatExtension.getApiDefinitionResponse(requestContext.getApi().getData(), requestContext);
    }

    private List<OgcApiLandingPageExtension> getDatasetExtenders() {
        return extensionRegistry.getExtensionsForType(OgcApiLandingPageExtension.class);
    }

    private List<OgcApiConformanceDeclarationExtension> getConformanceExtenders() {
        return extensionRegistry.getExtensionsForType(OgcApiConformanceDeclarationExtension.class);
    }

    private List<ConformanceClass> getConformanceClasses() {
        return extensionRegistry.getExtensionsForType(ConformanceClass.class);
    }

    private void addLinks(Response.ResponseBuilder response, ImmutableList<OgcApiLink> links) {
        links.stream().forEach(link -> response.links(link.getLink()));
    }
}

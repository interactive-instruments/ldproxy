/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.common.domain.*;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Instantiate
@Provides(specifications = {QueriesHandlerCommon.class})
public class QueriesHandlerCommon implements QueriesHandler<QueriesHandlerCommon.Query> {

    public enum Query implements QueryIdentifier {LANDING_PAGE, CONFORMANCE_DECLARATION, API_DEFINITION}

    @Requires
    I18n i18n;

    @Value.Immutable
    public interface QueryInputLandingPage extends QueryInput {
        boolean getIncludeLinkHeader();
        List<Link> getAdditionalLinks();
    }

    @Value.Immutable
    public interface QueryInputConformance extends QueryInput {
        boolean getIncludeLinkHeader();
    }

    @Value.Immutable
    public interface Definition extends QueryInput {
        Optional<String> getSubPath();
    }

    private final ExtensionRegistry extensionRegistry;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;


    public QueriesHandlerCommon(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;

        this.queryHandlers = ImmutableMap.of(
                Query.LANDING_PAGE, QueryHandler.with(QueryInputLandingPage.class, this::getLandingPageResponse),
                Query.CONFORMANCE_DECLARATION, QueryHandler.with(QueryInputConformance.class, this::getConformanceResponse),
                Query.API_DEFINITION, QueryHandler.with(Definition.class, this::getApiDefinitionResponse)
        );
    }

    @Override
    public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    private Response getLandingPageResponse(QueryInputLandingPage queryInput, ApiRequestContext requestContext) {
        final LandingPageLinksGenerator linksGenerator = new LandingPageLinksGenerator();
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();

        List<Link> links = linksGenerator.generateLinks(requestContext.getUriCustomizer()
                                                                      .copy(),
                                                        Optional.empty(), // TODO: support schema links, e.g. for WFS provider new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType()).getAsUrl()
                                                        requestContext.getMediaType(),
                                                        requestContext.getAlternateMediaTypes(),
                                                        i18n,
                                                        requestContext.getLanguage());

        Optional<BoundingBox> bbox = apiData.getSpatialExtent();
        Optional<TemporalExtent> interval = apiData.getTemporalExtent();
        OgcApiExtent spatialExtent = bbox.isPresent() && interval.isPresent() ?
                new OgcApiExtent(interval.get().getStart(), interval.get().getEnd(), bbox.get().getXmin(), bbox.get().getYmin(), bbox.get().getXmax(), bbox.get().getYmax()) :
                bbox.isPresent() ?
                        new OgcApiExtent(bbox.get().getXmin(), bbox.get().getYmin(), bbox.get().getXmax(), bbox.get().getYmax()) :
                        interval.isPresent() ?
                                new OgcApiExtent(interval.get().getStart(), interval.get().getEnd()) :
                                null;

        ImmutableLandingPage.Builder apiLandingPage = new ImmutableLandingPage.Builder()
                .title(apiData.getLabel())
                .description(apiData.getDescription().orElse(""))
                .externalDocs(apiData.getExternalDocs())
                .extent(Optional.ofNullable(spatialExtent))
                .links(links)
                .addAllLinks(queryInput.getAdditionalLinks());

        for (LandingPageExtension ogcApiLandingPageExtension : getDatasetExtenders()) {
            apiLandingPage = ogcApiLandingPageExtension.process(apiLandingPage,
                    apiData,
                    requestContext.getUriCustomizer()
                                  .copy(),
                    requestContext.getMediaType(),
                    requestContext.getAlternateMediaTypes(),
                    requestContext.getLanguage());
        }

        CommonFormatExtension outputFormatExtension = api.getOutputFormat(CommonFormatExtension.class,
                                                                          requestContext.getMediaType(),"/",
                                                                          Optional.empty())
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated.", requestContext.getMediaType().type())));

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(outputFormatExtension.getLandingPageEntity(apiLandingPage.build(),
                                                                   requestContext.getApi(),
                                                                   requestContext))
                .build();
    }

    private Response getConformanceResponse(QueryInputConformance queryInput,
                                            ApiRequestContext requestContext) {
        List<ConformanceClass> conformanceClasses = getConformanceClasses().stream()
                                                                           .filter(conformanceClass -> conformanceClass.isEnabledForApi(requestContext.getApi().getData()))
                                                                           .collect(Collectors.toList());

        List<Link> links = new ConformanceDeclarationLinksGenerator().generateLinks(
                requestContext.getUriCustomizer().copy(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                i18n,
                requestContext.getLanguage());

        CommonFormatExtension outputFormatExtension = requestContext.getApi().getOutputFormat(CommonFormatExtension.class, requestContext.getMediaType(), "/conformance", Optional.empty())
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        ImmutableConformanceDeclaration.Builder conformanceDeclaration = new ImmutableConformanceDeclaration.Builder()
                .links(links)
                .conformsTo(conformanceClasses.stream()
                                              .map(ConformanceClass::getConformanceClassUris)
                                              .flatMap(List::stream)
                                              .collect(Collectors.toList()));

        for (ConformanceDeclarationExtension ogcApiConformanceDeclarationExtension : getConformanceExtenders()) {
            conformanceDeclaration = ogcApiConformanceDeclarationExtension.process(conformanceDeclaration,
                    requestContext.getApi().getData(),
                    requestContext.getUriCustomizer()
                            .copy(),
                    requestContext.getMediaType(),
                    requestContext.getAlternateMediaTypes(),
                    requestContext.getLanguage());
        }

        return prepareSuccessResponse(requestContext.getApi(), requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(outputFormatExtension.getConformanceEntity(conformanceDeclaration.build(),
                        requestContext.getApi(),
                        requestContext))
                .build();
    }

    private Response getApiDefinitionResponse(Definition queryInput,
                                              ApiRequestContext requestContext) {

        String subPath = queryInput.getSubPath().orElse("");
        ApiDefinitionFormatExtension outputFormatExtension =
                requestContext.getApi()
                              .getOutputFormat(ApiDefinitionFormatExtension.class,
                                               requestContext.getMediaType(),
                                         "/api"+(subPath.isEmpty() ? "" : subPath.startsWith("/") ? subPath : "/"+subPath ),
                                               Optional.empty())
                              .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        if (subPath.matches("/?[^/]+")) {
            return outputFormatExtension.getApiDefinitionFile(requestContext.getApi().getData(), requestContext, subPath);
        }

        return outputFormatExtension.getApiDefinitionResponse(requestContext.getApi().getData(), requestContext);
    }

    private List<LandingPageExtension> getDatasetExtenders() {
        return extensionRegistry.getExtensionsForType(LandingPageExtension.class);
    }

    private List<ConformanceDeclarationExtension> getConformanceExtenders() {
        return extensionRegistry.getExtensionsForType(ConformanceDeclarationExtension.class);
    }

    private List<ConformanceClass> getConformanceClasses() {
        return extensionRegistry.getExtensionsForType(ConformanceClass.class);
    }

    private void addLinks(Response.ResponseBuilder response, ImmutableList<Link> links) {
        links.stream().forEach(link -> response.links(link.getLink()));
    }
}

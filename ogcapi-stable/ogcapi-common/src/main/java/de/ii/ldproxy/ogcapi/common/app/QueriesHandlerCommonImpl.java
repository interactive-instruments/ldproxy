/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.I18n;
import de.ii.ldproxy.ogcapi.foundation.domain.Link;
import de.ii.ldproxy.ogcapi.foundation.domain.Metadata;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.PageRepresentation;
import de.ii.ldproxy.ogcapi.foundation.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.foundation.domain.QueryIdentifier;
import de.ii.ldproxy.ogcapi.foundation.domain.QueryInput;
import de.ii.ldproxy.ogcapi.foundation.domain.TemporalExtent;
import de.ii.ldproxy.ogcapi.common.domain.*;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.immutables.value.Value;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@AutoBind
public class QueriesHandlerCommonImpl implements QueriesHandlerCommon {

    public enum Query implements QueryIdentifier {LANDING_PAGE, CONFORMANCE_DECLARATION, API_DEFINITION}

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
    private final I18n i18n;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;

    @Inject
    public QueriesHandlerCommonImpl(ExtensionRegistry extensionRegistry, I18n i18n) {
        this.extensionRegistry = extensionRegistry;
        this.i18n = i18n;
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

        ImmutableLandingPage.Builder builder = new ImmutableLandingPage.Builder()
            .title(apiData.getLabel())
            .description(apiData.getDescription().orElse(""))
            .attribution(apiData.getMetadata().flatMap(Metadata::getAttribution))
            .externalDocs(apiData.getExternalDocs())
            .extent(Optional.ofNullable(spatialExtent))
            .links(links)
            .addAllLinks(queryInput.getAdditionalLinks());

        for (LandingPageExtension ogcApiLandingPageExtension : getDatasetExtenders()) {
            builder = ogcApiLandingPageExtension.process(builder,
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

        LandingPage apiLandingPage = builder.build();

        Object entity = outputFormatExtension.getLandingPageEntity(apiLandingPage,
                                                                   requestContext.getApi(),
                                                                   requestContext);

        Date lastModified = getLastModified(queryInput, api);
        EntityTag etag = !outputFormatExtension.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
            || api.getData().getExtension(HtmlConfiguration.class).map(HtmlConfiguration::getSendEtags).orElse(false)
            ? getEtag(apiLandingPage, PageRepresentation.FUNNEL, outputFormatExtension)
            : null;
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext,
                                      queryInput.getIncludeLinkHeader() ? apiLandingPage.getLinks() : null,
                                      lastModified,
                                      etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null,
                                      true,
                                      String.format("landing-page.%s", outputFormatExtension.getMediaType().fileExtension()))
                .entity(entity)
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

        ImmutableConformanceDeclaration.Builder builder = new ImmutableConformanceDeclaration.Builder()
                .links(links)
                .conformsTo(conformanceClasses.stream()
                                              .map(conformanceClass -> conformanceClass.getConformanceClassUris(requestContext.getApi().getData()))
                                              .flatMap(List::stream)
                                              .distinct()
                                              .collect(Collectors.toUnmodifiableList()));

        for (ConformanceDeclarationExtension ogcApiConformanceDeclarationExtension : getConformanceExtenders()) {
            builder = ogcApiConformanceDeclarationExtension.process(builder,
                    requestContext.getApi().getData(),
                    requestContext.getUriCustomizer()
                            .copy(),
                    requestContext.getMediaType(),
                    requestContext.getAlternateMediaTypes(),
                    requestContext.getLanguage());
        }

        ConformanceDeclaration conformanceDeclaration = builder.build();

        Object entity = outputFormatExtension.getConformanceEntity(conformanceDeclaration,
                                                                   requestContext.getApi(),
                                                                   requestContext);
        Date lastModified = getLastModified(queryInput, requestContext.getApi());
        EntityTag etag = !outputFormatExtension.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
            || requestContext.getApi().getData().getExtension(HtmlConfiguration.class).map(HtmlConfiguration::getSendEtags).orElse(false)
            ? getEtag(conformanceDeclaration, ConformanceDeclaration.FUNNEL, outputFormatExtension)
            : null;
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext,
                                      queryInput.getIncludeLinkHeader() ? conformanceDeclaration.getLinks() : null,
                                      lastModified,
                                      etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null,
                                      true,
                                      String.format("conformance-declaration.%s", outputFormatExtension.getMediaType().fileExtension()))
                .entity(entity)
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

        Date lastModified = getLastModified(queryInput, requestContext.getApi());
        // TODO support ETag
        EntityTag etag = null;
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        // TODO support headers
        return outputFormatExtension.getApiDefinitionResponse(requestContext.getApi().getData(),
                                                              requestContext);
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
}

/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.application;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.immutables.value.Value;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Instantiate
@Provides(specifications = {OgcApiQueriesHandlerCommon.class})
public class OgcApiQueriesHandlerCommon implements OgcApiQueriesHandler<OgcApiQueriesHandlerCommon.Query> {

    public enum Query implements OgcApiQueryIdentifier {LANDING_PAGE, CONFORMANCE_DECLARATION, API_DEFINITION}

    @Value.Immutable
    public interface OgcApiQueryInputLandingPage extends OgcApiQueryInput {

    }

    @Value.Immutable
    public interface OgcApiQueryInputConformance extends OgcApiQueryInput {

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

        List<OgcApiLink> ogcApiLinks = linksGenerator.generateLandingPageLinks(requestContext.getUriCustomizer()
                                                                                             .copy(),
                Optional.empty() /* TODO: support schema links, e.g. for WFS provider new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType()).getAsUrl()*/,
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes());

        ImmutableLandingPage.Builder apiLandingPage = new ImmutableLandingPage.Builder()
                .title(requestContext.getApi().getData().getLabel())
                .description(requestContext.getApi().getData().getDescription().orElse(""))
                .links(ogcApiLinks);


        for (OgcApiLandingPageExtension ogcApiLandingPageExtension : getDatasetExtenders()) {
            apiLandingPage = ogcApiLandingPageExtension.process(apiLandingPage,
                    requestContext.getApi().getData(),
                    requestContext.getUriCustomizer()
                                  .copy(),
                    requestContext.getMediaType(),
                    requestContext.getAlternateMediaTypes());
        }

        CommonFormatExtension outputFormatExtension = requestContext.getApi()
                .getOutputFormat(CommonFormatExtension.class,
                        requestContext.getMediaType(),
                        "/")
                .orElseThrow(NotAcceptableException::new);

        Response landingPageResponse = outputFormatExtension.getLandingPageResponse(apiLandingPage.build(),
                requestContext.getApi(),
                requestContext);

        return Response.ok()
                       .entity(landingPageResponse.getEntity())
                       .type(requestContext.getMediaType()
                                           .type())
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
                requestContext.getAlternateMediaTypes());

        CommonFormatExtension outputFormatExtension = getOutputFormat(CommonFormatExtension.class, requestContext.getMediaType(), requestContext.getApi().getData(), "/conformance")
                .orElseThrow(NotAcceptableException::new);

        ImmutableConformanceDeclaration.Builder conformanceDeclaration = new ImmutableConformanceDeclaration.Builder()
                .title("Conformance Declaration") // TODO
                .description("This API implements the conformance classes from standards and community specifications that are listed below. Conformance classes are identified by a URI.") // TODO
                .links(ogcApiLinks)
                .conformsTo(conformanceClasses.stream()
                                              .map(ConformanceClass::getConformanceClass)
                                              .collect(Collectors.toList()));

        for (OgcApiConformanceDeclarationExtension ogcApiConformanceDeclarationExtension : getConformanceExtenders()) {
            conformanceDeclaration = ogcApiConformanceDeclarationExtension.process(conformanceDeclaration,
                    requestContext.getApi().getData(),
                    requestContext.getUriCustomizer()
                            .copy(),
                    requestContext.getMediaType(),
                    requestContext.getAlternateMediaTypes());
        }

        Response conformanceDeclarationResponse = outputFormatExtension.getConformanceResponse(conformanceDeclaration.build(),
                requestContext.getApi(),
                requestContext);

        return Response.ok()
                       .entity(conformanceDeclarationResponse.getEntity())
                       .type(requestContext.getMediaType()
                                           .type())
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
                              .orElseThrow(NotAcceptableException::new);

        if (subPath.matches("/?[^/]+"))
            return outputFormatExtension.getApiDefinitionFile(requestContext.getApi().getData(), requestContext, subPath);

        return outputFormatExtension.getApiDefinitionResponse(requestContext.getApi().getData(), requestContext);
    }

    private <T extends FormatExtension> Optional<T> getOutputFormat(Class<T> extensionType, OgcApiMediaType mediaType, OgcApiDatasetData apiData, String path) {
        return extensionRegistry.getExtensionsForType(extensionType)
                                .stream()
                                .filter(outputFormatExtension -> path.matches(outputFormatExtension.getPathPattern()))
                                .filter(outputFormatExtension -> mediaType.type().isCompatible(outputFormatExtension.getMediaType().type()))
                                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
                                .findFirst();
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

}

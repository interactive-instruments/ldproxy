/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.application;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.crs.api.EpsgCrs;
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
import java.util.stream.Stream;

@Component
@Instantiate
@Provides(specifications = {OgcApiQueriesHandlerCommon.class})
public class OgcApiQueriesHandlerCommon implements OgcApiQueriesHandler<OgcApiQueriesHandlerCommon.CommonQuery> {

    public enum CommonQuery implements OgcApiQueryIdentifier {LANDING_PAGE, CONFORMANCE, API_DEFINITION}

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
    private final Map<CommonQuery, OgcApiQueryHandler<? extends OgcApiQueryInput>> queryHandlers;


    public OgcApiQueriesHandlerCommon(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;

        this.queryHandlers = ImmutableMap.of(
                CommonQuery.LANDING_PAGE, OgcApiQueryHandler.with(OgcApiQueryInputLandingPage.class, this::getLandingPageResponse),
                CommonQuery.CONFORMANCE, OgcApiQueryHandler.with(OgcApiQueryInputConformance.class, this::getConformanceResponse),
                CommonQuery.API_DEFINITION, OgcApiQueryHandler.with(OgcApiQueryInputApiDefinition.class, this::getApiDefinitionResponse)
        );
    }

    @Override
    public Map<CommonQuery, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    public Response getLandingPageResponse(OgcApiRequestContext requestContext) {

        final LandingPageLinksGenerator linksGenerator = new LandingPageLinksGenerator();

        //TODO: to crs extension
        OgcApiDatasetData apiData = requestContext.getApi().getData();
        ImmutableList<String> crs = ImmutableList.<String>builder()
                .addAll(Stream.of(apiData.getFeatureProvider()
                        .getNativeCrs()
                        .getAsUri())
                        .filter(crsUri -> !crsUri.equalsIgnoreCase(OgcApiDatasetData.DEFAULT_CRS_URI))
                        .collect(Collectors.toList()))
                .add(OgcApiDatasetData.DEFAULT_CRS_URI)
                .addAll(apiData.getAdditionalCrs()
                        .stream()
                        .map(EpsgCrs::getAsUri)
                        .filter(crsUri -> !crsUri.equalsIgnoreCase(OgcApiDatasetData.DEFAULT_CRS_URI))
                        .collect(Collectors.toList()))
                .build();

        CommonFormatExtension format =
                requestContext.getApi()
                        .getOutputFormat(CommonFormatExtension.class, requestContext.getMediaType(), "/")
                        .orElseThrow(NotAcceptableException::new);
        List<CommonFormatExtension> alternateFormats =
                requestContext.getApi()
                        .getAllOutputFormats(CommonFormatExtension.class,
                                             requestContext.getMediaType(),
                                       "/",
                                             Optional.of(format));
        List<OgcApiMediaType> alternateMediaTypes = alternateFormats.stream()
                .map(alternateFormat -> alternateFormat.getMediaType())
                .collect(Collectors.toList());

        List<OgcApiLink> ogcApiLinks = linksGenerator.generateLandingPageLinks(requestContext.getUriCustomizer().copy(), Optional.empty(), requestContext.getMediaType(), alternateMediaTypes);

        ImmutableDataset.Builder dataset = new ImmutableDataset.Builder()
                .crs(crs)
                .links(ogcApiLinks);

        for (OgcApiLandingPageExtension ogcApiLandingPageExtension : getDatasetExtenders()) {
            dataset = ogcApiLandingPageExtension.process(dataset, requestContext.getApi().getData(), requestContext.getUriCustomizer().copy(), requestContext.getMediaType(), alternateMediaTypes);
        }

        return format.getLandingPageResponse(dataset.build(), requestContext.getApi(), requestContext);
    }


    private Response getLandingPageResponse(OgcApiQueryInputLandingPage queryInput, OgcApiRequestContext requestContext) {
        final LandingPageLinksGenerator linksGenerator = new LandingPageLinksGenerator();

        //TODO: to crs extension
        OgcApiDatasetData apiData = requestContext.getApi().getData();
        ImmutableList<String> crs = ImmutableList.<String>builder()
                .addAll(Stream.of(apiData.getFeatureProvider()
                        .getNativeCrs()
                        .getAsUri())
                        .filter(crsUri -> !crsUri.equalsIgnoreCase(OgcApiDatasetData.DEFAULT_CRS_URI))
                        .collect(Collectors.toList()))
                .add(OgcApiDatasetData.DEFAULT_CRS_URI)
                .addAll(apiData.getAdditionalCrs()
                        .stream()
                        .map(EpsgCrs::getAsUri)
                        .filter(crsUri -> !crsUri.equalsIgnoreCase(OgcApiDatasetData.DEFAULT_CRS_URI))
                        .collect(Collectors.toList()))
                .build();

        List<OgcApiLink> ogcApiLinks = linksGenerator.generateLandingPageLinks(requestContext.getUriCustomizer()
                                                                                     .copy(), Optional.empty()/*new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType()).getAsUrl()*/, requestContext.getMediaType(), requestContext.getAlternateMediaTypes());

        ImmutableDataset.Builder dataset = new ImmutableDataset.Builder()
                //.collections(collections)
                .title(requestContext.getApi().getData().getLabel())
                .description(requestContext.getApi().getData().getDescription().orElse(""))
                .crs(crs)
                .links(ogcApiLinks);


        for (OgcApiLandingPageExtension ogcApiLandingPageExtension : getDatasetExtenders()) {
            dataset = ogcApiLandingPageExtension.process(dataset,
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

        Response datasetResponse = outputFormatExtension.getLandingPageResponse(dataset.build(),
                requestContext.getApi(),
                requestContext);

        return Response.ok()
                       .entity(datasetResponse.getEntity())
                       .type(requestContext.getMediaType()
                                           .type())
                       .build();
    }

    private Response getConformanceResponse(OgcApiQueryInputConformance queryInput,
                                            OgcApiRequestContext requestContext) {

        List<ConformanceClass> conformanceClasses = getConformanceClasses().stream()
                                                                           .filter(conformanceClass -> conformanceClass.isEnabledForApi(requestContext.getApi().getData()))
                                                                           .collect(Collectors.toList());

        CommonFormatExtension outputFormatExtension = getOutputFormat(CommonFormatExtension.class, requestContext.getMediaType(), requestContext.getApi().getData(), "/conformance")
                .orElseThrow(NotAcceptableException::new);

        //TODO
        Response conformanceResponse = outputFormatExtension
                .getConformanceResponse(conformanceClasses,
                        requestContext.getApi(),
                        requestContext);

        return Response.ok()
                       .entity(conformanceResponse.getEntity())
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

    private List<ConformanceClass> getConformanceClasses() {
        return extensionRegistry.getExtensionsForType(ConformanceClass.class);
    }

}

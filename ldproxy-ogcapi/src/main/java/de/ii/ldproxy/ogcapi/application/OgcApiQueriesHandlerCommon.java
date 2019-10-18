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
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ImmutableDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueriesHandler;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryHandler;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryIdentifier;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryInput;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.OutputFormatExtension;
import de.ii.ldproxy.ogcapi.domain.Wfs3DatasetMetadataExtension;
import de.ii.ldproxy.ogcapi.domain.Wfs3Link;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.immutables.value.Value;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Instantiate
@Provides(specifications = {OgcApiQueriesHandlerCommon.class})
public class OgcApiQueriesHandlerCommon implements OgcApiQueriesHandler<OgcApiQueriesHandlerCommon.CommonQuery> {

    public enum CommonQuery implements OgcApiQueryIdentifier {DATASET, CONFORMANCE}

    @Value.Immutable
    public interface OgcApiQueryInputDataset extends OgcApiQueryInput {

    }

    @Value.Immutable
    public interface OgcApiQueryInputConformance extends OgcApiQueryInput {

    }

    private final OgcApiExtensionRegistry extensionRegistry;
    private final Map<CommonQuery, OgcApiQueryHandler<? extends OgcApiQueryInput>> queryHandlers;


    public OgcApiQueriesHandlerCommon(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;

        this.queryHandlers = ImmutableMap.of(
                CommonQuery.DATASET, OgcApiQueryHandler.with(OgcApiQueryInputDataset.class, this::getDatasetResponse),
                CommonQuery.CONFORMANCE, OgcApiQueryHandler.with(OgcApiQueryInputConformance.class, this::getConformanceResponse)
        );
    }

    @Override
    public Map<CommonQuery, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers() {
        return queryHandlers;
    }


    private Response getDatasetResponse(OgcApiQueryInputDataset queryInput, OgcApiRequestContext requestContext) {
        final DatasetLinksGenerator linksGenerator = new DatasetLinksGenerator();

        //TODO: to crs extension
        ImmutableList<String> crs = Stream.concat(
                Stream.of(
                        requestContext.getDataset()
                                      .getFeatureProvider()
                                      .getNativeCrs()
                                      .getAsUri(),
                        OgcApiDatasetData.DEFAULT_CRS_URI
                ),
                requestContext.getDataset()
                              .getAdditionalCrs()
                              .stream()
                              .map(EpsgCrs::getAsUri)
        )
                                           .distinct()
                                           .collect(ImmutableList.toImmutableList());


        List<Wfs3Link> wfs3Links = linksGenerator.generateDatasetLinks(requestContext.getUriCustomizer()
                                                                                     .copy(), Optional.empty()/*new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType()).getAsUrl()*/, requestContext.getMediaType(), requestContext.getAlternativeMediaTypes());


        ImmutableDataset.Builder dataset = new ImmutableDataset.Builder()
                //.collections(collections)
                .title(requestContext.getDataset().getLabel())
                .description(requestContext.getDataset().getDescription().orElse(""))
                .crs(crs)
                .links(wfs3Links);


        for (Wfs3DatasetMetadataExtension wfs3DatasetMetadataExtension : getDatasetExtenders()) {
            dataset = wfs3DatasetMetadataExtension.process(dataset, requestContext.getDataset(), requestContext.getUriCustomizer()
                                                                                                               .copy(), requestContext.getMediaType(), requestContext.getAlternativeMediaTypes());
        }

        OutputFormatExtension outputFormatExtension = findOutputFormat(requestContext.getMediaType(), requestContext.getDataset())
                .orElseThrow(NotAcceptableException::new);

        //TODO
        Response datasetResponse = outputFormatExtension
                .getDatasetResponse(dataset.build(), requestContext.getDataset(), requestContext.getMediaType(), requestContext.getAlternativeMediaTypes(), requestContext.getUriCustomizer(), requestContext.getStaticUrlPrefix(), false);

        return Response.ok()
                       .entity(datasetResponse.getEntity())
                       .type(requestContext.getMediaType()
                                           .metadata())
                       .build();
    }

    private Response getConformanceResponse(OgcApiQueryInputConformance queryInput,
                                            OgcApiRequestContext requestContext) {
        //Wfs3MediaType wfs3MediaType = checkMediaType(mediaType);

        List<ConformanceClass> conformanceClasses = getConformanceClasses().stream()
                                                                           .filter(conformanceClass -> conformanceClass.isEnabledForDataset(requestContext.getDataset()))
                                                                           .collect(Collectors.toList());

        OutputFormatExtension outputFormatExtension = findOutputFormat(requestContext.getMediaType(), requestContext.getDataset())
                .orElseThrow(NotAcceptableException::new);

        //TODO
        Response conformanceResponse = outputFormatExtension
                .getConformanceResponse(conformanceClasses, requestContext.getDataset()
                                                                          .getLabel(), requestContext.getMediaType(), requestContext.getAlternativeMediaTypes(), requestContext.getUriCustomizer(), requestContext.getStaticUrlPrefix());

        return Response.ok()
                       .entity(conformanceResponse.getEntity())
                       .type(requestContext.getMediaType()
                                           .metadata())
                       .build();
    }

    private Optional<OutputFormatExtension> findOutputFormat(OgcApiMediaType mediaType, OgcApiDatasetData dataset) {
        return extensionRegistry.getExtensionsForType(OutputFormatExtension.class)
                                .stream()
                                .filter(outputFormatExtension -> Objects.equals(outputFormatExtension.getMediaType(), mediaType))
                                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForDataset(dataset))
                                .findFirst();
    }

    private List<Wfs3DatasetMetadataExtension> getDatasetExtenders() {
        return extensionRegistry.getExtensionsForType(Wfs3DatasetMetadataExtension.class).stream().sorted(Comparator.comparingInt(OgcApiExtension::getSortPriority)).collect(Collectors.toList());
    }

    private List<ConformanceClass> getConformanceClasses() {
        return extensionRegistry.getExtensionsForType(ConformanceClass.class);
    }

}

/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessChain;
import de.ii.ldproxy.ogcapi.features.processing.ImmutableProcessing;
import de.ii.ldproxy.ogcapi.features.processing.Processing;
import de.ii.xtraplatform.codelists.CodelistRegistry;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureStream2;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

@Component
@Instantiate
@Provides
public class TilesQueriesHandlerImpl implements TilesQueriesHandler {

    private final I18n i18n;
    private final CrsTransformerFactory crsTransformerFactory;
    private final Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> queryHandlers;
    private final MetricRegistry metricRegistry;
    private CodelistRegistry codelistRegistry;
    private final OgcApiExtensionRegistry extensionRegistry;

    public TilesQueriesHandlerImpl(@Requires I18n i18n,
                                   @Requires CrsTransformerFactory crsTransformerFactory,
                                   @Requires Dropwizard dropwizard,
                                   @Requires CodelistRegistry codelistRegistry,
                                   @Requires OgcApiExtensionRegistry extensionRegistry) {
        this.i18n = i18n;
        this.crsTransformerFactory = crsTransformerFactory;
        this.codelistRegistry = codelistRegistry;

        this.metricRegistry = dropwizard.getEnvironment()
                                        .metrics();
        this.extensionRegistry = extensionRegistry;

        this.queryHandlers = ImmutableMap.of(
                Query.TILE_MATRIX_SETS,
                OgcApiQueryHandler.with(OgcApiQueryInputTileMatrixSets.class, this::getTileMatrixSetsResponse),
                Query.TILE_MATRIX_SET,
                OgcApiQueryHandler.with(OgcApiQueryInputTileMatrixSet.class, this::getTileMatrixSetResponse)
        );
    }

    @Override
    public Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    private Response getTileMatrixSetsResponse(OgcApiQueryInputTileMatrixSets queryInput, OgcApiRequestContext requestContext) {
        OgcApiApi api = requestContext.getApi();
        String path = "/tileMatrixSets";

        TileMatrixSetsFormatExtension outputFormat = api.getOutputFormat(TileMatrixSetsFormatExtension.class, requestContext.getMediaType(), path)
                .orElseThrow(NotAcceptableException::new);

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        List<OgcApiLink> links = new TileMatrixSetsLinksGenerator().generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                queryInput.getIncludeHomeLink(),
                true,
                i18n,
                requestContext.getLanguage());

        TileMatrixSets tileMatrixSets = ImmutableTileMatrixSets.builder()
                .tileMatrixSets(
                        queryInput.getTileMatrixSets()
                                .stream()
                                .map(tileMatrixSet -> ImmutableTileMatrixSetLinks.builder()
                                        .id(tileMatrixSet.getId())
                                        .title(tileMatrixSet.getTileMatrixSetData().getTitle())
                                        .links(vectorTilesLinkGenerator.generateTileMatrixSetsLinks(
                                                requestContext.getUriCustomizer(),
                                                tileMatrixSet.getId().toString(),
                                                i18n,
                                                requestContext.getLanguage()))
                                        .build())
                                .collect(Collectors.toList()))
                .links(links)
                .build();

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(outputFormat.getTileMatrixSetsEntity(tileMatrixSets, api, requestContext))
                .build();
    }

    private Response getTileMatrixSetResponse(OgcApiQueryInputTileMatrixSet queryInput, OgcApiRequestContext requestContext) {
        OgcApiApi api = requestContext.getApi();
        String tileMatrixSetId = queryInput.getTileMatrixSetId();
        String path = "/tileMatrixSets/"+tileMatrixSetId;

        TileMatrixSetsFormatExtension outputFormat = api.getOutputFormat(TileMatrixSetsFormatExtension.class, requestContext.getMediaType(), path)
                .orElseThrow(NotAcceptableException::new);

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        List<OgcApiLink> links = new TileMatrixSetsLinksGenerator().generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                queryInput.getIncludeHomeLink(),
                false,
                i18n,
                requestContext.getLanguage());

        TileMatrixSet tileMatrixSet = null;
        for (OgcApiContentExtension contentExtension : extensionRegistry.getExtensionsForType(OgcApiContentExtension.class)) {
            if (contentExtension instanceof TileMatrixSet && ((TileMatrixSet) contentExtension).getId().equals(tileMatrixSetId)) {
                tileMatrixSet = (TileMatrixSet) contentExtension;
                break;
            }
        }
        if (Objects.isNull(tileMatrixSet)) {
            throw new NotFoundException("Unknown tile matrix set: " + tileMatrixSetId);
        }

        TileMatrixSetData tileMatrixSetData = ImmutableTileMatrixSetData.builder()
                .from(tileMatrixSet.getTileMatrixSetData())
                .links(links)
                .build();

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(outputFormat.getTileMatrixSetEntity(tileMatrixSetData, api, requestContext))
                .build();
    }

/*
    private Response getProcessResponse(OgcApiQueryInputObservationProcessing queryInput, OgcApiRequestContext requestContext) {
        OgcApiApi api = requestContext.getApi();
        OgcApiApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        FeatureQuery query = queryInput.getQuery();
        FeatureProvider2 featureProvider = queryInput.getFeatureProvider();
        EpsgCrs defaultCrs = queryInput.getDefaultCrs();
        boolean includeLinkHeader = queryInput.getIncludeLinkHeader();
        Map<String, Object> processingParameters = queryInput.getProcessingParameters();
        FeatureProcessChain processes = queryInput.getProcesses();

        ObservationProcessingOutputFormat outputFormat = api.getOutputFormat(
                ObservationProcessingOutputFormat.class,
                requestContext.getMediaType(),
                "/collections/" + collectionId + processes.getSubSubPath())
                .orElseThrow(NotAcceptableException::new);

        ensureCollectionIdExists(api.getData(), collectionId);
        ensureFeatureProviderSupportsQueries(featureProvider);

        Optional<CrsTransformer> crsTransformer = Optional.empty();
        boolean swapCoordinates = false;

        EpsgCrs targetCrs = query.getCrs().orElse(defaultCrs);
        if (featureProvider.supportsCrs()) {
            EpsgCrs sourceCrs = featureProvider.crs().getNativeCrs();
            //TODO: warmup on service start
            crsTransformer = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
            swapCoordinates = crsTransformer.isPresent() ? crsTransformer.get()
                    .needsCoordinateSwap() : query.getCrs().isPresent() && featureProvider.crs().shouldSwapCoordinates(query.getCrs().get());
        }

        List<OgcApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        // TODO add links
        List<OgcApiLink> links = ImmutableList.of();

        // TODO update
        ImmutableFeatureTransformationContextObservationProcessing.Builder transformationContext = new ImmutableFeatureTransformationContextObservationProcessing.Builder()
                .apiData(api.getData())
                .collectionId(collectionId)
                .ogcApiRequest(requestContext)
                .crsTransformer(crsTransformer)
                .codelists(codelistRegistry.getCodelists())
                .defaultCrs(defaultCrs)
                .links(links)
                .isFeatureCollection(true)
                .isHitsOnly(query.hitsOnly())
                .isPropertyOnly(query.propertyOnly())
                .processes(queryInput.getProcesses())
                .processingParameters(queryInput.getProcessingParameters())
                .fields(query.getFields())
                .limit(query.getLimit())
                .offset(query.getOffset())
                .maxAllowableOffset(query.getMaxAllowableOffset())
                .geometryPrecision(query.getGeometryPrecision())
                .shouldSwapCoordinates(swapCoordinates);

        StreamingOutput streamingOutput;

        if (outputFormat.canTransformFeatures()) {
            FeatureStream2 featureStream = featureProvider.queries()
                    .getFeatureStream2(query);

            streamingOutput = stream(featureStream, outputStream -> outputFormat.getFeatureTransformer(transformationContext.outputStream(outputStream)
                    .build(), requestContext.getLanguage())
                    .get());
        } else {
            throw new NotAcceptableException();
        }

        return response(streamingOutput,
                requestContext.getMediaType(),
                requestContext.getLanguage(),
                includeLinkHeader ? links : null,
                targetCrs);
    }

    private Response response(Object entity, OgcApiMediaType mediaType, Optional<Locale> language,
                              List<OgcApiLink> links, EpsgCrs crs) {
        Response.ResponseBuilder response = Response.ok()
                                                    .entity(entity);

        if (mediaType != null)
            response.type(mediaType.type()
                                   .toString());

        if (language.isPresent())
            response.language(language.get());

        if (links != null)
            links.stream()
                 .forEach(link -> response.links(link.getLink()));

        if (crs != null)
            response.header("Content-Crs", "<"+crs.toUriString()+">");

        return response.build();
    }

    private StreamingOutput stream(FeatureStream2 featureTransformStream,
                                   final Function<OutputStream, FeatureTransformer2> featureTransformer) {
        Timer.Context timer = metricRegistry.timer(name(ObservationProcessingQueriesHandlerImpl.class, "stream"))
                                            .time();
        return outputStream -> {
            try {
                featureTransformStream.runWith(featureTransformer.apply(outputStream))
                                      .toCompletableFuture()
                                      .join();
                timer.stop();
            } catch (CompletionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException) e.getCause();
                }
                throw new IllegalStateException("Feature stream error", e.getCause());
            }
        };
    }

    public static void ensureCollectionIdExists(OgcApiApiDataV2 apiData, String collectionId) {
        if (!apiData.isCollectionEnabled(collectionId)) {
            throw new NotFoundException();
        }
    }

    private static void ensureFeatureProviderSupportsQueries(FeatureProvider2 featureProvider) {
        if (!featureProvider.supportsQueries()) {
            throw new IllegalStateException("feature provider does not support queries");
        }
    }
 */
}

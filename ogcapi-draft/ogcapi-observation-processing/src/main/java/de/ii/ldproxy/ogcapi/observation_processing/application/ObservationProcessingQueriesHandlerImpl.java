/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.application.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessChain;
import de.ii.ldproxy.ogcapi.features.processing.ImmutableProcessing;
import de.ii.ldproxy.ogcapi.features.processing.Processing;
import de.ii.ldproxy.ogcapi.observation_processing.api.*;
import de.ii.xtraplatform.akka.http.Http;
import de.ii.xtraplatform.codelists.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.entities.domain.EntityRegistry;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

@Component
@Instantiate
@Provides
public class ObservationProcessingQueriesHandlerImpl implements ObservationProcessingQueriesHandler {

    private static final String DAPA_PATH_ELEMENT = "dapa";
    private final I18n i18n;
    private final CrsTransformerFactory crsTransformerFactory;
    private final Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> queryHandlers;
    private final MetricRegistry metricRegistry;
    private final EntityRegistry entityRegistry;
    private final OgcApiFeatureCoreProviders providers;
    private final Http http;


    public ObservationProcessingQueriesHandlerImpl(@Requires I18n i18n,
                                                   @Requires CrsTransformerFactory crsTransformerFactory,
                                                   @Requires Dropwizard dropwizard,
                                                   @Requires EntityRegistry entityRegistry,
                                                   @Requires OgcApiFeatureCoreProviders providers,
                                                   @Requires Http http) {
        this.i18n = i18n;
        this.crsTransformerFactory = crsTransformerFactory;
        this.entityRegistry = entityRegistry;

        this.metricRegistry = dropwizard.getEnvironment()
                                        .metrics();
        this.providers = providers;
        this.http = http;

        this.queryHandlers = ImmutableMap.of(
                Query.PROCESS,
                OgcApiQueryHandler.with(OgcApiQueryInputObservationProcessing.class, this::getProcessResponse),
                Query.VARIABLES,
                OgcApiQueryHandler.with(OgcApiQueryInputVariables.class, this::getVariablesResponse),
                Query.LIST,
                OgcApiQueryHandler.with(OgcApiQueryInputProcessing.class, this::getProcessingResponse)
        );
    }

    @Override
    public Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    public static void ensureCollectionIdExists(OgcApiApiDataV2 apiData, String collectionId) {
        if (!apiData.isCollectionEnabled(collectionId)) {
            throw new NotFoundException(MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));
        }
    }

    private static void ensureFeatureProviderSupportsQueries(FeatureProvider2 featureProvider) {
        if (!featureProvider.supportsQueries()) {
            throw new IllegalStateException("Feature provider does not support queries.");
        }
    }

    private Response getVariablesResponse(OgcApiQueryInputVariables queryInput, OgcApiRequestContext requestContext) {
        OgcApiApi api = requestContext.getApi();
        OgcApiApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        if (!apiData.isCollectionEnabled(collectionId))
            throw new NotFoundException(MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));

        ObservationProcessingOutputFormatVariables outputFormat = api.getOutputFormat(
                ObservationProcessingOutputFormatVariables.class,
                requestContext.getMediaType(),
                "/collections/"+collectionId+"/"+DAPA_PATH_ELEMENT+"/variables")
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        ensureCollectionIdExists(api.getData(), collectionId);

        List<OgcApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();
        List<OgcApiLink> links =
                new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(), requestContext.getMediaType(), alternateMediaTypes, i18n, requestContext.getLanguage());

        Variables variables = ImmutableVariables.builder()
                .variables(queryInput.getVariables())
                .links(links)
                .build();

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(outputFormat.getEntity(variables, collectionId, api, requestContext))
                .build();
    }

    private Response getProcessingResponse(OgcApiQueryInputProcessing queryInput, OgcApiRequestContext requestContext) {
        OgcApiApi api = requestContext.getApi();
        OgcApiApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        if (!apiData.isCollectionEnabled(collectionId))
            throw new NotFoundException(MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));

        ObservationProcessingOutputFormatProcessing outputFormat = api.getOutputFormat(
                ObservationProcessingOutputFormatProcessing.class,
                requestContext.getMediaType(),
                "/collections/"+collectionId+"/"+DAPA_PATH_ELEMENT)
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        ensureCollectionIdExists(api.getData(), collectionId);

        final ObservationProcessingLinksGenerator linkGenerator = new ObservationProcessingLinksGenerator();
        List<OgcApiLink> links = linkGenerator.generateDapaLinks(requestContext.getUriCustomizer(), requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(), i18n, requestContext.getLanguage());

        Processing processing = ImmutableProcessing.builder()
                .from(queryInput.getProcessing())
                .links(links)
                .build();

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(outputFormat.getEntity(processing, collectionId, api, requestContext))
                .build();
    }

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
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        ensureCollectionIdExists(api.getData(), collectionId);
        ensureFeatureProviderSupportsQueries(featureProvider);

        Optional<CrsTransformer> crsTransformer = Optional.empty();
        boolean swapCoordinates = false;

        EpsgCrs targetCrs = query.getCrs().orElse(defaultCrs);
        if (featureProvider.supportsCrs()) {
            EpsgCrs sourceCrs = featureProvider.crs().getNativeCrs();
            //TODO: warmup on service start
            crsTransformer = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
            swapCoordinates = crsTransformer.isPresent() && crsTransformer.get()
                                                                          .needsCoordinateSwap();
        }

        List<OgcApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        // TODO add links
        List<OgcApiLink> links = ImmutableList.of();

        ImmutableFeatureTransformationContextObservationProcessing.Builder transformationContext = new ImmutableFeatureTransformationContextObservationProcessing.Builder()
                .apiData(api.getData())
                .featureSchema(featureProvider.getData().getTypes().get(collectionId))
                .i18n(i18n)
                .language(requestContext.getLanguage())
                .codelists(entityRegistry.getEntitiesForType(Codelist.class)
                                         .stream()
                                         .collect(Collectors.toMap(c -> c.getId(), c -> c)))
                .collectionId(collectionId)
                .ogcApiRequest(requestContext)
                .crsTransformer(crsTransformer)
                .defaultCrs(defaultCrs)
                .links(links)
                .isFeatureCollection(true)
                .isHitsOnly(query.hitsOnly())
                .isPropertyOnly(query.propertyOnly())
                .processes(queryInput.getProcesses())
                .processingParameters(queryInput.getProcessingParameters())
                .variables(queryInput.getVariables())
                .outputFormat(outputFormat)
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

            streamingOutput = stream(featureStream, outputStream -> outputFormat.getFeatureTransformer(transformationContext.outputStream(outputStream).build(), providers, http)
                    .get());
        } else {
            throw new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated, because it does not support streaming.", requestContext.getMediaType().type()));
        }

        return prepareSuccessResponse(api, requestContext, includeLinkHeader ? links : null, targetCrs)
                .entity(streamingOutput)
                .build();
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
                throw new IllegalStateException("Feature stream error.", e.getCause());
            }
        };
    }
}

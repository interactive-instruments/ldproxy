/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.api.FeatureStream;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.GmlConsumer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.immutables.value.Value;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static com.codahale.metrics.MetricRegistry.name;

@Component
@Instantiate
@Provides(specifications = {OgcApiFeaturesCoreQueriesHandler.class})
public class OgcApiFeaturesCoreQueriesHandler implements OgcApiQueriesHandler<OgcApiFeaturesCoreQueriesHandler.Query> {

    // TODO merge ldproxy-wfs3-core and ldproxy-wfs3-api to ldproxy-ogcapi-features-core

    public enum Query implements OgcApiQueryIdentifier {FEATURES, FEATURE}

    @Value.Immutable
    public interface OgcApiQueryInputFeatures extends OgcApiQueryInput {
        String getCollectionId();
        FeatureQuery getQuery();
        Optional<Integer> getDefaultPageSize();
    }

    @Value.Immutable
    public interface OgcApiQueryInputFeature extends OgcApiQueryInput {
        String getCollectionId();
        String getFeatureId();
        FeatureQuery getQuery();
    }

    private final OgcApiExtensionRegistry extensionRegistry;
    private final Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> queryHandlers;
    private final MetricRegistry metricRegistry;

    public OgcApiFeaturesCoreQueriesHandler(@Requires OgcApiExtensionRegistry extensionRegistry, @Requires Dropwizard dropwizard) {
        this.extensionRegistry = extensionRegistry;

        this.metricRegistry = dropwizard.getEnvironment()
                .metrics();

        this.queryHandlers = ImmutableMap.of(
                Query.FEATURES, OgcApiQueryHandler.with(OgcApiQueryInputFeatures.class, this::getItemsResponse),
                Query.FEATURE, OgcApiQueryHandler.with(OgcApiQueryInputFeature.class, this::getItemResponse)
        );
    }

    @Override
    public Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    public static void checkCollectionId(OgcApiDatasetData apiData, String collectionId) {
        if (!apiData.isFeatureTypeEnabled(collectionId)) {
            throw new NotFoundException();
        }
    }

    private <T extends FormatExtension> Optional<T> getOutputFormat(Class<T> extensionType, OgcApiMediaType mediaType, OgcApiDatasetData apiData, String path) {
        return extensionRegistry.getExtensionsForType(extensionType)
                .stream()
                .filter(outputFormatExtension -> path.matches(outputFormatExtension.getPathPattern()))
                .filter(outputFormatExtension -> mediaType.type().isCompatible(outputFormatExtension.getMediaType().type()))
                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
                .findFirst();
    }

    private Response getItemsResponse(OgcApiQueryInputFeatures queryInput, OgcApiRequestContext requestContext) {

        OgcApiDataset api = requestContext.getApi();
        OgcApiDatasetData apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        FeatureQuery query = queryInput.getQuery();
        Optional<Integer> defaultPageSize = queryInput.getDefaultPageSize();
        boolean onlyHitsIfMore = false; // TODO check

        OgcApiFeatureFormatExtension outputFormat = getOutputFormat(
                    OgcApiFeatureFormatExtension.class,
                    requestContext.getMediaType(),
                    apiData,
                    "/collections/"+collectionId+"/items")
                .orElseThrow(NotAcceptableException::new);

        return getItemsResponse(api, requestContext, collectionId, query, true, outputFormat, onlyHitsIfMore, defaultPageSize);
    }

    private Response getItemResponse(OgcApiQueryInputFeature queryInput,
                                           OgcApiRequestContext requestContext) {

        OgcApiDataset api = requestContext.getApi();
        OgcApiDatasetData apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        String featureId = queryInput.getFeatureId();
        FeatureQuery query = queryInput.getQuery();

        OgcApiFeatureFormatExtension outputFormat = getOutputFormat(
                OgcApiFeatureFormatExtension.class,
                requestContext.getMediaType(),
                apiData,
                "/collections/"+collectionId+"/items/"+featureId)
                .orElseThrow(NotAcceptableException::new);

        return getItemsResponse(api, requestContext, collectionId, query, false, outputFormat, false, Optional.empty());
    }

    private Response getItemsResponse(OgcApiDataset api, OgcApiRequestContext requestContext, String collectionId,
                                      FeatureQuery query, boolean isCollection, OgcApiFeatureFormatExtension outputFormat,
                                      boolean onlyHitsIfMore, Optional<Integer> defaultPageSize) {
        checkCollectionId(api.getData(), collectionId);
        Optional<CrsTransformer> crsTransformer = api.getCrsTransformer(query.getCrs());
        List<OgcApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        boolean swapCoordinates = crsTransformer.isPresent() ? crsTransformer.get().needsCoordinateSwap() : api.getFeatureProvider()
                .shouldSwapCoordinates(query.getCrs());

        List<OgcApiLink> links =
                isCollection ?
                        new FeaturesLinksGenerator().generateLinks(requestContext.getUriCustomizer(), query.getOffset(), query.getLimit(), defaultPageSize.orElse(0), requestContext.getMediaType(), alternateMediaTypes):
                        new FeatureLinksGenerator().generateLinks(requestContext.getUriCustomizer(), requestContext.getMediaType(), alternateMediaTypes);

        ImmutableFeatureTransformationContextGeneric.Builder transformationContext = new ImmutableFeatureTransformationContextGeneric.Builder()
                .apiData(api.getData())
                .collectionId(collectionId)
                .ogcApiRequest(requestContext)
                .crsTransformer(crsTransformer)
                .links(links)
                .isFeatureCollection(isCollection)
                .isHitsOnly(query.hitsOnly())
                .isPropertyOnly(query.propertyOnly())
                .fields(query.getFields())
                .limit(query.getLimit())
                .offset(query.getOffset())
                .maxAllowableOffset(query.getMaxAllowableOffset())
                .geometryPrecision(query.getGeometryPrecision())
                .shouldSwapCoordinates(swapCoordinates)
                .isHitsOnlyIfMore(onlyHitsIfMore);

        StreamingOutput streamingOutput;
        if (requestContext.getMediaType()
                .matches(MediaType.valueOf(api.getFeatureProvider()
                        .getSourceFormat()))
                && outputFormat.canPassThroughFeatures()) {
            FeatureStream<GmlConsumer> featureStream = api.getFeatureProvider()
                    .getFeatureStream(query);

            streamingOutput = stream2(featureStream, outputStream -> outputFormat.getFeatureConsumer(transformationContext.outputStream(outputStream)
                    .build())
                    .get());
        } else if (outputFormat.canTransformFeatures()) {
            FeatureStream<FeatureTransformer> featureTransformStream = api.getFeatureProvider()
                    .getFeatureTransformStream(query);

            streamingOutput = stream(featureTransformStream, outputStream -> outputFormat.getFeatureTransformer(transformationContext.outputStream(outputStream)
                    .build())
                    .get());
        } else {
            throw new NotAcceptableException();
        }

        return response(streamingOutput, requestContext.getMediaType()
                .type()
                .toString());
    }

    private Response response(Object entity, String type) {
        Response.ResponseBuilder response = Response.ok()
                .entity(entity);
        if (type != null) {
            response.type(type);
        }

        return response.build();
    }

    private StreamingOutput stream(FeatureStream<FeatureTransformer> featureTransformStream,
                                   final Function<OutputStream, FeatureTransformer> featureTransformer) {
        Timer.Context timer = metricRegistry.timer(name(OgcApiFeaturesCoreQueriesHandler.class, "stream"))
                .time();
        Timer.Context timer2 = metricRegistry.timer(name(OgcApiFeaturesCoreQueriesHandler.class, "wait"))
                .time();

        return outputStream -> {
            try {
                featureTransformStream.apply(featureTransformer.apply(outputStream), timer2)
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

    private StreamingOutput stream2(FeatureStream<GmlConsumer> featureTransformStream,
                                    final Function<OutputStream, GmlConsumer> featureTransformer) {
        return outputStream -> {
            try {
                featureTransformStream.apply(featureTransformer.apply(outputStream), null)
                        .toCompletableFuture()
                        .join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException) e.getCause();
                }
                throw new IllegalStateException("Feature stream error", e.getCause());
            }
        };
    }
}

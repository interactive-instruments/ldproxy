/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.api;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.wfs3.templates.StringTemplateFilters;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.feature.provider.api.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.immutables.value.Value;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static com.codahale.metrics.MetricRegistry.name;

@Component
@Instantiate
@Provides(specifications = {OgcApiFeaturesCoreQueriesHandler.class})
public class OgcApiFeaturesCoreQueriesHandler implements OgcApiQueriesHandler<OgcApiFeaturesCoreQueriesHandler.Query> {

    @Requires
    I18n i18n;

    public enum Query implements OgcApiQueryIdentifier {FEATURES, FEATURE}

    @Value.Immutable
    public interface OgcApiQueryInputFeatures extends OgcApiQueryInput {
        String getCollectionId();

        FeatureQuery getQuery();

        Optional<Integer> getDefaultPageSize();

        boolean getShowsFeatureSelfLink();

        boolean getIncludeHomeLink();

        boolean getIncludeLinkHeader();
    }

    @Value.Immutable
    public interface OgcApiQueryInputFeature extends OgcApiQueryInput {
        String getCollectionId();

        String getFeatureId();

        FeatureQuery getQuery();

        boolean getIncludeHomeLink();

        boolean getIncludeLinkHeader();
    }

    private final OgcApiExtensionRegistry extensionRegistry;
    private final Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> queryHandlers;
    private final MetricRegistry metricRegistry;

    public OgcApiFeaturesCoreQueriesHandler(@Requires OgcApiExtensionRegistry extensionRegistry,
                                            @Requires Dropwizard dropwizard) {
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

    public static void ensureCollectionIdExists(OgcApiDatasetData apiData, String collectionId) {
        if (!apiData.isFeatureTypeEnabled(collectionId)) {
            throw new NotFoundException();
        }
    }

    private static void ensureFeatureProviderSupportsQueries(FeatureProvider2 featureProvider) {
        if (!featureProvider.supportsQueries()) {
            throw new IllegalStateException("feature provider does not support queries");
        }
    }

    private Response getItemsResponse(OgcApiQueryInputFeatures queryInput, OgcApiRequestContext requestContext) {

        OgcApiDataset api = requestContext.getApi();
        OgcApiDatasetData apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        FeatureQuery query = queryInput.getQuery();
        Optional<Integer> defaultPageSize = queryInput.getDefaultPageSize();
        boolean onlyHitsIfMore = false; // TODO check

        OgcApiFeatureFormatExtension outputFormat = api.getOutputFormat(
                    OgcApiFeatureFormatExtension.class,
                    requestContext.getMediaType(),
                    "/collections/"+collectionId+"/items")
                .orElseThrow(NotAcceptableException::new);

        return getItemsResponse(api, requestContext, collectionId, query, true, null, outputFormat, onlyHitsIfMore, defaultPageSize,
                queryInput.getIncludeHomeLink(), queryInput.getShowsFeatureSelfLink(), queryInput.getIncludeLinkHeader());
    }

    private Response getItemResponse(OgcApiQueryInputFeature queryInput,
                                           OgcApiRequestContext requestContext) {

        OgcApiDataset api = requestContext.getApi();
        OgcApiDatasetData apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        String featureId = queryInput.getFeatureId();
        FeatureQuery query = queryInput.getQuery();

        OgcApiFeatureFormatExtension outputFormat = api.getOutputFormat(
                OgcApiFeatureFormatExtension.class,
                requestContext.getMediaType(),
                "/collections/"+collectionId+"/items/"+featureId)
                .orElseThrow(NotAcceptableException::new);

        String persistentUri = null;
        Optional<String> template = api.getData().getFeatureTypes().get(collectionId).getPersistentUriTemplate();
        if (template.isPresent()) {
            persistentUri = StringTemplateFilters.applyTemplate(template.get(), featureId);
        }

        return getItemsResponse(api, requestContext, collectionId, query, false, persistentUri, outputFormat, false, Optional.empty(),
                queryInput.getIncludeHomeLink(), false, queryInput.getIncludeLinkHeader());
    }

    private Response getItemsResponse(OgcApiDataset api, OgcApiRequestContext requestContext, String collectionId,
                                      FeatureQuery query, boolean isCollection, String canonicalUri,
                                      OgcApiFeatureFormatExtension outputFormat,
                                      boolean onlyHitsIfMore, Optional<Integer> defaultPageSize,
                                      boolean includeHomeLink,
                                      boolean showsFeatureSelfLink, boolean includeLinkHeader) {

        ensureCollectionIdExists(api.getData(), collectionId);
        FeatureProvider2 featureProvider = api.getFeatureProvider();
        ensureFeatureProviderSupportsQueries(featureProvider);
        Optional<CrsTransformer> crsTransformer = api.getCrsTransformer(query.getCrs());
        List<OgcApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        boolean swapCoordinates = crsTransformer.isPresent() ? crsTransformer.get()
                                                                             .needsCoordinateSwap() : featureProvider.shouldSwapCoordinates(query.getCrs());

        List<OgcApiLink> links =
                isCollection ?
                        new FeaturesLinksGenerator().generateLinks(requestContext.getUriCustomizer(), query.getOffset(), query.getLimit(), defaultPageSize.orElse(0), requestContext.getMediaType(), alternateMediaTypes, includeHomeLink, i18n, requestContext.getLanguage()):
                        new FeatureLinksGenerator().generateLinks(requestContext.getUriCustomizer(), requestContext.getMediaType(), alternateMediaTypes, outputFormat.getCollectionMediaType(), canonicalUri, includeHomeLink, i18n, requestContext.getLanguage());

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
                .isHitsOnlyIfMore(onlyHitsIfMore)
                .showsFeatureSelfLink(showsFeatureSelfLink);

        StreamingOutput streamingOutput;

        if (outputFormat.canPassThroughFeatures() && featureProvider.supportsPassThrough() && outputFormat.getMediaType()
                                                                                                          .matches(featureProvider.passThrough()
                                                                                                                                  .getMediaType())) {
            FeatureSourceStream featureStream = featureProvider.passThrough()
                                                          .getFeatureSourceStream(query);

            streamingOutput = stream2(featureStream, outputStream -> outputFormat.getFeatureConsumer(transformationContext.outputStream(outputStream)
                    .build())
                    .get());
        } else if (outputFormat.canTransformFeatures()) {
            FeatureStream2 featureStream = featureProvider.queries()
                                                          .getFeatureStream2(query);

            streamingOutput = stream(featureStream, outputStream -> outputFormat.getFeatureTransformer(transformationContext.outputStream(outputStream)
                    .build(), requestContext.getLanguage())
                    .get());
        } else {
            throw new NotAcceptableException();
        }

        // TODO add Content-Crs header
        // TODO determine numberMatched, numberReturned and optionally return them as OGC-numberMatched and OGC-numberReturned headers

        return response(streamingOutput,
                        requestContext.getMediaType(),
                        requestContext.getLanguage(),
                        includeLinkHeader ? links : null);
    }

    private Response response(Object entity, OgcApiMediaType mediaType, Optional<Locale> language,
                              List<OgcApiLink> links) {
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

        return response.build();
    }

    private StreamingOutput stream(FeatureStream2 featureTransformStream,
                                   final Function<OutputStream, FeatureTransformer> featureTransformer) {
        Timer.Context timer = metricRegistry.timer(name(OgcApiFeaturesCoreQueriesHandler.class, "stream"))
                .time();
        Timer.Context timer2 = metricRegistry.timer(name(OgcApiFeaturesCoreQueriesHandler.class, "wait"))
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

    private StreamingOutput stream2(FeatureSourceStream featureTransformStream,
                                    final Function<OutputStream, FeatureConsumer> featureTransformer) {
        return outputStream -> {
            try {
                featureTransformStream.runWith(featureTransformer.apply(outputStream))
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

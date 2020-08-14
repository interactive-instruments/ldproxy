/**
 * Copyright 2020 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.application;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryHandler;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryInput;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureLinksGenerator;
import de.ii.ldproxy.ogcapi.features.core.api.FeaturesLinksGenerator;
import de.ii.ldproxy.ogcapi.features.core.api.ImmutableFeatureTransformationContextGeneric;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeaturesCoreQueriesHandler;
import de.ii.xtraplatform.codelists.CodelistRegistry;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.features.domain.*;
import de.ii.xtraplatform.stringtemplates.StringTemplateFilters;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;
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

import static com.codahale.metrics.MetricRegistry.name;

@Component
@Instantiate
@Provides
public class OgcApiFeaturesCoreQueriesHandlerImpl implements OgcApiFeaturesCoreQueriesHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiFeaturesCoreQueriesHandlerImpl.class);

    private final I18n i18n;
    private final CrsTransformerFactory crsTransformerFactory;
    private final Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> queryHandlers;
    private final MetricRegistry metricRegistry;
    private final CodelistRegistry codelistRegistry;

    public OgcApiFeaturesCoreQueriesHandlerImpl(@Requires I18n i18n,
                                                @Requires CrsTransformerFactory crsTransformerFactory,
                                                @Requires Dropwizard dropwizard,
                                                @Requires CodelistRegistry codelistRegistry) {
        this.i18n = i18n;
        this.crsTransformerFactory = crsTransformerFactory;
        this.codelistRegistry = codelistRegistry;

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

    private Response getItemsResponse(OgcApiQueryInputFeatures queryInput, OgcApiRequestContext requestContext) {

        OgcApiApi api = requestContext.getApi();
        OgcApiApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        FeatureQuery query = queryInput.getQuery();
        Optional<Integer> defaultPageSize = queryInput.getDefaultPageSize();
        boolean onlyHitsIfMore = false; // TODO check

        OgcApiFeatureFormatExtension outputFormat = api.getOutputFormat(
                OgcApiFeatureFormatExtension.class,
                requestContext.getMediaType(),
                "/collections/" + collectionId + "/items")
                                                       .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        return getItemsResponse(api, requestContext, collectionId, query, queryInput.getFeatureProvider(), true, null, outputFormat, onlyHitsIfMore, defaultPageSize,
                queryInput.getIncludeHomeLink(), queryInput.getShowsFeatureSelfLink(), queryInput.getIncludeLinkHeader(), queryInput.getDefaultCrs());
    }

    private Response getItemResponse(OgcApiQueryInputFeature queryInput,
                                     OgcApiRequestContext requestContext) {

        OgcApiApi api = requestContext.getApi();
        OgcApiApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        String featureId = queryInput.getFeatureId();
        FeatureQuery query = queryInput.getQuery();

        OgcApiFeatureFormatExtension outputFormat = api.getOutputFormat(
                OgcApiFeatureFormatExtension.class,
                requestContext.getMediaType(),
                "/collections/" + collectionId + "/items/" + featureId)
                                                       .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        String persistentUri = null;
        Optional<String> template = api.getData()
                                       .getCollections()
                                       .get(collectionId)
                                       .getPersistentUriTemplate();
        if (template.isPresent()) {
            persistentUri = StringTemplateFilters.applyTemplate(template.get(), featureId);
        }

        return getItemsResponse(api, requestContext, collectionId, query, queryInput.getFeatureProvider(), false, persistentUri, outputFormat, false, Optional.empty(),
                queryInput.getIncludeHomeLink(), false, queryInput.getIncludeLinkHeader(), queryInput.getDefaultCrs());
    }

    private Response getItemsResponse(OgcApiApi api, OgcApiRequestContext requestContext, String collectionId,
                                      FeatureQuery query, FeatureProvider2 featureProvider, boolean isCollection,
                                      String canonicalUri,
                                      OgcApiFeatureFormatExtension outputFormat,
                                      boolean onlyHitsIfMore, Optional<Integer> defaultPageSize,
                                      boolean includeHomeLink,
                                      boolean showsFeatureSelfLink, boolean includeLinkHeader,
                                      EpsgCrs defaultCrs) {

        ensureCollectionIdExists(api.getData(), collectionId);
        ensureFeatureProviderSupportsQueries(featureProvider);

        Optional<CrsTransformer> crsTransformer = Optional.empty();
        boolean swapCoordinates = false;

        EpsgCrs targetCrs = query.getCrs()
                                 .orElse(defaultCrs);
        if (featureProvider.supportsCrs()) {
            EpsgCrs sourceCrs = featureProvider.crs()
                                               .getNativeCrs();
            //TODO: warmup on service start
            crsTransformer = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
            swapCoordinates = crsTransformer.isPresent() ? crsTransformer.get()
                                                                         .needsCoordinateSwap() : query.getCrs()
                                                                                                       .isPresent() && featureProvider.crs()
                                                                                                                                      .shouldSwapCoordinates(query.getCrs()
                                                                                                                                                                  .get());
        }


        List<OgcApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        List<OgcApiLink> links =
                isCollection ?
                        new FeaturesLinksGenerator().generateLinks(requestContext.getUriCustomizer(), query.getOffset(), query.getLimit(), defaultPageSize.orElse(0), requestContext.getMediaType(), alternateMediaTypes, includeHomeLink, i18n, requestContext.getLanguage()) :
                        new FeatureLinksGenerator().generateLinks(requestContext.getUriCustomizer(), requestContext.getMediaType(), alternateMediaTypes, outputFormat.getCollectionMediaType(), canonicalUri, includeHomeLink, i18n, requestContext.getLanguage());

        ImmutableFeatureTransformationContextGeneric.Builder transformationContext = new ImmutableFeatureTransformationContextGeneric.Builder()
                .apiData(api.getData())
                .featureSchema(featureProvider.getData().getTypes().get(collectionId))
                .collectionId(collectionId)
                .ogcApiRequest(requestContext)
                .crsTransformer(crsTransformer)
                .codelists(codelistRegistry.getCodelists())
                .defaultCrs(defaultCrs)
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
            FeatureSourceStream<?> featureStream = featureProvider.passThrough()
                                                                  .getFeatureSourceStream(query);

            streamingOutput = stream2(featureStream, !isCollection, outputStream -> outputFormat.getFeatureConsumer(transformationContext.outputStream(outputStream)
                                                                                                                                         .build())
                                                                                                .get());
        } else if (outputFormat.canTransformFeatures()) {
            FeatureStream2 featureStream = featureProvider.queries()
                                                          .getFeatureStream2(query);

            streamingOutput = stream(featureStream, !isCollection, outputStream -> outputFormat.getFeatureTransformer(transformationContext.outputStream(outputStream)
                                                                                                                                           .build(), requestContext.getLanguage())
                                                                                               .get());
        } else {
            throw new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated, because it does not support streaming.", requestContext.getMediaType().type()));
        }

        // TODO determine numberMatched, numberReturned and optionally return them as OGC-numberMatched and OGC-numberReturned headers
        // TODO For now remove the "next" links from the headers since at this point we don't know, whether there will be a next page

        return prepareSuccessResponse(api, requestContext, includeLinkHeader ? links.stream()
                                                                                    .filter(link -> !"next".equalsIgnoreCase(link.getRel()))
                                                                                    .collect(ImmutableList.toImmutableList()) :
                                                                                null, targetCrs)
                .entity(streamingOutput)
                .build();
    }

    private StreamingOutput stream(FeatureStream2 featureTransformStream, boolean failIfEmpty,
                                   final Function<OutputStream, FeatureTransformer2> featureTransformer) {
        Timer.Context timer = metricRegistry.timer(name(OgcApiFeaturesCoreQueriesHandlerImpl.class, "stream"))
                                            .time();

        return outputStream -> {
            try {
                FeatureStream2.Result result = featureTransformStream.runWith(featureTransformer.apply(outputStream))
                                                                     .toCompletableFuture()
                                                                     .join();
                timer.stop();

                if (result.getError()
                          .isPresent()) {
                    throw new InternalServerErrorException(result.getError().get().getMessage());
                }

                if (result.isEmpty() && failIfEmpty) {
                    throw new InternalServerErrorException("The feature stream returned an invalid empty response.");
                }

            } catch (CompletionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException) e.getCause();
                }
                throw new IllegalStateException("Feature stream error.", e.getCause());
            }
        };
    }

    private StreamingOutput stream2(FeatureSourceStream<?> featureTransformStream, boolean failIfEmpty,
                                    final Function<OutputStream, FeatureConsumer> featureTransformer) {
        return outputStream -> {
            try {
                FeatureStream2.Result result = featureTransformStream.runWith(featureTransformer.apply(outputStream))
                                                                     .toCompletableFuture()
                                                                     .join();

                if (result.getError()
                          .isPresent()) {
                    throw new InternalServerErrorException(result.getError().get().getMessage());
                }

                if (result.isEmpty() && failIfEmpty) {
                    throw new InternalServerErrorException("The feature stream returned an invalid empty response.");
                }
            } catch (CompletionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException) e.getCause();
                }
                throw new IllegalStateException("Feature stream error.", e.getCause());
            }
        };
    }
}

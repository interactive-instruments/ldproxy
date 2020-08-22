/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.transactional;

import akka.Done;
import akka.japi.function.Creator;
import akka.stream.IOResult;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.xtraplatform.features.geojson.domain.FeatureDecoderGeoJson;
import de.ii.xtraplatform.features.domain.FeatureDecoder;
import de.ii.xtraplatform.features.domain.FeatureTransactions;
import de.ii.xtraplatform.features.domain.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.features.geojson.domain.GeoJsonStreamParser;
import de.ii.xtraplatform.features.geojson.domain.MappingSwapper;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class CommandHandlerTransactional {


    public Response postItemsResponse(
            FeatureTransactions featureProvider,
            ApiMediaType mediaType, URICustomizer uriCustomizer, String collectionName,
            InputStream requestBody) {

        FeatureDecoder.WithSource featureSource = getFeatureSource(mediaType, requestBody);

        //TODO: collectionName != featureType
        FeatureTransactions.MutationResult result = featureProvider.createFeatures(collectionName, featureSource);

        if (result.getError().isPresent()) {
            //TODO: see FeaturesCoreQueryHandler
            throw new RuntimeException(result.getError().get());
        }

        List<String> ids = result.getIds();
        //List<String> ids = featureProvider.addFeaturesFromStream(collectionName, defaultReverseTransformer, getFeatureTransformStream(mediaType, featureTypeMapping, requestBody));



        if (ids.isEmpty()) {
            throw new IllegalArgumentException("No features found in input");
        }
        URI firstFeature = null;
        try {
            firstFeature = uriCustomizer.copy()
                                        .ensureLastPathSegment(ids.get(0))
                                        .build();
        } catch (URISyntaxException e) {
            //ignore
        }

        return Response.created(firstFeature)
                       .build();
    }

    public Response putItemResponse(
            FeatureTransactions featureProvider,
            ApiMediaType mediaType, String collectionName, String featureId,
            InputStream requestBody) {

        FeatureDecoder.WithSource featureSource = getFeatureSource(mediaType, requestBody);

        //TODO: collectionName != featureType
        FeatureTransactions.MutationResult result = featureProvider.updateFeature(collectionName, featureSource, featureId);

        if (result.getError().isPresent()) {
            //TODO: see FeaturesCoreQueryHandler
            throw new RuntimeException(result.getError().get());
        }

        return Response.noContent()
                       .build();
    }

    public Response deleteItemResponse(
            FeatureTransactions featureProvider,
            String collectionName, String featureId) {

        FeatureTransactions.MutationResult result = featureProvider.deleteFeature(collectionName, featureId);

        if (result.getError().isPresent()) {
            //TODO: see FeaturesCoreQueryHandler
            throw new RuntimeException(result.getError().get());
        }

        return Response.noContent()
                       .build();
    }

    private FeatureDecoder.WithSource getFeatureSource(ApiMediaType mediaType, InputStream requestBody) {

        //TODO: to inputformat extension, for the time being make it static
        FeatureDecoderGeoJson featureDecoderGeoJson = new FeatureDecoderGeoJson();

        Source<ByteString, CompletionStage<IOResult>> source = StreamConverters.fromInputStream((Creator<InputStream>) () -> requestBody);

        return featureDecoderGeoJson.withSource(source);
    }

    // TODO
    private Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> getFeatureTransformStream(
            ApiMediaType mediaType, FeatureTypeMapping featureTypeMapping, InputStream requestBody) {
        return featureTransformer -> {
            MappingSwapper mappingSwapper = new MappingSwapper();
            Sink<ByteString, CompletionStage<Done>> transformer = GeoJsonStreamParser.transform(mappingSwapper.swapMapping(featureTypeMapping, "SQL"), featureTransformer);
            return StreamConverters.fromInputStream((Creator<InputStream>) () -> requestBody)
                                   .toMat(transformer, Keep.right());

            //return CompletableFuture.completedFuture(Done.getInstance());
        };
    }
}

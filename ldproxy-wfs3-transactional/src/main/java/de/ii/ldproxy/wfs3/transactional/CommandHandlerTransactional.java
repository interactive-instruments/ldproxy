/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.transactional;

import akka.Done;
import akka.japi.function.Creator;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.TransformingFeatureProvider;
import de.ii.xtraplatform.feature.transformer.geojson.GeoJsonStreamParser;
import de.ii.xtraplatform.feature.transformer.geojson.MappingSwapper;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class CommandHandlerTransactional {


    public Response postItemsResponse(
            TransformingFeatureProvider featureProvider,
            OgcApiMediaType mediaType, URICustomizer uriCustomizer, String collectionName,
            FeatureTypeMapping featureTypeMapping, CrsTransformer defaultReverseTransformer,
            InputStream requestBody) {
        List<String> ids = featureProvider.addFeaturesFromStream(collectionName, defaultReverseTransformer, getFeatureTransformStream(mediaType, featureTypeMapping, requestBody));

        if (ids.isEmpty()) {
            throw new BadRequestException("No features found in input");
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
            TransformingFeatureProvider featureProvider,
            OgcApiMediaType mediaType, String collectionName, String featureId,
            FeatureTypeMapping featureTypeMapping, CrsTransformer defaultReverseTransformer,
            InputStream requestBody) {
        featureProvider.updateFeatureFromStream(collectionName, featureId, defaultReverseTransformer, getFeatureTransformStream(mediaType, featureTypeMapping, requestBody));

        return Response.noContent()
                       .build();
    }

    public Response deleteItemResponse(
            TransformingFeatureProvider featureProvider,
            String collectionName, String featureId) {
        featureProvider.deleteFeature(collectionName, featureId);

        return Response.noContent()
                       .build();
    }

    // TODO
    private Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> getFeatureTransformStream(
            OgcApiMediaType mediaType, FeatureTypeMapping featureTypeMapping, InputStream requestBody) {
        return featureTransformer -> {
            MappingSwapper mappingSwapper = new MappingSwapper();
            Sink<ByteString, CompletionStage<Done>> transformer = null;//TODO GeoJsonStreamParser.transform(mappingSwapper.swapMapping(featureTypeMapping, "SQL"), featureTransformer);
            return StreamConverters.fromInputStream((Creator<InputStream>) () -> requestBody)
                                   .toMat(transformer, Keep.right());

            //return CompletableFuture.completedFuture(Done.getInstance());
        };
    }
}

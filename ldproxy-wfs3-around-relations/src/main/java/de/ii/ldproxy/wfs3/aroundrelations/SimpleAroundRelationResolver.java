/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.aroundrelations;

import akka.NotUsed;
import akka.japi.function.Function2;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import de.ii.xtraplatform.akka.http.AkkaHttp;

/**
 * @author zahnen
 */
public class SimpleAroundRelationResolver implements AroundRelationResolver {

    private final AkkaHttp akkaHttp;

    public SimpleAroundRelationResolver(/*@Requires*/ AkkaHttp akkaHttp) {
        this.akkaHttp = akkaHttp;
    }

    @Override
    public String resolve(AroundRelationsQuery.AroundRelationQuery aroundRelationQuery) {
        return resolve(aroundRelationQuery, "");
    }

    @Override
    public String resolve(AroundRelationsQuery.AroundRelationQuery aroundRelationQuery, String additionalParameters) {
        String url = getUrl(aroundRelationQuery, additionalParameters);

        StringBuilder response = new StringBuilder();

        Source<ByteString, NotUsed> source = akkaHttp.get(url);

        source
                .runWith(Sink.fold(response, (Function2<StringBuilder, ByteString, StringBuilder>) (stringBuilder, byteString) -> stringBuilder.append(byteString.utf8String())), akkaHttp.getMaterializer())
                .toCompletableFuture()
                .join();


        return response.toString();
    }

    @Override
    public String getUrl(AroundRelationsQuery.AroundRelationQuery aroundRelationQuery, String additionalParameters) {
        return aroundRelationQuery.configuration.getUrlTemplate()
                                                .replace("{{bbox}}", aroundRelationQuery.getBbox())
                                                .replace("{{limit}}", Integer.toString(aroundRelationQuery.limit))
                                                .replace("{{offset}}", Integer.toString(aroundRelationQuery.offset)) + additionalParameters;
    }
}

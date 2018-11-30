/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.filtertransformer;

import akka.NotUsed;
import akka.japi.function.Function2;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.akka.http.AkkaHttp;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author zahnen
 */
public class RequestGeoJsonBboxTransformer implements FilterTransformer {

    private final RequestGeoJsonBboxConfiguration configuration;
    private final AkkaHttp akkaHttp;

    public RequestGeoJsonBboxTransformer(RequestGeoJsonBboxConfiguration configuration, AkkaHttp akkaHttp) {
        this.configuration = configuration;
        this.akkaHttp = akkaHttp;
    }


    @Override
    public Map<String, String> resolveParameters(Map<String, String> parameters) {

        Map<String, String> nextParameters = new HashMap<>();
        Map<String, String> resolvableParameters = new HashMap<>();

        parameters.keySet()
                  .forEach(key -> {
                      if (configuration.getParameters()
                                       .contains(key)) {
                          resolvableParameters.put(key, parameters.get(key));
                      } else {
                          nextParameters.put(key, parameters.get(key));
                      }
                  });

        if (!resolvableParameters.isEmpty()) {
            try {
                nextParameters.putAll(resolve(resolvableParameters));
            } catch (ParseException e) {
                // ignore
            }
        }

        return nextParameters;
    }

    private Map<String, String> resolve(Map<String, String> resolvableParameters) throws ParseException {

        //TODO get json see SimpleAroundRelationResolver
        //TODO parse bbox JTS GeoJsonReader??? or https://github.com/bjornharrtell/jts2geojson???
        //TODO expand with explicit if given

        String url = getUrl(resolvableParameters);

        StringBuilder response = new StringBuilder();

        Source<ByteString, NotUsed> source = akkaHttp.get(url);

        source.runWith(Sink.fold(response, (Function2<StringBuilder, ByteString, StringBuilder>) (stringBuilder, byteString) -> stringBuilder.append(byteString.utf8String())), akkaHttp.getMaterializer())
              .toCompletableFuture()
              .join();

        GeoJsonReader geoJsonReader = new GeoJsonReader();
        Geometry geometry = geoJsonReader.read(response.toString());
        Envelope envelope = geometry.getEnvelopeInternal();
        //TODO bufferInMeters
        envelope.expandBy(0.0001);

        String bbox = String.format(Locale.US, "%f,%f,%f,%f", envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());

        return ImmutableMap.of("bbox", bbox);
    }

    private String getUrl(Map<String, String> resolvableParameters) {

        String url = configuration.getUrlTemplate();

        for (String parameter : configuration.getParameters()) {
            if (resolvableParameters.containsKey(parameter)) {
                url = url.replaceAll("\\{\\{" + parameter + "}}", resolvableParameters.get(parameter));
            } else {
                url = url.replaceAll("\\{\\{" + parameter + "}}", "''");
            }
        }


        return url;
    }
}

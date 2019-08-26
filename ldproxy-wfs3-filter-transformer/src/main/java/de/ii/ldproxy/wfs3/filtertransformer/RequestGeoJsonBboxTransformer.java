/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.filtertransformer;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.akka.http.HttpClient;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author zahnen
 */
public class RequestGeoJsonBboxTransformer implements FilterTransformer {

    private final RequestGeoJsonBboxConfiguration configuration;
    private final HttpClient httpClient;

    public RequestGeoJsonBboxTransformer(RequestGeoJsonBboxConfiguration configuration, HttpClient httpClient) {
        this.configuration = configuration;
        this.httpClient = httpClient;
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

        //TODO expand with explicit if given

        String url = getUrl(resolvableParameters);

        String response = httpClient.getAsString(url);

        GeoJsonReader geoJsonReader = new GeoJsonReader();
        Geometry geometry = geoJsonReader.read(response);
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
                url = url.replaceAll("\\{\\{" + parameter + "}}", URLEncoder.encode(resolvableParameters.get(parameter)));
            } else {
                url = url.replaceAll("\\{\\{" + parameter + "}}", "''");
            }
        }


        return url;
    }
}

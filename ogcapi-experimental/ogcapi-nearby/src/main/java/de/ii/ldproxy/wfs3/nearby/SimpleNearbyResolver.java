/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.nearby;

import de.ii.xtraplatform.akka.http.HttpClient;

/**
 * @author zahnen
 */
public class SimpleNearbyResolver implements NearbyResolver {

    private final HttpClient httpClient;

    public SimpleNearbyResolver(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String resolve(NearbyQuery.AroundRelationQuery aroundRelationQuery) {
        return resolve(aroundRelationQuery, "");
    }

    @Override
    public String resolve(NearbyQuery.AroundRelationQuery aroundRelationQuery, String additionalParameters) {
        String url = getUrl(aroundRelationQuery, additionalParameters);

        return httpClient.getAsString(url);
    }

    @Override
    public String getUrl(NearbyQuery.AroundRelationQuery aroundRelationQuery, String additionalParameters) {
        return aroundRelationQuery.configuration.getUrlTemplate()
                                                .replace("{{bbox}}", aroundRelationQuery.getBbox())
                                                .replace("{{limit}}", Integer.toString(aroundRelationQuery.limit))
                                                .replace("{{offset}}", Integer.toString(aroundRelationQuery.offset)) + additionalParameters;
    }
}

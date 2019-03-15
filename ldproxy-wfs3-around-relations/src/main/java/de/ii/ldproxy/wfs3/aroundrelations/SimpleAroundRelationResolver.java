/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.aroundrelations;

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

        return akkaHttp.getAsString(url);
    }

    @Override
    public String getUrl(AroundRelationsQuery.AroundRelationQuery aroundRelationQuery, String additionalParameters) {
        return aroundRelationQuery.configuration.getUrlTemplate()
                                                .replace("{{bbox}}", aroundRelationQuery.getBbox())
                                                .replace("{{limit}}", Integer.toString(aroundRelationQuery.limit))
                                                .replace("{{offset}}", Integer.toString(aroundRelationQuery.offset)) + additionalParameters;
    }
}

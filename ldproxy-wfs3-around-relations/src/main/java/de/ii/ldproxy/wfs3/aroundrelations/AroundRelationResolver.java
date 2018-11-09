/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.aroundrelations;

/**
 * @author zahnen
 */
public interface AroundRelationResolver {
    String resolve(AroundRelationsQuery.AroundRelationQuery aroundRelationQuery);

    String getUrl(AroundRelationsQuery.AroundRelationQuery aroundRelationQuery, String additionalParameters);

    String resolve(AroundRelationsQuery.AroundRelationQuery aroundRelationQuery, String additionalParameters);
}

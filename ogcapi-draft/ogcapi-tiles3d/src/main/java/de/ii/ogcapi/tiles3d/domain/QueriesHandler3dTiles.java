/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.domain;

import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.QueryIdentifier;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import java.net.URI;
import java.util.List;
import org.immutables.value.Value;

public interface QueriesHandler3dTiles extends QueriesHandler<QueriesHandler3dTiles.Query> {

  enum Query implements QueryIdentifier {
    TILESET,
    CONTENT,
    SUBTREE
  }

  @Value.Immutable
  interface QueryInputTileset extends QueryInput {
    String getCollectionId();

    int getMaxLevel();

    Float getGeometricErrorRoot();
  }

  @Value.Immutable
  interface QueryInputContent extends QueryInput {
    String getCollectionId();

    int getLevel();

    int getX();

    int getY();

    byte[] getContent();
  }

  @Value.Immutable
  interface QueryInputSubtree extends QueryInput {
    OgcApi getApi();

    FeatureProvider2 getFeatureProvider();

    String getFeatureType();

    String getGeometryProperty();

    URI getServicesUri();

    String getCollectionId();

    int getLevel();

    int getX();

    int getY();

    int getSubtreeLevels();

    int getFirstLevelWithContent();

    int getMaxLevel();

    List<Cql2Expression> getContentFilters();

    List<Cql2Expression> getTileFilters();
  }
}

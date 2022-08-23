/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryIdentifier;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

public interface FeaturesCoreQueriesHandler
    extends QueriesHandler<FeaturesCoreQueriesHandler.Query> {

  @Override
  Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers();

  enum Query implements QueryIdentifier {
    FEATURES,
    FEATURE
  }

  @Value.Immutable
  interface QueryInputFeatures extends QueryInput {
    String getCollectionId();

    FeatureQuery getQuery();

    FeatureProvider2 getFeatureProvider();

    EpsgCrs getDefaultCrs();

    Optional<Integer> getDefaultPageSize();

    boolean getShowsFeatureSelfLink();

    @Value.Default
    default boolean sendResponseAsStream() {
      return true;
    }

    // TODO rename and generalize when all file handling is moved to xtraplatform-spatial
    @Value.Default
    default Optional<File> getSaveContentAsFile() {
      return Optional.empty();
    }

    @Value.Default
    default String getPath() {
      return "/collections/" + getCollectionId() + "/items";
    }
  }

  @Value.Immutable
  interface QueryInputFeature extends QueryInput {
    String getCollectionId();

    String getFeatureId();

    FeatureQuery getQuery();

    FeatureProvider2 getFeatureProvider();

    EpsgCrs getDefaultCrs();

    @Value.Default
    default boolean sendResponseAsStream() {
      return false;
    }

    @Value.Default
    default Optional<File> getSaveContentAsFile() {
      return Optional.empty();
    }
  }
}

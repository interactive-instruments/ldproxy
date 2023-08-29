/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.domain;

import de.ii.ogcapi.foundation.domain.ApiSecurity.Scope;
import de.ii.ogcapi.foundation.domain.ApiSecurity.ScopeBase;
import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryIdentifier;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

public interface SearchQueriesHandler extends QueriesHandler<SearchQueriesHandler.Query> {

  String SCOPE_SEARCH = "search";
  Scope SCOPE_SEARCH_READ =
      Scope.of(ScopeBase.READ, SCOPE_SEARCH, "access stored queries and their parameters");
  Scope SCOPE_SEARCH_WRITE = Scope.of(ScopeBase.WRITE, SCOPE_SEARCH, "mutate stored queries");

  @Override
  Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers();

  enum Query implements QueryIdentifier {
    STORED_QUERIES,
    QUERY,
    DEFINITION,
    PARAMETERS,
    PARAMETER,
    CREATE_REPLACE,
    DELETE
  }

  @Value.Immutable
  interface QueryInputStoredQueries extends QueryInput {}

  @Value.Immutable
  interface QueryInputQuery extends QueryInput {

    QueryExpression getQuery();

    FeatureProvider2 getFeatureProvider();

    EpsgCrs getDefaultCrs();

    Optional<Integer> getMinimumPageSize();

    Optional<Integer> getDefaultPageSize();

    Optional<Integer> getMaximumPageSize();

    boolean getShowsFeatureSelfLink();

    @Value.Default
    default boolean getAllLinksAreLocal() {
      return false;
    }

    boolean getProfileIsApplicable();

    boolean isStoredQuery();
  }

  @Value.Immutable
  interface QueryInputQueryDefinition extends QueryInput {
    String getQueryId();

    QueryExpression getQuery();
  }

  @Value.Immutable
  interface QueryInputParameters extends QueryInput {
    String getQueryId();

    QueryExpression getQuery();
  }

  @Value.Immutable
  interface QueryInputParameter extends QueryInput {
    String getQueryId();

    QueryExpression getQuery();

    String getParameterName();
  }

  @Value.Immutable
  interface QueryInputStoredQueryCreateReplace extends QueryInput {
    String getQueryId();

    QueryExpression getQuery();

    boolean getStrict();

    boolean getDryRun();
  }

  @Value.Immutable
  interface QueryInputStoredQueryDelete extends QueryInput {
    String getQueryId();
  }
}

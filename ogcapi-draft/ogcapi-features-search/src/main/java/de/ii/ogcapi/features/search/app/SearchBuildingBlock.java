/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.search.domain.ImmutableSearchConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Search
 * @langEn The module *Search* TODO.
 * @langDe Das Modul *Search* TODO.
 * @example {@link de.ii.ogcapi.features.search.domain.SearchConfiguration}
 * @propertyTable {@link de.ii.ogcapi.features.search.domain.ImmutableSearchConfiguration}
 * @endpointTable {@link EndpointAdHocQuery}, {@link EndpointStoredQueries}, {@link
 *     EndpointStoredQuery}, {@link EndpointStoredQueriesManager}
 * @queryParameterTable {@link QueryParameterFAdHocQuery}, {@link QueryParameterFStoredQueries},
 *     {@link QueryParameterFStoredQuery}, {@link QueryParameterFQueryDefinition}, {@link
 *     QueryParameterDryRunStoredQueriesManager}
 */
@Singleton
@AutoBind
public class SearchBuildingBlock implements ApiBuildingBlock {

  @Inject
  public SearchBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableSearchConfiguration.Builder()
        .enabled(false)
        .managerEnabled(false)
        .validationEnabled(false)
        .build();
  }
}

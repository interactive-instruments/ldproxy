/**
 * Copyright 2020 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.api;

import de.ii.ldproxy.ogcapi.domain.OgcApiQueriesHandler;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryHandler;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryIdentifier;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryInput;
import de.ii.xtraplatform.feature.provider.api.FeatureProvider2;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

public interface OgcApiFeaturesCoreQueriesHandler extends OgcApiQueriesHandler<OgcApiFeaturesCoreQueriesHandler.Query> {

    @Override
    Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers();

    enum Query implements OgcApiQueryIdentifier {FEATURES, FEATURE}

    @Value.Immutable
    interface OgcApiQueryInputFeatures extends OgcApiQueryInput {
        String getCollectionId();

        FeatureQuery getQuery();

        FeatureProvider2 getFeatureProvider();

        Optional<Integer> getDefaultPageSize();

        boolean getShowsFeatureSelfLink();

        boolean getIncludeHomeLink();

        boolean getIncludeLinkHeader();
    }

    @Value.Immutable
    interface OgcApiQueryInputFeature extends OgcApiQueryInput {
        String getCollectionId();

        String getFeatureId();

        FeatureQuery getQuery();

        FeatureProvider2 getFeatureProvider();

        boolean getIncludeHomeLink();

        boolean getIncludeLinkHeader();
    }

}

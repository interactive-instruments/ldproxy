/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.api;

import de.ii.ldproxy.ogcapi.domain.OgcApiQueriesHandler;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryHandler;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryIdentifier;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryInput;
import de.ii.ldproxy.ogcapi.feature_processing.api.FeatureProcessChain;
import de.ii.ldproxy.ogcapi.feature_processing.api.Processing;
import de.ii.ldproxy.ogcapi.observation_processing.application.Variable;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import org.immutables.value.Value;

import java.util.*;

public interface ObservationProcessingQueriesHandler extends OgcApiQueriesHandler<ObservationProcessingQueriesHandler.Query> {

    @Override
    Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers();

    enum Query implements OgcApiQueryIdentifier {PROCESS, VARIABLES, LIST}

    @Value.Immutable
    interface OgcApiQueryInputObservationProcessing extends OgcApiQueryInput {

        // the query
        FeatureProvider2 getFeatureProvider();
        String getCollectionId();
        FeatureQuery getQuery();
        EpsgCrs getDefaultCrs();
        List<Variable> getVariables();

        // the output
        boolean getIncludeLinkHeader();

        // the processing
        FeatureProcessChain getProcesses();
        Map<String, Object> getProcessingParameters();

    }

    @Value.Immutable
    interface OgcApiQueryInputVariables extends OgcApiQueryInput {

        String getCollectionId();
        boolean getIncludeLinkHeader();
        boolean getIncludeHomeLink();
        List<Variable> getVariables();
    }

    @Value.Immutable
    interface OgcApiQueryInputProcessing extends OgcApiQueryInput {

        String getCollectionId();
        boolean getIncludeLinkHeader();
        boolean getIncludeHomeLink();
        Processing getProcessing();
    }
}

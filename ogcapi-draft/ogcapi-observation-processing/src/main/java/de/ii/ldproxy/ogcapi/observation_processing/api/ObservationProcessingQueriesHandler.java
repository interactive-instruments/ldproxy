/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.api;

import de.ii.ldproxy.ogcapi.domain.QueriesHandler;
import de.ii.ldproxy.ogcapi.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.domain.QueryIdentifier;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessChain;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.Processing;
import de.ii.ldproxy.ogcapi.observation_processing.application.Variable;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import org.immutables.value.Value;

import java.util.*;

public interface ObservationProcessingQueriesHandler extends QueriesHandler<ObservationProcessingQueriesHandler.Query> {

    @Override
    Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers();

    enum Query implements QueryIdentifier {PROCESS, VARIABLES, LIST}

    @Value.Immutable
    interface QueryInputObservationProcessing extends QueryInput {

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
    interface QueryInputVariables extends QueryInput {

        String getCollectionId();
        boolean getIncludeLinkHeader();
        List<Variable> getVariables();
    }

    @Value.Immutable
    interface QueryInputProcessing extends QueryInput {

        String getCollectionId();
        boolean getIncludeLinkHeader();
        Processing getProcessing();
    }
}

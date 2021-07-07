/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources.domain;

import de.ii.ldproxy.ogcapi.domain.QueriesHandler;
import de.ii.ldproxy.ogcapi.domain.QueryIdentifier;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import org.immutables.value.Value;

import java.util.Optional;

public interface QueriesHandlerResources extends QueriesHandler<QueriesHandlerResources.Query> {

    enum Query implements QueryIdentifier {RESOURCES, RESOURCE}

    @Value.Immutable
    interface QueryInputResources extends QueryInput {
        boolean getIncludeLinkHeader();
    }

    @Value.Immutable
    interface QueryInputResource extends QueryInput {
        String getResourceId();
    }
}

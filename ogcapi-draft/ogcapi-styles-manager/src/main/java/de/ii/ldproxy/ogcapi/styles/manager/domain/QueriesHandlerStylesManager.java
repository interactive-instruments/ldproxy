/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.manager.domain;

import de.ii.ldproxy.ogcapi.domain.QueriesHandler;
import de.ii.ldproxy.ogcapi.domain.QueryIdentifier;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import org.immutables.value.Value;

import javax.ws.rs.core.MediaType;
import java.util.Optional;

public interface QueriesHandlerStylesManager extends QueriesHandler<QueriesHandlerStylesManager.Query> {

    enum Query implements QueryIdentifier {CREATE_STYLE, REPLACE_STYLE, DELETE_STYLE, REPLACE_STYLE_METADATA, UPDATE_STYLE_METADATA}

    @Value.Immutable
    interface QueryInputStyleCreateReplace extends QueryInput {
        Optional<String> getCollectionId();
        Optional<String> getStyleId();
        MediaType getContentType();
        byte[] getRequestBody();
        boolean getStrict();
        boolean getDryRun();
    }

    @Value.Immutable
    interface QueryInputStyleDelete extends QueryInput {
        Optional<String> getCollectionId();
        String getStyleId();
    }

    @Value.Immutable
    interface QueryInputStyleMetadata extends QueryInput {
        Optional<String> getCollectionId();
        String getStyleId();
        MediaType getContentType();
        byte[] getRequestBody();
        boolean getStrict();
        boolean getDryRun();
    }
}

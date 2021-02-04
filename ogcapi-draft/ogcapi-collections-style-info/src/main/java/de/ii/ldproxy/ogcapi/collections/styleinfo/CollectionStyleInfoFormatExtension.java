/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.styleinfo;

import de.ii.ldproxy.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApi;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Path;

import static de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

public interface CollectionStyleInfoFormatExtension extends GenericFormatExtension {

    @Override
    default String getPathPattern() {
        return "^/collections(?:/"+COLLECTION_ID_PATTERN+")?/?$";
    }

    Response patchStyleInfos(byte[] requestBody, Path styleInfosStore, OgcApi api, String collectionId) throws IOException;

    default boolean canSupportTransactions() { return true; }
}

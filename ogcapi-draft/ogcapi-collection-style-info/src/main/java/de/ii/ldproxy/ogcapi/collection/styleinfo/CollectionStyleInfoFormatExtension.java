/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collection.styleinfo;

import de.ii.ldproxy.ogcapi.domain.*;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;

public interface CollectionStyleInfoFormatExtension extends FormatExtension {

    @Override
    default String getPathPattern() {
        return "^/collections(?:/[\\w\\-]+)?/?$";
    }

    Response patchStyleInfos(byte[] requestBody, File styleInfosStore, OgcApiApi api, String collectionId) throws IOException;

    default boolean canSupportTransactions() { return true; }

    @Override
    default boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, StyleInfoConfiguration.class);
    }

    @Override
    default boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), StyleInfoConfiguration.class);
    }
}

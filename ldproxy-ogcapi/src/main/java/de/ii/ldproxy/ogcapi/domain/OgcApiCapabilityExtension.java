/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

public interface OgcApiCapabilityExtension extends OgcApiExtension {

    // TODO: document the relevance of these extensions

    @Override
    default boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return true;
    }

    ExtensionConfiguration getDefaultConfiguration(OgcApiConfigPreset preset);
}

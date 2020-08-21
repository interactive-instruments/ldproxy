/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.api;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.xtraplatform.features.domain.FeatureProvider2;

public interface FeaturesCoreProviders {

    FeatureProvider2 getFeatureProvider(OgcApiDataV2 apiData);

    FeatureProvider2 getFeatureProvider(OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureType);
}

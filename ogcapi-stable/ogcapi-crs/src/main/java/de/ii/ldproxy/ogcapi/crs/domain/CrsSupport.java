/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.crs.domain;

import de.ii.ldproxy.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.crs.domain.EpsgCrs;

import java.util.List;
import java.util.Optional;

public interface CrsSupport {

    boolean isEnabled(OgcApiDataV2 apiData);

    List<EpsgCrs> getSupportedCrsList(OgcApiDataV2 apiData);

    List<EpsgCrs> getSupportedCrsList(OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureTypeConfiguration);

    boolean isSupported(OgcApiDataV2 apiData, EpsgCrs crs);

    boolean isSupported(OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureTypeConfiguration, EpsgCrs crs);

    EpsgCrs getStorageCrs(OgcApiDataV2 apiData,
                          Optional<FeatureTypeConfigurationOgcApi> featureTypeConfiguration);
}

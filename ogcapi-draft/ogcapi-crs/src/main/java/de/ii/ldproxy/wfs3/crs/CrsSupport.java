/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.crs;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.xtraplatform.crs.domain.EpsgCrs;

import java.util.List;
import java.util.Optional;

public interface CrsSupport {

    List<EpsgCrs> getSupportedCrsList(OgcApiApiDataV2 apiData);

    List<EpsgCrs> getSupportedCrsList(OgcApiApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureTypeConfiguration);

    boolean isSupported(OgcApiApiDataV2 apiData, EpsgCrs crs);

    boolean isSupported(OgcApiApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureTypeConfiguration, EpsgCrs crs);

    EpsgCrs getStorageCrs(OgcApiApiDataV2 apiData,
                          Optional<FeatureTypeConfigurationOgcApi> featureTypeConfiguration);
}

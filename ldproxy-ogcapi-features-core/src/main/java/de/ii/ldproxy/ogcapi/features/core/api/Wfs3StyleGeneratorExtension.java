/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.api;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtension;
import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;

public interface Wfs3StyleGeneratorExtension extends OgcApiExtension {

    // TODO this is only for GSFS and does not belong here?

    String generateStyle(OgcApiApiDataV2 data, FeatureTypeConfigurationOgcApi featureType, int index);

    String generateStyle(SimpleFeatureGeometry simpleFeatureGeometry, int index);
}

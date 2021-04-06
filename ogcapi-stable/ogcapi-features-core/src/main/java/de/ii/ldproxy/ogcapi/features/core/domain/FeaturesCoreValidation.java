/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface FeaturesCoreValidation {
    List<String> getCollectionsWithoutType(OgcApiDataV2 apiData, Map<String, FeatureSchema> featureSchemas);

    List<String> getInvalidPropertyKeys(Collection<String> keys, FeatureSchema schema);

    Map<String, Collection<String>> getInvalidPropertyKeys(Map<String, Collection<String>> keyMap, Map<String, FeatureSchema> featureSchemas);
}

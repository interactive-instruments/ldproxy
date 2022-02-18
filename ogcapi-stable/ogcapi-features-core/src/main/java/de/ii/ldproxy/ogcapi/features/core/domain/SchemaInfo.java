/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import java.util.List;
import java.util.Map;

public interface SchemaInfo {

    List<String> getPropertyNames(FeatureSchema featureType, boolean withArrayBrackets,
        boolean withObjects);

    Map<String, String> getNameTitleMap(FeatureSchema featureType);

    List<String> getPropertyNames(OgcApiDataV2 apiData, String collectionId);

    List<String> getPropertyNames(OgcApiDataV2 apiData, String collectionId, boolean withSpatial, boolean withArrayBrackets);

    Map<String, SchemaBase.Type> getPropertyTypes(FeatureSchema featureType, boolean withArrayBrackets);
}

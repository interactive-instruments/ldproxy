/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain.processing;

import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import java.util.List;

public interface FeatureProcessInfo {

    List<FeatureProcessChain> getProcessingChains(OgcApiDataV2 apiData, String collectionId,
                                                  Class<? extends FeatureProcess> processType);
    List<FeatureProcessChain> getProcessingChains(OgcApiDataV2 apiData,
                                                  Class<? extends FeatureProcess> processType);
    boolean matches(OgcApiDataV2 apiData, Class<? extends FeatureProcess> processType,
                    String definitionPath, String... processNames);
}

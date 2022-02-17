/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.foundation.domain;


import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.util.List;

@AutoMultiBind
public interface ConformanceClass extends ApiExtension {
    List<String> getConformanceClassUris(OgcApiDataV2 apiData);
}

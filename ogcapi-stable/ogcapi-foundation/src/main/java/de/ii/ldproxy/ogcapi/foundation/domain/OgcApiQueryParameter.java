/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.util.Set;

@AutoMultiBind
public interface OgcApiQueryParameter extends ParameterExtension {

    default String getStyle() { return "form"; }

    boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method);
    default boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId, HttpMethods method) { return isApplicable(apiData, definitionPath, method); }

    default Set<String> getFilterParameters(Set<String> filterParameters, OgcApiDataV2 apiData, String collectionId) { return filterParameters; };
}

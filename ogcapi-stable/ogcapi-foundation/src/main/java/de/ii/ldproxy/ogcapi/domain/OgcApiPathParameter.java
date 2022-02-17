/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.util.List;
import java.util.Optional;

@AutoMultiBind
public interface OgcApiPathParameter extends ParameterExtension {
    default boolean getExplodeInOpenApi(OgcApiDataV2 apiData) { return false; }
    List<String> getValues(OgcApiDataV2 apiData);
    String getPattern();

    boolean isApplicable(OgcApiDataV2 apiData, String definitionPath);
    default boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId) { return isApplicable(apiData, definitionPath); }
    default boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, Optional<String> collectionId) {
        return collectionId.isPresent() ? isApplicable(apiData,definitionPath,collectionId.get()) : isApplicable(apiData,definitionPath);
    }

    @Override
    default boolean getRequired(OgcApiDataV2 apiData) { return true; }
}

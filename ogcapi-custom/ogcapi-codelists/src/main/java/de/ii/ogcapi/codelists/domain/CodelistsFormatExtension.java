/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.codelists.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;

@AutoMultiBind
public interface CodelistsFormatExtension extends GenericFormatExtension {

  Object getCodelistsEntity(
      Codelists codelists, OgcApiDataV2 apiData, ApiRequestContext requestContext);

  @Override
  default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return CodelistsConfiguration.class;
  }

  @Override
  default boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return GenericFormatExtension.super.isEnabledForApi(apiData);
  }
}

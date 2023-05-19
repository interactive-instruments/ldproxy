/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.schema.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument.VERSION;
import de.ii.ogcapi.features.core.domain.JsonSchemaObject;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;

@AutoMultiBind
public interface SchemaFormatExtension extends FormatExtension {

  @Override
  default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return SchemaConfiguration.class;
  }

  @Override
  default boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return FormatExtension.super.isEnabledForApi(apiData)
        && apiData
            .getExtension(SchemaConfiguration.class)
            .map(cfg -> cfg.getVersions().contains(getVersion()))
            .orElse(false);
  }

  @Override
  default boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return FormatExtension.super.isEnabledForApi(apiData, collectionId)
        && apiData
            .getExtension(SchemaConfiguration.class, collectionId)
            .map(cfg -> cfg.getVersions().contains(getVersion()))
            .orElse(false);
  }

  VERSION getVersion();

  default Object getEntity(
      JsonSchemaObject schema, String collectionId, OgcApi api, ApiRequestContext requestContext) {
    return schema;
  }
}

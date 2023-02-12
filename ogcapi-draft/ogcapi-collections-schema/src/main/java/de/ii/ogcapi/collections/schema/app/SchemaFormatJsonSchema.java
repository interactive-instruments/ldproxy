/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.schema.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.collections.schema.domain.SchemaFormatExtension;
import de.ii.ogcapi.features.core.domain.JsonSchemaObject;
import de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class SchemaFormatJsonSchema implements SchemaFormatExtension {

  @Inject
  SchemaFormatJsonSchema() {}

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return GeoJsonConfiguration.class;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
            .getExtension(getBuildingBlockConfigurationType())
            .map(ExtensionConfiguration::isEnabled)
            .orElse(false)
        && apiData
            .getExtension(GeoJsonConfiguration.class)
            .map(ExtensionConfiguration::isEnabled)
            .orElse(true);
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return apiData
            .getCollections()
            .get(collectionId)
            .getExtension(getBuildingBlockConfigurationType())
            .map(ExtensionConfiguration::isEnabled)
            .orElse(false)
        && apiData
            .getCollections()
            .get(collectionId)
            .getExtension(GeoJsonConfiguration.class)
            .map(ExtensionConfiguration::isEnabled)
            .orElse(true);
  }

  @Override
  public ApiMediaType getMediaType() {
    return ApiMediaType.JSON_SCHEMA_MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent() {
    return SCHEMA_CONTENT;
  }

  @Override
  public Object getEntity(
      JsonSchemaObject schema, String collectionId, OgcApi api, ApiRequestContext requestContext) {
    return schema;
  }
}

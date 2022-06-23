/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.maps.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class PathParameterTileRow implements OgcApiPathParameter {

  private final SchemaValidator schemaValidator;

  @Inject
  PathParameterTileRow(SchemaValidator schemaValidator) {
    this.schemaValidator = schemaValidator;
  }

  @Override
  public String getPattern() {
    return "\\d+";
  }

  @Override
  public List<String> getValues(OgcApiDataV2 apiData) {
    return ImmutableList.of();
  }

  private final Schema<?> schema = new IntegerSchema().minimum(BigDecimal.ZERO);

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    return schema;
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }

  @Override
  public String getId() {
    return "tileRowMapTile";
  }

  @Override
  public String getName() {
    return "tileRow";
  }

  @Override
  public String getDescription() {
    return "Row index of the tile on the selected zoom level. See http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ for more information about Level, Row and Column in the Google Maps tiling scheme (WebMercatorQuad). "
        + "Example: In the WebMercatorQuad tiling scheme Ireland is fully within the tile with the following values: Level 5, Row 10 and Col 15";
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
    return isEnabledForApi(apiData)
        && (definitionPath.equals("/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
            || definitionPath.equals(
                "/collections/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"));
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return MapTilesConfiguration.class;
  }
}

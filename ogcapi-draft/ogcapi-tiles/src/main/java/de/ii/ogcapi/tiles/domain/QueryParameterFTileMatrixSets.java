/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.common.domain.QueryParameterF;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetsFormatExtension;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @langEn Todo
 * @langDe Todo
 * @name TileMatrixSets
 * @endpoints Tileset
 */
@Singleton
@AutoBind
public class QueryParameterFTileMatrixSets extends QueryParameterF {

  @Inject
  public QueryParameterFTileMatrixSets(
      ExtensionRegistry extensionRegistry, SchemaValidator schemaValidator) {
    super(extensionRegistry, schemaValidator);
  }

  @Override
  public String getId() {
    return "fTileMatrixSets";
  }

  @Override
  protected boolean matchesPath(String definitionPath) {
    return definitionPath.equals("/tileMatrixSets")
        || definitionPath.equals("/tileMatrixSets/{tileMatrixSetId}");
  }

  @Override
  protected Class<? extends FormatExtension> getFormatClass() {
    return TileMatrixSetsFormatExtension.class;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TilesConfiguration.class;
  }
}
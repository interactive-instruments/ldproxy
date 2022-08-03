/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ogcapi.features.gltf.domain._3dTilesConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class PathParameterCollectionId3dTiles extends AbstractPathParameterCollectionId {

  @Inject
  PathParameterCollectionId3dTiles(SchemaValidator schemaValidator) {
    super(schemaValidator);
  }

  @Override
  public String getId() {
    return "collectionId3dTiles";
  }

  @Override
  public boolean matchesPath(String definitionPath) {
    return definitionPath.startsWith("/collections/{collectionId}/3dtiles");
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return _3dTilesConfiguration.class;
  }
}

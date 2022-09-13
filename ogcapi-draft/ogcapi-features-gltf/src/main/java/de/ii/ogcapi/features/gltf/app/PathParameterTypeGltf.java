/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.common.domain.PathParameterType;
import de.ii.ogcapi.features.gltf.domain.GltfConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class PathParameterTypeGltf extends PathParameterType {

  static final List<String> TYPES = ImmutableList.of("3d-metadata");
  static final String SCHEMA_TYPE_PATTERN = "[\\w\\-]+";

  @Inject
  public PathParameterTypeGltf(
      ExtensionRegistry extensionRegistry, SchemaValidator schemaValidator) {
    super(extensionRegistry, schemaValidator);
  }

  @Override
  public String getId() {
    return "typeSchemaGltf";
  }

  @Override
  protected boolean isApplicablePath(OgcApiDataV2 apiData, String definitionPath) {
    return definitionPath.equals("/collections/{collectionId}/schemas/{type}");
  }

  @Override
  public List<String> getValues(OgcApiDataV2 apiData) {
    return TYPES;
  }

  @Override
  public String getPattern() {
    return SCHEMA_TYPE_PATTERN;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return GltfConfiguration.class;
  }
}

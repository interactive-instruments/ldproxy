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
import de.ii.ogcapi.features.gltf.domain._3dTilesConfiguration;
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
public class PathParameterY implements OgcApiPathParameter {

  private final SchemaValidator schemaValidator;

  @Inject
  PathParameterY(SchemaValidator schemaValidator) {
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
  public String getName() {
    return "y";
  }

  @Override
  public String getDescription() {
    return "TODO";
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
    return isEnabledForApi(apiData)
        && (definitionPath.equals("/collections/{collectionId}/3dtiles/content/{level}/{x}/{y}"));
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return _3dTilesConfiguration.class;
  }
}

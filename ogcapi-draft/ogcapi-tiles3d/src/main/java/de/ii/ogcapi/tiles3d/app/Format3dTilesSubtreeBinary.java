/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles3d.domain.Format3dTilesSubtree;
import de.ii.ogcapi.tiles3d.domain.Tiles3dConfiguration;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@AutoBind
@Singleton
public class Format3dTilesSubtreeBinary implements Format3dTilesSubtree {

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "octet-stream"))
          .label("subtree-Binary")
          .parameter("subtree")
          .build();

  @Inject
  public Format3dTilesSubtreeBinary() {}

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return Tiles3dConfiguration.class;
  }

  @Override
  public boolean canSupportTransactions() {
    return false;
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {

    // no additional operational checks for now, only validation; we can stop, if no validation is
    // requested
    if (apiValidation == MODE.NONE) {
      return ValidationResult.of();
    }

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().mode(apiValidation);

    // TODO

    return builder.build();
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    String schemaRef = "#/components/schemas/anyObject";
    Schema<?> schema = new ObjectSchema();
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(schema)
        .schemaRef(schemaRef)
        .ogcApiMediaType(getMediaType())
        .build();
  }

  @Override
  public Object getEntity(
      Object subtree,
      List<Link> links,
      String collectionId,
      OgcApi api,
      ApiRequestContext requestContext) {
    return subtree;
  }
}

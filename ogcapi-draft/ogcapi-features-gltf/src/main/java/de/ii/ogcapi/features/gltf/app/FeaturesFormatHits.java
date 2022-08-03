/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesCoreValidation;
import de.ii.ogcapi.features.core.domain.SchemaGeneratorCollectionOpenApi;
import de.ii.ogcapi.features.core.domain.SchemaGeneratorOpenApi;
import de.ii.ogcapi.features.gltf.domain._3dTilesConfiguration;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import io.swagger.v3.oas.models.media.IntegerSchema;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@AutoBind
@Singleton
public class FeaturesFormatHits implements FeatureFormatExtension {

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("text", "plain"))
          .label("Text")
          .parameter("txt")
          .build();

  protected final FeaturesCoreProviders providers;
  protected final EntityRegistry entityRegistry;
  protected final FeaturesCoreValidation featuresCoreValidator;
  protected final SchemaGeneratorOpenApi schemaGeneratorFeature;
  protected final SchemaGeneratorCollectionOpenApi schemaGeneratorFeatureCollection;
  protected final CrsTransformer toEcef;

  @Inject
  public FeaturesFormatHits(
      FeaturesCoreProviders providers,
      EntityRegistry entityRegistry,
      FeaturesCoreValidation featuresCoreValidator,
      SchemaGeneratorOpenApi schemaGeneratorFeature,
      SchemaGeneratorCollectionOpenApi schemaGeneratorFeatureCollection,
      CrsTransformerFactory crsTransformerFactory) {
    this.providers = providers;
    this.entityRegistry = entityRegistry;
    this.featuresCoreValidator = featuresCoreValidator;
    this.schemaGeneratorFeature = schemaGeneratorFeature;
    this.schemaGeneratorFeatureCollection = schemaGeneratorFeatureCollection;
    this.toEcef =
        crsTransformerFactory
            .getTransformer(OgcCrs.CRS84h, EpsgCrs.of(4978))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Could not create a CRS transformer from CRS84h to EPSG:4978."));
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return _3dTilesConfiguration.class;
  }

  @Override
  public boolean canSupportTransactions() {
    return false;
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public String getPathPattern() {
    return "^/_for_internal_use_only_$";
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(new IntegerSchema())
        .schemaRef("_for_internal_use_only_")
        .ogcApiMediaType(getMediaType())
        .build();
  }

  @Override
  public ApiMediaType getCollectionMediaType() {
    return null;
  }

  @Override
  public boolean canEncodeFeatures() {
    return true;
  }

  @Override
  public Optional<FeatureTokenEncoder<?>> getFeatureEncoder(
      FeatureTransformationContext transformationContext, Optional<Locale> language) {

    return Optional.of(new FeatureEncoderHits(transformationContext));
  }
}

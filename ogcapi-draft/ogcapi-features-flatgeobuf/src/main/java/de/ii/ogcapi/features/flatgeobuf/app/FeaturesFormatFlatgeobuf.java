/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.flatgeobuf.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeatureSchemaCache;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.flatgeobuf.domain.FlatgeobufConfiguration;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class FeaturesFormatFlatgeobuf implements ConformanceClass, FeatureFormatExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesFormatFlatgeobuf.class);

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "flatgeobuf"))
          .label("FlatGeobuf")
          .parameter("fgb")
          .build();
  public static final ApiMediaType COLLECTION_MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "json"))
          .label("JSON")
          .parameter("json")
          .build();

  private final FeaturesCoreProviders providers;
  private final CrsInfo crsInfo;
  private final FeatureSchemaCache schemaCache;

  @Inject
  public FeaturesFormatFlatgeobuf(FeaturesCoreProviders providers, CrsInfo crsInfo) {
    this.providers = providers;
    this.crsInfo = crsInfo;
    this.schemaCache = new SchemaCacheFlatgeobuf();
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of();
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return FlatgeobufConfiguration.class;
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public ApiMediaType getCollectionMediaType() {
    return COLLECTION_MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    // TODO Should we describe the schema used in the binary file? As an OpenAPI schema?
    String schemaRef = "#/components/schemas/FlatGeobuf";
    Schema<?> schema = new BinarySchema();
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(schema)
        .schemaRef(schemaRef)
        .ogcApiMediaType(MEDIA_TYPE)
        .build();
  }

  @Override
  public boolean canEncodeFeatures() {
    return true;
  }

  @Override
  public Optional<FeatureTokenEncoder<?>> getFeatureEncoder(
      FeatureTransformationContext transformationContext, Optional<Locale> language) {

    OgcApiDataV2 apiData = transformationContext.getApiData();
    String collectionId = transformationContext.getCollectionId();
    FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
    EpsgCrs crs =
        transformationContext.getCrsTransformer().isPresent()
            ? transformationContext.getCrsTransformer().get().getTargetCrs()
            : providers
                .getFeatureProvider(apiData)
                .map(FeatureProvider2::getData)
                .flatMap(FeatureProviderDataV2::getNativeCrs)
                .orElse(EpsgCrs.of(4326, EpsgCrs.Force.LON_LAT));

    FeatureSchema schema =
        schemaCache.getSchema(
            providers
                .getFeatureSchema(apiData, collectionData)
                .orElse(
                    new ImmutableFeatureSchema.Builder()
                        .name(collectionId)
                        .type(SchemaBase.Type.OBJECT)
                        .build()),
            apiData,
            apiData.getCollectionData(collectionId).orElse(null));

    return Optional.of(
        new FeatureEncoderFlatgeobuf(
            ImmutableFeatureTransformationContextFlatgeobuf.builder()
                .from(transformationContext)
                .schema(schema)
                .is3d(crsInfo.is3d(crs))
                .build()));
  }
}

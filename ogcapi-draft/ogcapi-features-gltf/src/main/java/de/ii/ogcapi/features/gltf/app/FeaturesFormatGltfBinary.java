/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.collections.domain.CollectionsConfiguration;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesCoreValidation;
import de.ii.ogcapi.features.gltf.domain.FeatureTransformationContextGltf;
import de.ii.ogcapi.features.gltf.domain.GltfConfiguration;
import de.ii.ogcapi.features.gltf.domain.ImmutableFeatureTransformationContextGltf;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@AutoBind
@Singleton
public class FeaturesFormatGltfBinary implements FeatureFormatExtension {

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("model", "gltf-binary"))
          .label("glTF-Binary")
          .parameter("glb")
          .build();

  public static final ApiMediaType COLLECTION_MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "json"))
          .label("JSON")
          .parameter("json")
          .build();

  protected final FeaturesCoreProviders providers;
  protected final EntityRegistry entityRegistry;
  protected final FeaturesCoreValidation featuresCoreValidator;
  protected final CrsTransformer toEcef;

  @Inject
  public FeaturesFormatGltfBinary(
      FeaturesCoreProviders providers,
      EntityRegistry entityRegistry,
      FeaturesCoreValidation featuresCoreValidator,
      CrsTransformerFactory crsTransformerFactory) {
    this.providers = providers;
    this.entityRegistry = entityRegistry;
    this.featuresCoreValidator = featuresCoreValidator;
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
    return GltfConfiguration.class;
  }

  @Override
  public boolean canSupportTransactions() {
    return false;
  }

  @Override
  public EpsgCrs getContentCrs(EpsgCrs targetCrs) {
    return EpsgCrs.of(4978);
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

    // TODO verify the schema meets the CityJSON requirements, otherwise throw an error

    OgcApiDataV2 apiData = api.getData();
    Map<String, FeatureSchema> featureSchemas = providers.getFeatureSchemas(apiData);

    // get Gltf configurations to process
    Map<String, GltfConfiguration> configurationMap =
        apiData.getCollections().entrySet().stream()
            .map(
                entry -> {
                  final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
                  final GltfConfiguration config =
                      collectionData.getExtension(GltfConfiguration.class).orElse(null);
                  if (Objects.isNull(config)) {
                    return null;
                  }
                  return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), config);
                })
            .filter(Objects::nonNull)
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    Map<String, Collection<String>> keyMap =
        configurationMap.entrySet().stream()
            .map(
                entry ->
                    new AbstractMap.SimpleImmutableEntry<>(
                        entry.getKey(), entry.getValue().getTransformations().keySet()))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    for (Map.Entry<String, Collection<String>> stringCollectionEntry :
        featuresCoreValidator.getInvalidPropertyKeys(keyMap, featureSchemas).entrySet()) {
      for (String property : stringCollectionEntry.getValue()) {
        builder.addStrictErrors(
            MessageFormat.format(
                "A transformation for property ''{0}'' in collection ''{1}'' is invalid, because the property was not found in the provider schema.",
                property, stringCollectionEntry.getKey()));
      }
    }

    Set<String> codelists =
        entityRegistry.getEntitiesForType(Codelist.class).stream()
            .map(Codelist::getId)
            .collect(Collectors.toUnmodifiableSet());
    for (Map.Entry<String, GltfConfiguration> entry : configurationMap.entrySet()) {
      String collectionId = entry.getKey();
      for (Map.Entry<String, List<PropertyTransformation>> entry2 :
          entry.getValue().getTransformations().entrySet()) {
        String property = entry2.getKey();
        for (PropertyTransformation transformation : entry2.getValue()) {
          builder = transformation.validate(builder, collectionId, property, codelists);
        }
      }
    }

    return builder.build();
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    String schemaRef = "#/components/schemas/binary";
    Schema<?> schema = new StringSchema().format("binary");
    String collectionId = path.split("/", 4)[2];
    if (collectionId.equals("{collectionId}")
        && apiData
            .getExtension(CollectionsConfiguration.class)
            .filter(config -> config.getCollectionDefinitionsAreIdentical().orElse(false))
            .isPresent()) {
      collectionId = apiData.getCollections().keySet().iterator().next();
    }
    // TODO generate OpenAPI schemas?
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(schema)
        .schemaRef(schemaRef)
        .ogcApiMediaType(getMediaType())
        .build();
  }

  @Override
  public ApiMediaType getCollectionMediaType() {
    return COLLECTION_MEDIA_TYPE;
  }

  @Override
  public boolean canEncodeFeatures() {
    return true;
  }

  @Override
  public Optional<FeatureTokenEncoder<?>> getFeatureEncoder(
      FeatureTransformationContext transformationContext, Optional<Locale> language) {

    // CRS must be CRS84h
    EpsgCrs crs = transformationContext.getTargetCrs();
    if (!OgcCrs.CRS84h.equals(crs)) {
      throw new IllegalArgumentException(
          String.format(
              "glTF-Binary requires a 3D coordinate reference system. Found: '%s'",
              crs.toUriString()));
    }

    OgcApiDataV2 apiData = transformationContext.getApiData();
    String collectionId = transformationContext.getCollectionId();
    FeatureTypeConfigurationOgcApi collectionData =
        apiData.getCollectionData(collectionId).orElseThrow();

    FeatureTransformationContextGltf transformationContextGltf =
        ImmutableFeatureTransformationContextGltf.builder()
            .from(transformationContext)
            .clampToGround(
                "true"
                    .equals(
                        transformationContext
                            .getOgcApiRequest()
                            .getParameters()
                            .get("clampToGround")))
            .crsTransformerCrs84hToEcef(toEcef)
            .build();

    return Optional.of(new FeatureEncoderGltf(transformationContextGltf, entityRegistry));
  }
}

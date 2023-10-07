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
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesCoreValidation;
import de.ii.ogcapi.features.gltf.domain.FeatureTransformationContextGltf;
import de.ii.ogcapi.features.gltf.domain.GltfAsset;
import de.ii.ogcapi.features.gltf.domain.GltfConfiguration;
import de.ii.ogcapi.features.gltf.domain.GltfSchema;
import de.ii.ogcapi.features.gltf.domain.ImmutableFeatureTransformationContextGltf;
import de.ii.ogcapi.features.gltf.domain.Metadata3dSchemaCache;
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
import de.ii.xtraplatform.entities.domain.EntityRegistry;
import de.ii.xtraplatform.entities.domain.ImmutableValidationResult;
import de.ii.xtraplatform.entities.domain.ValidationResult;
import de.ii.xtraplatform.entities.domain.ValidationResult.MODE;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.services.domain.ServicesContext;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.Schema;
import java.io.IOException;
import java.net.URI;
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

  public static final Schema<?> SCHEMA = new BinarySchema();
  public static final String SCHEMA_REF = "#/components/schemas/glTF";

  private final FeaturesCoreProviders providers;
  private final EntityRegistry entityRegistry;
  private final FeaturesCoreValidation featuresCoreValidator;
  private final CrsTransformerFactory crsTransformerFactory;
  private CrsTransformer toEcef;
  private final URI serviceUri;
  private final Metadata3dSchemaCache schemaCache;

  @Inject
  public FeaturesFormatGltfBinary(
      FeaturesCoreProviders providers,
      EntityRegistry entityRegistry,
      FeaturesCoreValidation featuresCoreValidator,
      CrsTransformerFactory crsTransformerFactory,
      ServicesContext servicesContext,
      Metadata3dSchemaCache schemaCache) {
    this.providers = providers;
    this.entityRegistry = entityRegistry;
    this.featuresCoreValidator = featuresCoreValidator;
    this.crsTransformerFactory = crsTransformerFactory;
    this.toEcef = null;
    this.serviceUri = servicesContext.getUri();
    this.schemaCache = schemaCache;
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
  public ApiMediaTypeContent getContent() {
    // TODO Should we describe the schema used in the binary file? As an OpenAPI schema?
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(SCHEMA)
        .schemaRef(SCHEMA_REF)
        .ogcApiMediaType(getMediaType())
        .build();
  }

  @Override
  public EpsgCrs getContentCrs(EpsgCrs targetCrs) {
    return EpsgCrs.of(4978);
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().mode(apiValidation);

    if (toEcef == null) {
      toEcef =
          crsTransformerFactory
              .getTransformer(OgcCrs.CRS84h, EpsgCrs.of(4978))
              .orElseGet(
                  () -> {
                    builder.addErrors(
                        "Could not create a CRS transformer from CRS84h to EPSG:4978.");
                    return null;
                  });
    }

    // no additional operational checks for now, only validation; we can stop, if no validation is
    // requested
    if (apiValidation == MODE.NONE) {
      return builder.build();
    }

    final OgcApiDataV2 apiData = api.getData();
    final Map<String, FeatureSchema> featureSchemas = providers.getFeatureSchemas(apiData);

    validateSchema(api, builder);

    // get Gltf configurations to process
    Map<String, GltfConfiguration> configurationMap = getGltfConfigurationMap(apiData);

    Map<String, Collection<String>> propertyNamesWithTransformations =
        getPropertyNamesWithTransformations(configurationMap);

    for (Map.Entry<String, Collection<String>> stringCollectionEntry :
        featuresCoreValidator
            .getInvalidPropertyKeys(propertyNamesWithTransformations, featureSchemas)
            .entrySet()) {
      for (String property : stringCollectionEntry.getValue()) {
        builder.addStrictErrors(
            MessageFormat.format(
                "A transformation for property ''{0}'' in collection ''{1}'' is invalid, because the property was not found in the provider schema.",
                property, stringCollectionEntry.getKey()));
      }
    }

    validateTransformations(builder, configurationMap);

    return builder.build();
  }

  private void validateTransformations(
      ImmutableValidationResult.Builder builder, Map<String, GltfConfiguration> configurationMap) {
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
          transformation.validate(builder, collectionId, property, codelists);
        }
      }
    }
  }

  private Map<String, Collection<String>> getPropertyNamesWithTransformations(
      Map<String, GltfConfiguration> configurationMap) {
    Map<String, Collection<String>> keyMap =
        configurationMap.entrySet().stream()
            .map(
                entry ->
                    new AbstractMap.SimpleImmutableEntry<>(
                        entry.getKey(), entry.getValue().getTransformations().keySet()))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    return keyMap;
  }

  private Map<String, GltfConfiguration> getGltfConfigurationMap(OgcApiDataV2 apiData) {
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
    return configurationMap;
  }

  private void validateSchema(OgcApi api, ImmutableValidationResult.Builder builder) {
    if (api.getData().getCollections().size() != 1) {
      builder.addErrors(
          MessageFormat.format(
              "Support for glTF currently requires an API with a single feature collection. Found '{0}' collections.",
              api.getData().getCollections().size()));
      return;
    }

    FeatureTypeConfigurationOgcApi collectionData =
        api.getData().getCollections().values().iterator().next();
    FeatureSchema featureType =
        providers.getFeatureSchema(api.getData(), collectionData).orElse(null);
    if (Objects.isNull(featureType)) {
      builder.addErrors(
          MessageFormat.format(
              "No schema for feature type '{0}' was found.", collectionData.getId()));
    } else {
      boolean atLeastOneLoD = false;
      if (featureType.getPropertyMap().containsKey("lod1Solid")) {
        atLeastOneLoD =
            validateSolid(builder, featureType.getPropertyMap().get("lod1Solid"), "lod1Solid");
      }
      if (featureType.getPropertyMap().containsKey("lod2Solid")
          && validateSolid(builder, featureType.getPropertyMap().get("lod2Solid"), "lod2Solid")) {
        atLeastOneLoD =
            validateSurfaces(builder, featureType.getPropertyMap().get("surfaces"), "surfaces");
      }
      if (!atLeastOneLoD) {
        builder.addErrors(
            "The feature type of the API must be a building features of LoD1 or LoD2.");
      }
    }
  }

  private boolean validateSolid(
      ImmutableValidationResult.Builder builder, FeatureSchema solid, String propertyName) {
    boolean valid =
        Objects.nonNull(solid)
            && solid.isSpatial()
            && solid
                .getGeometryType()
                .filter(gt -> gt.equals(SimpleFeatureGeometry.MULTI_POLYGON))
                .isPresent();
    if (!valid) {
      builder.addErrors(
          MessageFormat.format(
              "Feature property '{0}' must be a MULTI_POLYGON with the constraints 'composite' and 'closed'. Found: {1}",
              propertyName, Objects.nonNull(solid) ? solid.toString() : "null"));
      return false;
    }
    valid =
        solid.getConstraints().stream().anyMatch(SchemaConstraints::isComposite)
            && solid.getConstraints().stream().anyMatch(SchemaConstraints::isClosed);
    if (!valid) {
      builder.addStrictErrors(
          MessageFormat.format(
              "Feature property '{0}' must be a MULTI_POLYGON with the constraints 'composite' and 'closed'. Found: {1}",
              propertyName, solid.toString()));
    }
    return true;
  }

  private boolean validateSurfaces(
      ImmutableValidationResult.Builder builder, FeatureSchema surfaces, String propertyName) {
    //noinspection ConstantConditions
    boolean valid =
        Objects.nonNull(surfaces)
            && surfaces.getType().equals(Type.OBJECT_ARRAY)
            && surfaces.getPropertyMap().containsKey("surfaceType")
            && surfaces.getPropertyMap().get("surfaceType").getType().equals(Type.STRING)
            && surfaces.getPropertyMap().containsKey("lod2MultiSurface")
            && surfaces
                .getPropertyMap()
                .get("lod2MultiSurface")
                .getGeometryType()
                .filter(gt -> gt.equals(SimpleFeatureGeometry.MULTI_POLYGON))
                .isPresent();
    if (!valid) {
      builder.addErrors(
          MessageFormat.format(
              "Feature property '{0}' must be an array of objects with the string property 'surfaceType' and the MULTI_POLYGON property 'lod2MultiSurface'. Found: {1}",
              propertyName, Objects.nonNull(surfaces) ? surfaces.toString() : "null"));
      return false;
    }
    return true;
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
              "glTF-Binary requires that coordinates are in WGS 84 longitude/latitude/ellipsoidal height (OGC:CRS84h). Found: '%s'",
              crs.toUriString()));
    }

    OgcApiDataV2 apiData = transformationContext.getApiData();
    String collectionId = transformationContext.getCollectionId();

    URI schemaUri =
        URI.create(
            String.format(
                "%s/%s/collections/%s/gltf/schema?f=json",
                serviceUri.toString(), apiData.getId(), collectionId));

    GltfSchema gltfSchema =
        schemaCache.getSchema(
            transformationContext.getFeatureSchema().orElseThrow(),
            apiData,
            collectionId,
            entityRegistry.getEntitiesForType(Codelist.class));

    FeatureTransformationContextGltf transformationContextGltf =
        ImmutableFeatureTransformationContextGltf.builder()
            .from(transformationContext)
            .clampToEllipsoid(
                "true"
                    .equalsIgnoreCase(
                        transformationContext
                            .getOgcApiRequest()
                            .getParameters()
                            .get("clampToEllipsoid")))
            .crsTransformerCrs84hToEcef(toEcef)
            .schemaUri(schemaUri)
            .gltfSchema(gltfSchema)
            .gltfConfiguration(
                apiData.getExtension(GltfConfiguration.class, collectionId).orElseThrow())
            .build();

    return Optional.of(new FeatureEncoderGltf(transformationContextGltf));
  }

  @Override
  public boolean supportsHitsOnly() {
    return true;
  }

  @Override
  public Optional<Long> getNumberMatched(Object content) {
    return getMetadata(content, "numberMatched");
  }

  @Override
  public Optional<Long> getNumberReturned(Object content) {
    return getMetadata(content, "numberReturned");
  }

  private Optional<Long> getMetadata(Object content, String key) {
    if (content instanceof byte[]) {
      try {
        GltfAsset asset = GltfAsset.of((byte[]) content);
        if (asset.getAsset().getExtras().containsKey(key)) {
          Object value = asset.getAsset().getExtras().get(key);
          if (value instanceof Long) {
            return Optional.of((Long) value);
          } else if (value instanceof Integer) {
            return Optional.of(((Integer) value).longValue());
          } else if (value instanceof String) {
            return Optional.of(Long.parseLong((String) value));
          }
        }
      } catch (IOException e) {
        // ignore
      }
    }
    return Optional.empty();
  }
}

/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import static de.ii.ogcapi.features.gltf.domain.GltfProperty.GLTF_TYPE.UINT8;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.gltf.domain.GltfProperty;
import de.ii.ogcapi.features.gltf.domain.GltfProperty.GLTF_TYPE;
import de.ii.ogcapi.features.gltf.domain.GltfSchema;
import de.ii.ogcapi.features.gltf.domain.ImmutableGltfSchema;
import de.ii.ogcapi.features.gltf.domain.ImmutableGltfSchema.Builder;
import de.ii.ogcapi.features.gltf.domain.ImmutableSchemaClass;
import de.ii.ogcapi.features.gltf.domain.ImmutableSchemaEnum;
import de.ii.ogcapi.features.gltf.domain.ImmutableSchemaEnumValue;
import de.ii.ogcapi.features.gltf.domain.ImmutableSchemaProperty;
import de.ii.ogcapi.features.gltf.domain.Metadata3dSchemaCache;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.codelists.domain.CodelistData;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class Metadata3dSchemaCacheImpl implements Metadata3dSchemaCache {

  private final ConcurrentMap<Integer, ConcurrentMap<String, GltfSchema>> cache;

  @Inject
  protected Metadata3dSchemaCacheImpl() {
    this.cache = new ConcurrentHashMap<>();
  }

  @Override
  public final GltfSchema getSchema(
      FeatureSchema featureSchema,
      OgcApiDataV2 apiData,
      String collectionId,
      Map<String, GltfProperty> properties,
      List<Codelist> codelists,
      boolean withSurfaceTypes) {
    int apiHashCode = apiData.hashCode();
    if (!cache.containsKey(apiHashCode)) {
      cache.put(apiHashCode, new ConcurrentHashMap<>());
    }
    if (!cache.get(apiHashCode).containsKey(collectionId)) {
      cache
          .get(apiHashCode)
          .put(
              collectionId,
              deriveSchema(
                  featureSchema, apiData, collectionId, properties, codelists, withSurfaceTypes));
    }

    return cache.get(apiHashCode).get(collectionId);
  }

  protected GltfSchema deriveSchema(
      FeatureSchema schema,
      OgcApiDataV2 apiData,
      String collectionId,
      Map<String, GltfProperty> properties,
      List<Codelist> codelists,
      boolean withSurfaceTypes) {

    boolean hasSchema = false;
    Builder schemaBuilder = ImmutableGltfSchema.builder().id(apiData.getId() + "_" + collectionId);
    ImmutableSchemaClass.Builder classBuilder = ImmutableSchemaClass.builder();
    if (!properties.isEmpty()) {
      hasSchema = true;

      Map<String, Map<String, Integer>> propertyEnums = GltfHelper.getEnums(schema, properties);

      for (FeatureSchema property : schema.getProperties()) {
        if (!properties.containsKey(property.getName())) {
          continue;
        }
        GLTF_TYPE type = properties.get(property.getName()).getType();
        String noData = properties.get(property.getName()).getNoData().orElse("0");
        ImmutableSchemaProperty.Builder propertyBuilder = ImmutableSchemaProperty.builder();

        if (propertyEnums.containsKey(property.getName())) {
          // 1) enum based on enum values
          ImmutableSchemaEnum.Builder schemaEnumBuilder =
              getSchemaEnumBuilder(property, type, noData);
          propertyEnums
              .get(property.getName())
              .forEach(
                  (key, value) ->
                      schemaEnumBuilder.addValues(
                          ImmutableSchemaEnumValue.builder().value(value).name(key).build()));
          schemaBuilder.putEnums(property.getName(), schemaEnumBuilder.build());
          propertyBuilder.type("ENUM");
          propertyBuilder.enumType(property.getName());
        } else if (properties.get(property.getName()).shouldUseCode()
            && property.getConstraints().filter(c -> c.getCodelist().isPresent()).isPresent()) {
          // 2) enum based on a codelist
          ImmutableSchemaEnum.Builder schemaEnumBuilder =
              getSchemaEnumBuilder(property, type, noData);
          property
              .getConstraints()
              .flatMap(SchemaConstraints::getCodelist)
              .flatMap(
                  codelist ->
                      codelists.stream()
                          .filter(codelist1 -> codelist1.getId().equals(codelist))
                          .findFirst()
                          .map(Codelist::getData)
                          .map(CodelistData::getEntries))
              .ifPresent(
                  codelistValues ->
                      codelistValues.forEach(
                          (code, value) ->
                              schemaEnumBuilder.addValues(
                                  ImmutableSchemaEnumValue.builder()
                                      .value(Integer.parseInt(code))
                                      .name(value)
                                      .build())));
          schemaBuilder.putEnums(property.getName(), schemaEnumBuilder.build());
          propertyBuilder.type("ENUM");
          propertyBuilder.enumType(property.getName());
        } else {
          // 3) just based on the specified type for the property
          switch (type) {
            case STRING:
              propertyBuilder.type(type.name());
              propertyBuilder.noData(noData);
              break;
            case BOOLEAN:
              propertyBuilder.type(type.name());
              break;
            default:
              propertyBuilder.type("SCALAR");
              propertyBuilder.componentType(type.name());
              propertyBuilder.noData(noData);
              break;
          }
        }

        propertyBuilder.description(property.getLabel());
        property
            .getConstraints()
            .flatMap(SchemaConstraints::getRequired)
            .ifPresent(propertyBuilder::required);
        classBuilder.putProperties(property.getName(), propertyBuilder.build());
      }
    }

    if (withSurfaceTypes) {
      hasSchema = true;
      Map<String, Byte> surfaceTypeEnums = GltfHelper.getSurfaceTypeEnums();

      schemaBuilder.putEnums(
          FeatureEncoderGltf.SURFACE_TYPE,
          ImmutableSchemaEnum.builder()
              .name("Semantic Surface Types")
              .valueType(UINT8.name())
              .values(
                  surfaceTypeEnums.entrySet().stream()
                      .map(
                          entry ->
                              ImmutableSchemaEnumValue.builder()
                                  .value(entry.getValue())
                                  .name(entry.getKey())
                                  .build())
                      .collect(Collectors.toUnmodifiableList()))
              .build());

      classBuilder.putProperties(
          "surfaceType",
          ImmutableSchemaProperty.builder()
              .type("ENUM")
              .enumType(FeatureEncoderGltf.SURFACE_TYPE)
              .build());
    }

    if (!hasSchema) {
      return null;
    }

    schemaBuilder.putClasses("building", classBuilder.build());
    return schemaBuilder.build();
  }

  private ImmutableSchemaEnum.Builder getSchemaEnumBuilder(
      FeatureSchema property, GLTF_TYPE type, String noData) {
    return ImmutableSchemaEnum.builder()
        .name(property.getLabel())
        .description(property.getDescription())
        .valueType(type.name())
        .addValues(
            ImmutableSchemaEnumValue.builder()
                .value(Integer.parseInt(noData))
                .name("No data")
                .build());
  }
}

/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class SchemaInfoImpl implements SchemaInfo {

  private final FeaturesCoreProviders providers;

  @Inject
  public SchemaInfoImpl(FeaturesCoreProviders providers) {
    this.providers = providers;
  }

  /**
   * @param featureType
   * @param withArrayBrackets
   * @param withObjects
   * @return the list of all property names of the feature type
   */
  @Override
  public List<String> getPropertyNames(
      FeatureSchema featureType, boolean withArrayBrackets, boolean withObjects) {
    if (Objects.isNull(featureType)) {
      return ImmutableList.of();
    }

    return featureType.getProperties().stream()
        .map(
            featureProperty ->
                getPropertyNames(featureProperty, "", withArrayBrackets, withObjects))
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  private List<String> getPropertyNames(
      FeatureSchema property, String basePath, boolean withArrayBrackets, boolean withObjects) {
    List<String> propertyNames = new ArrayList<>();
    if (property.isObject()) {
      if (withObjects) {
        propertyNames.add(getPropertyName(property, basePath, withArrayBrackets));
      }
      property
          .getProperties()
          .forEach(
              subProperty ->
                  propertyNames.addAll(
                      getPropertyNames(
                          subProperty,
                          getPropertyName(property, basePath, withArrayBrackets),
                          withArrayBrackets,
                          false)));
    } else {
      propertyNames.add(getPropertyName(property, basePath, withArrayBrackets));
    }
    return propertyNames;
  }

  private String getPropertyName(
      FeatureSchema property, String basePath, boolean withArrayBrackets) {
    return (basePath.isEmpty() ? "" : basePath + ".")
        + property.getName().trim()
        + (property.isArray() && withArrayBrackets ? "[]" : "");
  }

  /**
   * @param featureType
   * @param withArrayBrackets
   * @return the list of all property names of the feature type
   */
  @Override
  public Map<String, SchemaBase.Type> getPropertyTypes(
      FeatureSchema featureType, boolean withArrayBrackets) {
    if (Objects.isNull(featureType)) {
      return ImmutableMap.of();
    }

    ImmutableMap.Builder<String, SchemaBase.Type> nameTypeMapBuilder = new ImmutableMap.Builder<>();
    featureType
        .getProperties()
        .forEach(
            featureProperty ->
                addPropertyTypes(nameTypeMapBuilder, featureProperty, "", withArrayBrackets));
    return nameTypeMapBuilder.build();
  }

  private void addPropertyTypes(
      ImmutableMap.Builder<String, SchemaBase.Type> nameTypeMapBuilder,
      FeatureSchema property,
      String basePath,
      boolean withArrayBrackets) {
    if (property.isObject()) {
      property
          .getProperties()
          .forEach(
              subProperty ->
                  addPropertyTypes(
                      nameTypeMapBuilder,
                      subProperty,
                      getPropertyName(property, basePath, withArrayBrackets),
                      withArrayBrackets));
    } else {
      nameTypeMapBuilder.put(getPropertyType(property, basePath, withArrayBrackets));
    }
  }

  private static Map.Entry<String, SchemaBase.Type> getPropertyType(
      FeatureSchema property, String basePath, boolean withArrayBrackets) {
    String propertyName =
        (basePath.isEmpty() ? "" : basePath + ".")
            + property.getName().trim()
            + (property.isArray() && withArrayBrackets ? "[]" : "");
    return new AbstractMap.SimpleImmutableEntry<>(propertyName, property.getType());
  }

  /**
   * @param featureType
   * @return a map with an entry for each property and the label/title of the property as the value
   */
  @Override
  public Map<String, String> getNameTitleMap(FeatureSchema featureType) {
    if (Objects.isNull(featureType)) {
      return ImmutableMap.of();
    }

    ImmutableMap.Builder<String, String> nameTitleMapBuilder = new ImmutableMap.Builder<>();
    featureType
        .getProperties()
        .forEach(
            featureProperty -> addPropertyTitles(nameTitleMapBuilder, featureProperty, "", ""));
    return nameTitleMapBuilder.build();
  }

  private static void addPropertyTitles(
      ImmutableMap.Builder<String, String> nameTitleMapBuilder,
      FeatureSchema property,
      String basePath,
      String baseTitle) {
    if (property.isObject()) {
      nameTitleMapBuilder.put(getPropertyTitle(property, basePath, baseTitle));
      property
          .getProperties()
          .forEach(
              subProperty -> {
                Map.Entry<String, String> entry = getPropertyTitle(property, basePath, baseTitle);
                addPropertyTitles(
                    nameTitleMapBuilder, subProperty, entry.getKey(), entry.getValue());
              });
    } else {
      nameTitleMapBuilder.put(getPropertyTitle(property, basePath, baseTitle));
    }
  }

  private static Map.Entry<String, String> getPropertyTitle(
      FeatureSchema property, String basePath, String baseTitle) {
    String name = (basePath.isEmpty() ? "" : basePath + ".") + property.getName().trim();
    String title =
        (baseTitle.isEmpty() ? "" : baseTitle + " > ")
            + property.getLabel().orElse(property.getName()).trim();
    return new AbstractMap.SimpleImmutableEntry<>(name, title);
  }

  @Override
  public List<String> getPropertyNames(OgcApiDataV2 apiData, String collectionId) {
    return getPropertyNames(apiData, collectionId, false, false);
  }

  @Override
  public List<String> getPropertyNames(
      OgcApiDataV2 apiData, String collectionId, boolean withSpatial, boolean withArrayBrackets) {
    Optional<FeatureSchema> schema =
        providers.getFeatureSchema(apiData, apiData.getCollections().get(collectionId));
    if (schema.isEmpty()) {
      return ImmutableList.of();
    }

    return schema.get().getProperties().stream()
        .filter(featureProperty -> !featureProperty.isSpatial() || withSpatial)
        .map(featureProperty -> getPropertyNames(featureProperty, "", withArrayBrackets, false))
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }
}

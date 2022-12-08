/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.CollectionPropertiesType;
import de.ii.ogcapi.features.core.domain.CollectionProperty;
import de.ii.ogcapi.features.core.domain.ImmutableCollectionProperty;
import de.ii.ogcapi.features.core.domain.JsonSchema;
import de.ii.ogcapi.features.core.domain.JsonSchemaAbstractDocument;
import de.ii.ogcapi.features.core.domain.JsonSchemaArray;
import de.ii.ogcapi.features.core.domain.JsonSchemaBoolean;
import de.ii.ogcapi.features.core.domain.JsonSchemaInteger;
import de.ii.ogcapi.features.core.domain.JsonSchemaNumber;
import de.ii.ogcapi.features.core.domain.JsonSchemaRef;
import de.ii.ogcapi.features.core.domain.JsonSchemaRefExternal;
import de.ii.ogcapi.features.core.domain.JsonSchemaString;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.ogcapi.html.domain.OgcApiView;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.DataClass")
public class CollectionPropertiesView extends OgcApiView {
  public List<CollectionProperty> collectionProperties;
  public String typeTitle;
  public String enumTitle;
  public boolean hasEnum;
  public String none;
  // sum must be 12 for bootstrap
  public Integer idCols;
  public Integer descCols;

  @SuppressWarnings("PMD.DataClass")
  public CollectionPropertiesView(
      OgcApiDataV2 apiData,
      JsonSchemaAbstractDocument schemaCollectionProperties,
      CollectionPropertiesType type,
      List<Link> links,
      List<NavigationDTO> breadCrumbs,
      String staticUrlPrefix,
      HtmlConfiguration htmlConfig,
      boolean noIndex,
      I18n i18n,
      Optional<Locale> language) {
    super(
        "collectionProperties.mustache",
        Charsets.UTF_8,
        apiData,
        breadCrumbs,
        htmlConfig,
        noIndex,
        staticUrlPrefix,
        links,
        i18n.get(type + "Title", language),
        i18n.get(type + "Description", language));

    Map<String, JsonSchema> properties = schemaCollectionProperties.getProperties();
    ImmutableList.Builder<CollectionProperty> builder = ImmutableList.builder();
    processProperties(properties, builder);
    this.collectionProperties = builder.build();
    int maxIdLength =
        this.collectionProperties.stream()
            .map(CollectionProperty::getId)
            .filter(Objects::nonNull)
            .mapToInt(String::length)
            .max()
            .orElse(0);
    idCols = Math.min(Math.max(2, 1 + maxIdLength / 10), 6);
    descCols = 12 - idCols;
    this.typeTitle = i18n.get("typeTitle", language);
    this.enumTitle = i18n.get("enumTitle", language);
    this.none = i18n.get("none", language);
  }

  private void processProperties(
      Map<String, JsonSchema> properties, ImmutableList.Builder<CollectionProperty> builder) {
    properties.forEach(
        (key, value) -> {
          processProperty(builder, key, value);
        });
  }

  private void processProperty(
      ImmutableList.Builder<CollectionProperty> builder, String name, JsonSchema type) {
    ImmutableCollectionProperty.Builder builder2 = ImmutableCollectionProperty.builder().id(name);
    builder2.title(type.getTitle()).description(type.getDescription());

    JsonSchema effectiveType = type;
    if (type instanceof JsonSchemaArray) {
      builder2.isArray(true);
      effectiveType = ((JsonSchemaArray) type).getItems();
    } else {
      builder2.isArray(false);
    }

    if (effectiveType instanceof JsonSchemaString) {
      processString((JsonSchemaString) effectiveType, builder2);
    } else if (effectiveType instanceof JsonSchemaNumber) {
      builder2.type("number");
    } else if (effectiveType instanceof JsonSchemaInteger) {
      builder2.type("integer");
      builder2.values(
          ((JsonSchemaInteger) effectiveType)
              .getEnums().stream().map(String::valueOf).collect(Collectors.toList()));
    } else if (effectiveType instanceof JsonSchemaBoolean) {
      builder2.type("boolean");
    } else if (effectiveType instanceof JsonSchemaRef) {
      builder2.type(
          ((JsonSchemaRef) effectiveType)
              .getRef()
              .replace("https://geojson.org/schema/", "")
              .replace(".json", ""));
    } else if (effectiveType instanceof JsonSchemaRefExternal) {
      builder2.type(
          ((JsonSchemaRefExternal) effectiveType)
              .getRef()
              .replace("https://geojson.org/schema/", "")
              .replace(".json", ""));
    } else {
      builder2.type("string");
    }
    builder.add(builder2.build());
  }

  private void processString(JsonSchemaString value, ImmutableCollectionProperty.Builder builder2) {
    Optional<String> format = value.getFormat();
    if (format.isPresent()) {
      if ("date-time".equals(format.get())) {
        builder2.type("date-time");
      } else if ("date".equals(format.get())) {
        builder2.type("date");
      } else {
        builder2.type("string");
      }
    } else {
      builder2.type("string");
    }
    builder2.values(value.getEnums());
  }
}

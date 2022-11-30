/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.CollectionPropertiesType;
import de.ii.ogcapi.features.core.domain.CollectionProperty;
import de.ii.ogcapi.features.core.domain.ImmutableCollectionProperty;
import de.ii.ogcapi.features.core.domain.JsonSchema;
import de.ii.ogcapi.features.core.domain.JsonSchemaArray;
import de.ii.ogcapi.features.core.domain.JsonSchemaBoolean;
import de.ii.ogcapi.features.core.domain.JsonSchemaInteger;
import de.ii.ogcapi.features.core.domain.JsonSchemaNumber;
import de.ii.ogcapi.features.core.domain.JsonSchemaObject;
import de.ii.ogcapi.features.core.domain.JsonSchemaRef;
import de.ii.ogcapi.features.core.domain.JsonSchemaRefExternal;
import de.ii.ogcapi.features.core.domain.JsonSchemaString;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.OgcApiView;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Value.Immutable
@Value.Style(builder = "new", visibility = ImplementationVisibility.PUBLIC)
public abstract class CollectionPropertiesView extends OgcApiView {

  @Value.Derived
  public List<CollectionProperty> collectionProperties() {
    Map<String, JsonSchema> properties = schemaCollectionProperties().getProperties();
    ImmutableList.Builder<CollectionProperty> builder = ImmutableList.builder();
    properties.forEach(
        (key, value) -> {
          ImmutableCollectionProperty.Builder builder2 =
              ImmutableCollectionProperty.builder().id(key);
          builder2.title(value.getTitle()).description(value.getDescription());

          if (value instanceof JsonSchemaArray) {
            builder2.isArray(true);
            value = ((JsonSchemaArray) value).getItems();
          } else {
            builder2.isArray(false);
          }

          if (value instanceof JsonSchemaString) {
            Optional<String> format = ((JsonSchemaString) value).getFormat();
            if (format.isPresent()) {
              if (format.get().equals("date-time")) {
                builder2.type("date-time");
              } else if (format.get().equals("date")) {
                builder2.type("date");
              } else {
                builder2.type("string");
              }
            } else {
              builder2.type("string");
            }
            builder2.values(((JsonSchemaString) value).getEnums());
          } else if (value instanceof JsonSchemaNumber) {
            builder2.type("number");
          } else if (value instanceof JsonSchemaInteger) {
            builder2.type("integer");
            builder2.values(
                ((JsonSchemaInteger) value)
                    .getEnums().stream().map(String::valueOf).collect(Collectors.toList()));
          } else if (value instanceof JsonSchemaBoolean) {
            builder2.type("boolean");
          } else if (value instanceof JsonSchemaRef) {
            builder2.type(
                ((JsonSchemaRef) value)
                    .getRef()
                    .replace("https://geojson.org/schema/", "")
                    .replace(".json", ""));
          } else if (value instanceof JsonSchemaRefExternal) {
            builder2.type(
                ((JsonSchemaRefExternal) value)
                    .getRef()
                    .replace("https://geojson.org/schema/", "")
                    .replace(".json", ""));
          } else {
            builder2.type("string");
          }
          builder.add(builder2.build());
        });
    return builder.build();
  }

  @Value.Derived
  public Optional<String> typeTitle() {
    return Optional.of(i18n().get("typeTitle", language()));
  }

  @Value.Derived
  public Optional<String> enumTitle() {
    return Optional.of(i18n().get("enumTitle", language()));
  }

  public abstract Optional<Boolean> hasEnum();

  @Value.Derived
  public String none() {
    return i18n().get("none", language());
  }
  // sum must be 12 for bootstrap
  @Value.Derived
  public Integer idCols() {
    int maxIdLength =
        this.collectionProperties().stream()
            .map(CollectionProperty::getId)
            .filter(Objects::nonNull)
            .mapToInt(String::length)
            .max()
            .orElse(0);
    return Math.min(Math.max(2, 1 + maxIdLength / 10), 6);
  }
  // Todo idCols needs to be calculated first
  @Value.Derived
  public Integer descCols() {
    if (idCols() != null) {
      return 12 - idCols();
    }
    return 12;
  }

  public abstract JsonSchemaObject schemaCollectionProperties();

  public abstract CollectionPropertiesType type();

  public abstract URICustomizer uriCustomizer();

  public abstract I18n i18n();

  public abstract Optional<Locale> language();

  // Todo Delete after the View class problem is solved
  public CollectionPropertiesView() {
    super("collectionProperties.mustache");
  }
}

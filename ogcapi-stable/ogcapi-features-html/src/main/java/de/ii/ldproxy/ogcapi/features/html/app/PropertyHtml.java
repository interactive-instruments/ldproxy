/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.app;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.features.domain.SchemaBase;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Modifiable
@Value.Style(set = "*")
public interface PropertyHtml extends PropertyBase<PropertyHtml, FeatureSchema> {

  Optional<String> getItemType();

  PropertyHtml itemType(String itemType);

  Optional<String> getItemProp();

  PropertyHtml itemProp(String itemProp);

  @Value.Lazy
  default String getSchemaOrgItemType() {
    return getItemType()
        .filter(itemType -> itemType.startsWith("http://schema.org/"))
        .map(itemType -> itemType.substring(18))
        .orElse("");
  }

  @Value.Default
  default String getName() {
    return getSchema().flatMap(FeatureSchema::getLabel).orElse(getSchema().map(FeatureSchema::getName).orElse(""));
  }

  @Value.Lazy
  default boolean hasValues() {
    return isValue() || (isArray() && getNestedProperties().stream().anyMatch(PropertyBase::isValue));
  }

  @Value.Lazy
  default List<PropertyHtml> getValues() {
    return isValue()
        ? ImmutableList.of(this)
        : isArray()
            ? getNestedProperties().stream().filter(PropertyBase::isValue).collect(Collectors.toList())
            : ImmutableList.of();
  }

  @Value.Lazy
  default String getFirstValue() {
    return hasValues() ? getValues().get(0).getValue() : null;
  }

  @Value.Lazy
  default List<PropertyHtml> getObjects() {
    return getNestedProperties().stream().filter(property -> !property.isValue() && property.getSchema().filter(
        SchemaBase::isGeometry).isEmpty()).collect(Collectors.toList());
  }

  @Value.Lazy
  default boolean isInArray() {
    return getParent().isPresent() && getParent().get().isArray();
  }

  @Value.Lazy
  default boolean isFirstObject() {
    return getParent().isPresent() && getParent().get().isArray() && Objects
        .equals(getParent().get().getNestedProperties().get(0), this);
  }

  @Value.Lazy
  @Override
  default boolean isObject() {
    return PropertyBase.super.isObject() && getSchema().filter(
        SchemaBase::isGeometry).isEmpty();
  }

  @Value.Lazy
  default boolean isNull() {
    return Objects.isNull(getValue());
  }

  @Value.Lazy
  default boolean isHtml() {
    return Objects.nonNull(getValue()) && getValue().startsWith("<") && (getValue().endsWith(">")
        || getValue().endsWith(">\n")) && getValue().contains("</");
  }

  @Value.Lazy
  default boolean isUrl() {
    return Objects.nonNull(getValue()) && (getValue().startsWith("http://") || getValue()
        .startsWith("https://"));
  }

  @Value.Lazy
  default boolean isImageUrl() {
    return Objects.nonNull(getValue()) && isUrl() && (getValue().toLowerCase()
        .endsWith(".png") || getValue().toLowerCase()
        .endsWith(".jpg") || getValue().toLowerCase()
        .endsWith(".jpeg") || getValue().toLowerCase()
        .endsWith(".gif"));
  }

  @Value.Lazy
  default boolean isLevel1() {
    return getPropertyPath().size()==1;
  }

  @Value.Lazy
  default boolean isLevel2() {
    return getPropertyPath().size()==2;
  }

  @Value.Lazy
  default boolean isLevel3() {
    return getPropertyPath().size()==3;
  }

  @Value.Lazy
  default boolean isLevel4() {
    return getPropertyPath().size()==4;
  }

  @Value.Lazy
  default boolean isLevel5() {
    return getPropertyPath().size()==5;
  }

  @Value.Lazy
  default boolean hasGeometry() {
    return getNestedProperties().stream().anyMatch(property -> property.getSchema().filter(
        SchemaBase::isGeometry).isPresent())
        || getNestedProperties().stream().anyMatch(PropertyHtml::hasGeometry);
  }

  @Value.Lazy
  default Optional<PropertyHtml> getGeometry() {
    return getNestedProperties().stream()
        .filter(property -> property.getSchema().filter(SchemaBase::isGeometry).isPresent())
        .findFirst()
        .or(() -> getNestedProperties().stream()
            .map(PropertyHtml::getGeometry)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst());
  }

  Splitter PATH_SPLITTER = Splitter.on('.').omitEmptyStrings();

  default Optional<PropertyHtml> findPropertyByPath(String pathString) {
    return findPropertyByPath(PATH_SPLITTER.splitToList(pathString));
  }

  default Optional<PropertyHtml> findPropertyByPath(List<String> path) {
    return getNestedProperties().stream()
        .filter(property -> Objects.equals(property.getPropertyPath(), path))
        .findFirst()
        .or(() -> getNestedProperties().stream()
            .filter(property -> property.getSchema().filter(SchemaBase::isGeometry).isEmpty())
            .map(property -> property.findPropertyByPath(path))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst());
  }
}

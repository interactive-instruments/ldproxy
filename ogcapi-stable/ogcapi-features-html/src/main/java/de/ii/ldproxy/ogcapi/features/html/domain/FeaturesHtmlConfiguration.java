/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.MapClient;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new", attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableFeaturesHtmlConfiguration.Builder.class)
public interface FeaturesHtmlConfiguration extends ExtensionConfiguration, PropertyTransformations {

  abstract class Builder extends ExtensionConfiguration.Builder {

  }

  enum LAYOUT {CLASSIC, COMPLEX_OBJECTS}
  enum POSITION {AUTO, TOP, RIGHT}

  @Deprecated(since = "3.1.0")
  @Nullable
  LAYOUT getLayout();

  @Nullable
  POSITION getMapPosition();

  @JsonAlias("itemLabelFormat")
  Optional<String> getFeatureTitleTemplate();

  @JsonSerialize(converter = IgnoreLinksWildcardSerializer.class)
  @Override
  Map<String, List<PropertyTransformation>> getTransformations();

  @Nullable
  MapClient.Type getMapClientType();

  @Nullable
  String getStyle();

  @Nullable
  Boolean getRemoveZoomLevelConstraints();

  @Nullable
  List<String> getGeometryProperties();

  @Nullable
  Integer getMaximumPageSize();

  @Value.Check
  default FeaturesHtmlConfiguration backwardsCompatibility() {
    if (getLayout() == LAYOUT.CLASSIC
      && (!hasTransformation(PropertyTransformations.WILDCARD, transformations ->
        transformations.getFlatten().isPresent()))) {
      Map<String, List<PropertyTransformation>> transformations = withTransformation(PropertyTransformations.WILDCARD,
          new ImmutablePropertyTransformation.Builder()
          .flatten(".")
          .build());

        return new ImmutableFeaturesHtmlConfiguration.Builder()
            .from(this)
            .mapPosition(POSITION.RIGHT)
            .transformations(transformations)
            .build();
    }

    if (getLayout() == LAYOUT.COMPLEX_OBJECTS && getMapPosition() != POSITION.TOP) {
      return new ImmutableFeaturesHtmlConfiguration.Builder()
          .from(this)
          .mapPosition(POSITION.TOP)
          .build();
    }

    return this;
  }

  String LINK_WILDCARD = "*{objectType=Link}";

  @Value.Check
  default FeaturesHtmlConfiguration transformLinks() {
    if (!hasTransformation(LINK_WILDCARD, transformation -> transformation.getReduceStringFormat().isPresent())) {

      Map<String, List<PropertyTransformation>> transformations = withTransformation(LINK_WILDCARD,
          new ImmutablePropertyTransformation.Builder()
          .reduceStringFormat("<a href=\"{{href}}\">{{title}}</a>")
          .build());

      return new ImmutableFeaturesHtmlConfiguration.Builder()
          .from(this)
          .transformations(transformations)
          .build();
    }

    return this;
  }

  class IgnoreLinksWildcardSerializer extends StdConverter<Map<String, List<PropertyTransformation>>, Map<String, List<PropertyTransformation>>> {

    @Override
    public Map<String, List<PropertyTransformation>> convert(
        Map<String, List<PropertyTransformation>> value) {
      if (value.containsKey(LINK_WILDCARD) && value.get(LINK_WILDCARD).stream().anyMatch(transformation -> transformation.getReduceStringFormat().isPresent())) {

        return value.entrySet().stream()
            .filter(entry -> !Objects.equals(entry.getKey(), LINK_WILDCARD)
                || entry.getValue().size() != 1
                || entry.getValue().get(0).getReduceStringFormat().isEmpty())
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
      }

      return value;
    }
  }

  @Override
  default Builder getBuilder() {
    return new ImmutableFeaturesHtmlConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    return new ImmutableFeaturesHtmlConfiguration.Builder()
        .from(source)
        .from(this)
        .transformations(PropertyTransformations.super.mergeInto((PropertyTransformations) source).getTransformations())
        .build();
  }
}

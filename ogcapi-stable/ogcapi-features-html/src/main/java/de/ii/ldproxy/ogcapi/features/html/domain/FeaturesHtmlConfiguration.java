/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.MapClient;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;

import java.util.List;
import java.util.Map;
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

  @Override
  Map<String, List<PropertyTransformation>> getTransformations();

  @Nullable
  MapClient.Type getMapClientType();

  @Nullable
  String getStyle();

  @Nullable
  Boolean getRemoveZoomLevelConstraints();

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

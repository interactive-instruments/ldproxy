/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.ldproxy.ogcapi.features.html.app.HtmlPropertyTransformations;
import de.ii.ldproxy.ogcapi.features.html.app.ImmutableHtmlPropertyTransformations;
import de.ii.xtraplatform.codelists.domain.Codelist;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
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

  @Nullable
  LAYOUT getLayout();

  // TODO duplicate from HtmlConfiguration
  @Nullable
  Boolean getSchemaOrgEnabled();

  Optional<String> getItemLabelFormat();

  @Override
  Map<String, PropertyTransformation> getTransformations();

  default Map<String, HtmlPropertyTransformations> getTransformations(
      Optional<PropertyTransformations> baseTransformations,
      Map<String, Codelist> codelists,
      String serviceUrl, boolean isOverview) {
    Map<String, ImmutableHtmlPropertyTransformations.Builder> mapBuilder = new LinkedHashMap<>();

    baseTransformations.ifPresent(base -> base.getSchemaTransformations(isOverview
    )
        .forEach((propertyName, schemaTransformers) -> {
          mapBuilder.putIfAbsent(propertyName, new ImmutableHtmlPropertyTransformations.Builder());
          mapBuilder.get(propertyName)
              .addAllSchemaTransformers(schemaTransformers);
        }));

    this.getSchemaTransformations(isOverview)
        .forEach((propertyName, schemaTransformers) -> {
          mapBuilder.putIfAbsent(propertyName, new ImmutableHtmlPropertyTransformations.Builder());
          mapBuilder.get(propertyName)
              .addAllSchemaTransformers(schemaTransformers);
        });

    baseTransformations.ifPresent(base -> base.getValueTransformations(codelists, ImmutableMap.of("serviceUrl", serviceUrl))
        .forEach((propertyName, valueTransformers) -> {
          mapBuilder.putIfAbsent(propertyName, new ImmutableHtmlPropertyTransformations.Builder());
          mapBuilder.get(propertyName)
              .addAllValueTransformers(valueTransformers);
        }));

    this.getValueTransformations(codelists, ImmutableMap.of("serviceUrl", serviceUrl))
        .forEach((propertyName, valueTransformers) -> {
          mapBuilder.putIfAbsent(propertyName, new ImmutableHtmlPropertyTransformations.Builder());
          mapBuilder.get(propertyName)
              .addAllValueTransformers(valueTransformers);
        });

    return mapBuilder.entrySet()
        .stream()
        .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()
            .build()))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

  }

  @Override
  default Builder getBuilder() {
    return new ImmutableFeaturesHtmlConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableFeaturesHtmlConfiguration.Builder builder = new ImmutableFeaturesHtmlConfiguration.Builder()
        .from(source)
        .from(this);

    Map<String, PropertyTransformation> mergedTransformations = new LinkedHashMap<>(
        ((PropertyTransformations) source).getTransformations());
    getTransformations().forEach((key, transformation) -> {
      if (mergedTransformations.containsKey(key)) {
        mergedTransformations.put(key, transformation.mergeInto(mergedTransformations.get(key)));
      } else {
        mergedTransformations.put(key, transformation);
      }
    });
    builder.transformations(mergedTransformations);

    return builder.build();
  }
}

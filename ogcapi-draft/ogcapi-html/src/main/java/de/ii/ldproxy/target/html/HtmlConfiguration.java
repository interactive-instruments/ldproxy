/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformations;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTypeMapping2;
import de.ii.xtraplatform.codelists.Codelist;
import de.ii.xtraplatform.entity.api.maptobuilder.BuildableBuilder;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new", attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableHtmlConfiguration.Builder.class)
public interface HtmlConfiguration extends ExtensionConfiguration, FeatureTransformations {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    enum LAYOUT {CLASSIC, COMPLEX_OBJECTS}

    @Nullable
    Boolean getNoIndexEnabled();

    @JsonAlias(value = "microdataEnabled")
    @Nullable
    Boolean getSchemaOrgEnabled();

    @Nullable
    Boolean getCollectionDescriptionsInOverview();

    @Nullable
    LAYOUT getLayout();

    Optional<String> getItemLabelFormat();

    @Override
    Map<String, FeatureTypeMapping2> getTransformations();

    default Map<String, HtmlPropertyTransformations> getTransformations(
            Optional<FeatureTransformations> baseTransformations,
            Map<String, Codelist> codelists,
            String serviceUrl, boolean isOverview) {
        Map<String, ImmutableHtmlPropertyTransformations.Builder> mapBuilder = new LinkedHashMap<>();

        baseTransformations.ifPresent(base -> base.getSchemaTransformations(isOverview)
                                                  .forEach((propertyName, schemaTransformers) -> {
                                                      mapBuilder.putIfAbsent(propertyName, ImmutableHtmlPropertyTransformations.builder());
                                                      mapBuilder.get(propertyName)
                                                                .addAllSchemaTransformers(schemaTransformers);
                                                  }));

        this.getSchemaTransformations(isOverview)
            .forEach((propertyName, schemaTransformers) -> {
                mapBuilder.putIfAbsent(propertyName, ImmutableHtmlPropertyTransformations.builder());
                mapBuilder.get(propertyName)
                          .addAllSchemaTransformers(schemaTransformers);
            });

        baseTransformations.ifPresent(base -> base.getValueTransformations(codelists, serviceUrl)
                                                  .forEach((propertyName, valueTransformers) -> {
                                                      mapBuilder.putIfAbsent(propertyName, ImmutableHtmlPropertyTransformations.builder());
                                                      mapBuilder.get(propertyName)
                                                                .addAllValueTransformers(valueTransformers);
                                                  }));

        this.getValueTransformations(codelists, serviceUrl)
            .forEach((propertyName, valueTransformers) -> {
                mapBuilder.putIfAbsent(propertyName, ImmutableHtmlPropertyTransformations.builder());
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
        return new ImmutableHtmlConfiguration.Builder();
    }
}

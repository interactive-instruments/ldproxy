/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.codelists.domain.ImmutableFeaturePropertyTransformerCodelist;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertySchemaTransformer;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyValueTransformer;
import de.ii.xtraplatform.features.domain.transform.ImmutableFeaturePropertyTransformerDateFormat;
import de.ii.xtraplatform.features.domain.transform.ImmutableFeaturePropertyTransformerRemove;
import de.ii.xtraplatform.features.domain.transform.ImmutableFeaturePropertyTransformerRename;
import de.ii.xtraplatform.stringtemplates.domain.ImmutableFeaturePropertyTransformerStringFormat;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface FeatureTransformations {

    Logger LOGGER = LoggerFactory.getLogger(FeatureTransformations.class);

    Map<String, PropertyTransformation> getTransformations();

    default Map<String, List<FeaturePropertySchemaTransformer>> getSchemaTransformations(boolean isOverview) {
        Map<String, List<FeaturePropertySchemaTransformer>> transformations = new LinkedHashMap<>();

        getTransformations().forEach((property, mapping) -> {
            transformations.putIfAbsent(property, new ArrayList<>());

            mapping.getRename()
                   .ifPresent(rename -> transformations.get(property)
                                                       .add(ImmutableFeaturePropertyTransformerRename.builder()
                                                                                                     .parameter(rename)
                                                                                                     .build()));

            mapping.getRemove()
                   .ifPresent(remove -> transformations.get(property)
                                                       .add(ImmutableFeaturePropertyTransformerRemove.builder()
                                                                                                     .parameter(remove)
                                                                                                     .isOverview(isOverview)
                                                                                                     .build()));
        });

        return transformations;
    }

    default Map<String, List<FeaturePropertyValueTransformer>> getValueTransformations(Map<String, Codelist> codelists,
                                                                                       String serviceUrl) {
        Map<String, List<FeaturePropertyValueTransformer>> transformations = new LinkedHashMap<>();

        getTransformations().forEach((property, mapping) -> {
            transformations.putIfAbsent(property, new ArrayList<>());

            mapping.getNull()
                    .ifPresent(nullValue -> transformations.get(property)
                                                           .add(new ImmutableFeaturePropertyTransformerNullValue.Builder()
                                                                                                            .propertyName(property)
                                                                                                            .parameter(nullValue)
                                                                                                            .build()));

            mapping.getStringFormat()
                   .ifPresent(stringFormat -> transformations.get(property)
                                                             .add(ImmutableFeaturePropertyTransformerStringFormat.builder()
                                                                                                                 .propertyName(property)
                                                                                                                 .parameter(stringFormat)
                                                                                                                 .serviceUrl(serviceUrl)
                                                                                                                 .build()));

            mapping.getDateFormat()
                   .ifPresent(dateFormat -> transformations.get(property)
                                                           .add(ImmutableFeaturePropertyTransformerDateFormat.builder()
                                                                                                             .propertyName(property)
                                                                                                             .parameter(dateFormat)
                                                                                                             .build()));


            mapping.getCodelist()
                   .ifPresent(codelist -> transformations.get(property)
                                                         .add(ImmutableFeaturePropertyTransformerCodelist.builder()
                                                                                                         .propertyName(property)
                                                                                                         .parameter(codelist)
                                                                                                         .codelists(codelists)
                                                                                                         .build()));
        });

        return transformations;
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean hasDeprecatedTransformationKeys() {
        return getTransformations().keySet()
                                   .stream()
                                   .anyMatch(key -> key.matches(".*\\[[^\\]]*\\].*"));
    }

    default Map<String, PropertyTransformation> normalizeTransformationKeys(String buildingBlock, String collectionId) {
        return getTransformations().entrySet()
                                   .stream()
                                   // normalize property names
                                   .map(transformation -> {
                                       if (transformation.getKey().matches(".*\\[[^\\]]*\\].*"))
                                           // TODO use info for now, but upgrade to warn after some time
                                           LOGGER.info("The transformation key '{}' in collection '{}' uses a deprecated style that includes square brackets for arrays. The brackets have been dropped during hydration. Building block: {}.", transformation.getKey(), collectionId, buildingBlock);
                                       return new AbstractMap.SimpleEntry<>(transformation.getKey().replaceAll("\\[[^\\]]*\\]", ""), transformation.getValue());
                                   })
                                   .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.ii.ldproxy.codelists.Codelist;
import de.ii.ldproxy.codelists.ImmutableFeaturePropertyTransformerCodelist;
import de.ii.ldproxy.wfs3.templates.ImmutableFeaturePropertyTransformerStringFormat;
import de.ii.xtraplatform.feature.transformer.api.FeaturePropertySchemaTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeaturePropertyValueTransformer;
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeaturePropertyTransformerDateFormat;
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeaturePropertyTransformerRemove;
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeaturePropertyTransformerRename;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface FeatureTransformations {

    Map<String, FeatureTypeMapping2> getTransformations();

    @Value.Derived
    @JsonIgnore
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

    @Value.Derived
    @JsonIgnore
    default Map<String, List<FeaturePropertyValueTransformer>> getValueTransformations(Map<String, Codelist> codelists,
                                                                                       String serviceUrl) {
        Map<String, List<FeaturePropertyValueTransformer>> transformations = new LinkedHashMap<>();

        getTransformations().forEach((property, mapping) -> {
            transformations.putIfAbsent(property, new ArrayList<>());

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

}

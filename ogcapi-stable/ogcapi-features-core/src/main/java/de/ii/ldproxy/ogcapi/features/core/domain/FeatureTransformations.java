/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.codelists.domain.ImmutableFeaturePropertyTransformerCodelist;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertySchemaTransformer;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyValueTransformer;
import de.ii.xtraplatform.features.domain.transform.ImmutableFeaturePropertyTransformerDateFormat;
import de.ii.xtraplatform.features.domain.transform.ImmutableFeaturePropertyTransformerRemove;
import de.ii.xtraplatform.features.domain.transform.ImmutableFeaturePropertyTransformerRename;
import de.ii.xtraplatform.stringtemplates.domain.ImmutableFeaturePropertyTransformerStringFormat;
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
            String propertyNormalized = property.replaceAll("\\[[^\\]]*\\]", "");
            transformations.putIfAbsent(propertyNormalized, new ArrayList<>());

            mapping.getRename()
                   .ifPresent(rename -> transformations.get(propertyNormalized)
                                                       .add(ImmutableFeaturePropertyTransformerRename.builder()
                                                                                                     .parameter(rename)
                                                                                                     .build()));

            mapping.getRemove()
                   .ifPresent(remove -> transformations.get(propertyNormalized)
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
            String propertyNormalized = property.replaceAll("\\[[^\\]]*\\]", "");
            transformations.putIfAbsent(propertyNormalized, new ArrayList<>());

            mapping.getNull()
                    .ifPresent(nullValue -> transformations.get(propertyNormalized)
                                                           .add(new ImmutableFeaturePropertyTransformerNullValue.Builder()
                                                                                                            .propertyName(property)
                                                                                                            .parameter(nullValue)
                                                                                                            .build()));

            mapping.getStringFormat()
                   .ifPresent(stringFormat -> transformations.get(propertyNormalized)
                                                             .add(ImmutableFeaturePropertyTransformerStringFormat.builder()
                                                                                                                 .propertyName(propertyNormalized)
                                                                                                                 .parameter(stringFormat)
                                                                                                                 .serviceUrl(serviceUrl)
                                                                                                                 .build()));

            mapping.getDateFormat()
                   .ifPresent(dateFormat -> transformations.get(propertyNormalized)
                                                           .add(ImmutableFeaturePropertyTransformerDateFormat.builder()
                                                                                                             .propertyName(propertyNormalized)
                                                                                                             .parameter(dateFormat)
                                                                                                             .build()));


            mapping.getCodelist()
                   .ifPresent(codelist -> transformations.get(propertyNormalized)
                                                         .add(ImmutableFeaturePropertyTransformerCodelist.builder()
                                                                                                         .propertyName(propertyNormalized)
                                                                                                         .parameter(codelist)
                                                                                                         .codelists(codelists)
                                                                                                         .build()));
        });

        return transformations;
    }

}

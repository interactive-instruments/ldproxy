/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertySchemaTransformer;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyValueTransformer;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class FeatureTransformerBase implements FeatureTransformer2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerBase.class);

    final private Optional<PropertyTransformations> coreTransformations;
    final private Optional<PropertyTransformations> buildingBlockTransformations;
    final private Map<String, Codelist> codelists;
    final private String serviceUrl;
    final private boolean isFeatureCollection;

    public enum NESTED_OBJECTS {NEST, FLATTEN}
    public enum MULTIPLICITY {ARRAY, SUFFIX}

    public <T extends ExtensionConfiguration & PropertyTransformations> FeatureTransformerBase(Class<T> buildingBlockConfigurationClass,
                                                                                              OgcApiDataV2 apiData,
                                                                                              String collectionId,
                                                                                              Map<String, Codelist> codelists,
                                                                                              String serviceUrl,
                                                                                              boolean isFeatureCollection) {
        this.codelists = codelists;
        this.serviceUrl = serviceUrl;
        this.isFeatureCollection = isFeatureCollection;

        FeatureTypeConfigurationOgcApi featureType = Objects.nonNull(collectionId) ?
                apiData.getCollections()
                       .get(collectionId) :
                null;
        coreTransformations = Objects.isNull(featureType) ? Optional.empty() : featureType.getExtension(FeaturesCoreConfiguration.class)
                                                                                          .map(configuration -> configuration);
        buildingBlockTransformations = Objects.isNull(featureType) ? Optional.empty() : featureType.getExtension(buildingBlockConfigurationClass)
                                                                                                   .map(configuration -> configuration);
    }

    protected List<FeaturePropertyValueTransformer> getValueTransformations(FeatureProperty featureProperty) {
        List<FeaturePropertyValueTransformer> valueTransformations = null;
        String tkey = featureProperty.getName()
                                     .replaceAll("\\[[^\\]]*\\]", "");
        if (coreTransformations.isPresent()) {
            valueTransformations = coreTransformations.get()
                                                      .getValueTransformations(codelists, Optional.empty(),
                                                          ImmutableMap.of("serviceUrl", serviceUrl))
                                                      .get(tkey);
        }
        if (buildingBlockTransformations.isPresent()) {
            if (Objects.nonNull(valueTransformations)) {
                List<FeaturePropertyValueTransformer> moreTransformations = buildingBlockTransformations.get()
                                                                                                        .getValueTransformations(codelists, Optional.empty(), ImmutableMap.of("serviceUrl", serviceUrl))
                                                                                                        .get(tkey);
                if (Objects.nonNull(moreTransformations)) {
                    valueTransformations = Stream.of(valueTransformations, moreTransformations)
                                                 .flatMap(Collection::stream)
                                                 .collect(ImmutableList.toImmutableList());
                }
            } else {
                valueTransformations = buildingBlockTransformations.get()
                                                                   .getValueTransformations(codelists, Optional.empty(), ImmutableMap.of("serviceUrl", serviceUrl))
                                                                   .get(tkey);
            }
        }

        return Objects.nonNull(valueTransformations) ? valueTransformations : ImmutableList.of();
    }

    protected List<FeaturePropertySchemaTransformer> getSchemaTransformations(FeatureProperty featureProperty) {
        List<FeaturePropertySchemaTransformer> schemaTransformations = null;
        String tkey = featureProperty.getName()
                                     .replaceAll("\\[[^\\]]*\\]", "");
        if (coreTransformations.isPresent()) {
            schemaTransformations = coreTransformations.get()
                                                       .getSchemaTransformations(isFeatureCollection
                                                       )
                                                       .get(tkey);
        }
        if (buildingBlockTransformations.isPresent()) {
            if (Objects.nonNull(schemaTransformations)) {
                List<FeaturePropertySchemaTransformer> moreTransformations = buildingBlockTransformations.get()
                                                                                                         .getSchemaTransformations(false
                                                                                                         )
                                                                                                         .get(tkey);
                if (Objects.nonNull(moreTransformations)) {
                    schemaTransformations = Stream.of(schemaTransformations, moreTransformations)
                                                  .flatMap(Collection::stream)
                                                  .collect(ImmutableList.toImmutableList());
                }
            } else {
                schemaTransformations = buildingBlockTransformations.get()
                                                                    .getSchemaTransformations(isFeatureCollection
                                                                    )
                                                                    .get(tkey);
            }
        }

        return Objects.nonNull(schemaTransformations) ? schemaTransformations : ImmutableList.of();
    }
}

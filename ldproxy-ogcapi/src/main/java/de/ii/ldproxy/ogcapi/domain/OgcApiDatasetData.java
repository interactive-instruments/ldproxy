/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.event.store.EntityDataBuilder;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformerServiceData;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.feature.provider.api.TargetMapping.BASE_TYPE;



@Value.Immutable
@JsonDeserialize(builder = ImmutableOgcApiDatasetData.Builder.class)
public abstract class OgcApiDatasetData extends FeatureTransformerServiceData<FeatureTypeConfigurationOgcApi> implements ExtendableConfiguration {

    public static final String DEFAULT_CRS_URI = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
    public static final EpsgCrs DEFAULT_CRS = new EpsgCrs(4326, true);
    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiDatasetData.class);

    static abstract class Builder implements EntityDataBuilder<OgcApiDatasetData> {
    }

    //@JsonMerge
    //@Nullable
    //public abstract ValueBuilderMap<Test, ImmutableTest.Builder> getTestMap();

    @Value.Default
    @Override
    public String getLabel() {
        return getId();
    }

    //behaves exactly like Map<String, FeatureTypeConfigurationOgcApi>, but supports mergeable builder deserialization
    //(immutables attributeBuilder does not work with maps yet)
    @JsonMerge
    @Override
    public abstract ValueBuilderMap<FeatureTypeConfigurationOgcApi, ImmutableFeatureTypeConfigurationOgcApi.Builder> getFeatureTypes();

    public abstract List<EpsgCrs> getAdditionalCrs();

    //TODO: Optional does not work with nested builders
    @Nullable
    public abstract Metadata getMetadata();

    @Override
    @Value.Derived
    public boolean isLoading() {
        //TODO: not set?
        return Objects.nonNull(getFeatureProvider().getMappingStatus()) && getFeatureProvider().getMappingStatus()
                                   .getLoading();
    }

    public boolean isFeatureTypeEnabled(final String featureType) {
        return getFeatureProvider().isFeatureTypeEnabled(featureType);
    }

    public Map<String, String> getFilterableFieldsForFeatureType(String featureType) {
        return getFilterableFieldsForFeatureType(featureType, false);
    }

    public Map<String, String> getFilterableFieldsForFeatureType(String featureType,
                                                                 boolean withoutSpatialAndTemporal) {
        FeatureTypeMapping featureTypeMapping = getFeatureProvider().getMappings()
                                                                    .get(featureType);

        return Objects.isNull(featureTypeMapping) ? (withoutSpatialAndTemporal ? ImmutableMap.of() : ImmutableMap.of("bbox", "NOT_AVAILABLE")) :
                featureTypeMapping.findMappings(BASE_TYPE)
                                  .entrySet()
                                  .stream()
                                  .filter(isFilterable(withoutSpatialAndTemporal))
                                  .collect(Collectors.toMap(getParameterName(), getParameterValue(), (key1, key2) -> key1));
    }

    //TODO: move to html module
    public Map<String, String> getHtmlNamesForFeatureType(String featureType) {
        FeatureTypeMapping featureTypeMapping = getFeatureProvider().getMappings()
                                                                    .get(featureType);

        Map<String, TargetMapping> baseMappings = Objects.nonNull(featureTypeMapping) ? featureTypeMapping.findMappings(BASE_TYPE) : ImmutableMap.of();

        return Objects.isNull(featureTypeMapping) ? ImmutableMap.of() :
                featureTypeMapping
                        .findMappings("text/html")
                        .entrySet()
                        .stream()
                        .filter(mapping -> mapping.getValue()
                                                  .getName() != null && mapping.getValue()
                                                                               .isEnabled() && baseMappings.get(mapping.getKey())
                                                                                                           .getName() != null)
                        .map(mapping -> new AbstractMap.SimpleImmutableEntry<>(/*TODO getParamterValue()*/baseMappings.get(mapping.getKey())
                                                                                                                      .getName()
                                                                                                                      .replaceAll("\\[\\w+\\]", "")
                                                                                                                      .toLowerCase(), mapping.getValue()
                                                                                                                                             .getName()))
                        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue, (s, s2) -> s));
    }

    private String propertyPathsToCql(String propertyPath) {
        List<String> path = splitPath(propertyPath);

        List<String> shortPath = path.stream()
                                     .map(s -> s.substring(s.lastIndexOf(":") + 1))
                                     .collect(Collectors.toList());

        String joinedPath = shortPath.stream()
                                     .collect(Collectors.joining("."));

        return joinedPath;

        //return propertyPathsToShort(propertyPath).replace('/', '.');
    }

    private String propertyPathsToShort(String propertyPath) {

        return propertyPath.replaceAll("(?:(?:(^| |\\()/)|(/))(?:\\[\\w+=\\w+\\])?(?:\\w+\\()*(\\w+)(?:\\)(?:,| |\\)))*", "$1$2$3");
    }

    private List<String> splitPath(String path) {
        Splitter splitter = path.contains("http://") ? Splitter.onPattern("\\/(?=http)") : Splitter.on("/");
        return splitter.omitEmptyStrings()
                       .splitToList(path);
    }

    private Function<Map.Entry<String, TargetMapping>, String> getParameterName() {
        return mapping -> mapping.getValue()
                .isSpatial() ? "bbox"
                : ((OgcApiFeaturesGenericMapping) mapping.getValue()).isTemporal() ? "datetime"
                : mapping.getValue()
                         .getName()
                         .replaceAll("\\[\\w+\\]", "")
                         .toLowerCase();
    }

    private Function<Map.Entry<String, TargetMapping>, String> getParameterValue() {
        return mapping -> mapping.getValue()
                                 .getName()
                                 .replaceAll("\\[\\w+\\]", "")
                                 .toLowerCase();
    }

    private Predicate<Map.Entry<String, TargetMapping>> isFilterable(boolean withoutSpatialAndTemporal) {
        return mapping -> ((OgcApiFeaturesGenericMapping) mapping.getValue()).isFilterable() &&
                (mapping.getValue()
                        .getName() != null) && // TODO: default name for GML geometries|| mapping.getValue().isSpatial()
                mapping.getValue()
                       .isEnabled() &&
                (!withoutSpatialAndTemporal || (!mapping.getValue()
                                                        .isSpatial() && !((OgcApiFeaturesGenericMapping) mapping.getValue()).isTemporal()));
    }
}

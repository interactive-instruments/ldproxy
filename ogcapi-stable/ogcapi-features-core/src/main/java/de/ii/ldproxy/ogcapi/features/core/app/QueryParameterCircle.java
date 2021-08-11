/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.collections.keyvalue.AbstractMapEntry;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class QueryParameterCircle extends ApiExtensionCache implements OgcApiQueryParameter {

    // TODO this is a temporary extension for Testbed-17, which should not be merged;
    //      if we add support for a center point / radius filter, it should be implemented
    //      using ST_BUFFER() and not be based on nautical miles

    private final Schema baseSchema;

    public QueryParameterCircle() {
        baseSchema = new ArraySchema().items(new NumberSchema().format("double")).minItems(3).maxItems(3);
    }

    @Override
    public String getName() {
        return "circle";
    }

    @Override
    public String getDescription() {
        return "Only features that have a geometry that intersects the circle are selected. " +
                "The circle is provided as three numbers:\n\n" +
                "* longitude of center point\n" +
                "* latitude of center point\n" +
                "* radius in nautical miles \n\n" +
                "The coordinate reference system of the center point is WGS 84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84).\n" +
                "If a feature has multiple spatial geometry properties, it is the decision of the server " +
                "whether only a single spatial geometry property is used to determine the extent or all relevant geometries.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
               method== HttpMethods.GET &&
               definitionPath.equals("/collections/{collectionId}/items"));
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return baseSchema;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        return baseSchema;
    }

    @Override
    public Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureType, Map<String, String> parameters, OgcApiDataV2 apiData) {
        if (parameters.containsKey(getName())) {
            try {
                return parameters.entrySet()
                                 .stream()
                                 .map(entry -> {
                                     if (entry.getKey().equals(getName())) {
                                         List<Double> circle = Splitter.on(",")
                                                                       .trimResults()
                                                                       .splitToStream(entry.getValue())
                                                                       .map(Double::parseDouble)
                                                                       .collect(Collectors.toUnmodifiableList());
                                         if (circle.size()==3) {
                                             double radiusInDegree = circle.get(2) / 60.0;
                                             return new AbstractMap.SimpleImmutableEntry<>("bbox", String.join(",",
                                                                                                               String.valueOf(circle.get(0) - radiusInDegree),
                                                                                                               String.valueOf(circle.get(1) - radiusInDegree),
                                                                                                               String.valueOf(circle.get(0) + radiusInDegree),
                                                                                                               String.valueOf(circle.get(1) + radiusInDegree)));
                                         }
                                     }
                                     return entry;
                                 })
                                 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid values for parameter 'circle': ", e);
            }
        }
        
        return parameters;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }
}

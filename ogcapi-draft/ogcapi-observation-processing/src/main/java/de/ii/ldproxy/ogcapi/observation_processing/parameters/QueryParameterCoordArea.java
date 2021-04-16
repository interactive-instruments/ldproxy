/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryMultiPolygon;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;

@Component
@Provides
@Instantiate
public class QueryParameterCoordArea extends ApiExtensionCache implements OgcApiQueryParameter {

    private final GeometryHelperWKT geometryHelper;
    final FeatureProcessInfo featureProcessInfo;

    public QueryParameterCoordArea(@Requires GeometryHelperWKT geometryHelper,
                                   @Requires FeatureProcessInfo featureProcessInfo) {
        this.geometryHelper = geometryHelper;
        this.featureProcessInfo = featureProcessInfo;
    }

    @Override
    public String getId() {
        return "coordsArea";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"area"));
    }

    @Override
    public String getName() {
        return "coords";
    }

    @Override
    public String getDescription() {
        return "A Well Known Text representation of a (MULTI)POLYGON geometry as defined in Simple Feature Access - Part 1: Common Architecture.";
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return new StringSchema().pattern(geometryHelper.getMultiPolygonRegex()+"|"+geometryHelper.getPolygonRegex());
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return OgcApiQueryParameter.super.isEnabledForApi(apiData) ||
                apiData.getCollections()
                        .values()
                        .stream()
                        .filter(FeatureTypeConfigurationOgcApi::getEnabled)
                        .anyMatch(featureType -> isEnabledForApi(apiData, featureType.getId()));
}

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ObservationProcessingConfiguration.class;
    }

    @Override
    public Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureType,
                                                   Map<String, String> parameters,
                                                   OgcApiDataV2 apiData) {
        if (parameters.containsKey("coordsRef") || parameters.getOrDefault("filter", "").contains("INTERSECTS")) {
            // ignore coord, if coordsRef is provided; the parameter may be processed already, so check filter, too
            parameters.remove(getName());

        } else if (parameters.containsKey(getName())) {
            // TODO support other CRS
            String coord = parameters.get(getName());
            if (!coord.matches(geometryHelper.getMultiPolygonRegex()) && !coord.matches(geometryHelper.getPolygonRegex())) {
                throw new IllegalArgumentException(String.format("The parameter '%s' has an invalid value '%s'.", "coords", coord));
            }

            String spatialPropertyName = getSpatialProperty(apiData, featureType.getId());
            String filter = parameters.get("filter");
            filter = (filter==null? "" : filter+" AND ") + "(INTERSECTS("+spatialPropertyName+","+coord+"))";
            parameters.put("filter",filter);
            parameters.remove(getName());
        }

        return parameters;
    }

    private String getSpatialProperty(OgcApiDataV2 apiData, String collectionId) {
        return apiData.getCollections()
                .get(collectionId)
                .getExtension(FeaturesCoreConfiguration.class)
                .orElseThrow(() -> new RuntimeException(String.format("No configuration found for feature collection '%s'.",collectionId)))
                .getQueryables()
                .orElseThrow(() -> new RuntimeException(String.format("Configuration for feature collection '%s' does not specify any spatial queryable.",collectionId)))
                .getSpatial()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("Configuration for feature collection '%s' does not specify any spatial queryable.",collectionId)));
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                Map<String, Object> context,
                                                Map<String, String> parameters,
                                                OgcApiDataV2 serviceData) {
        if (parameters.containsKey("coordsRef"))
            // ignore coord
            return context;

        String coord = parameters.get(getName());
        if (coord!=null) {
            if (coord.matches(geometryHelper.getMultiPolygonRegex())) {
                context.put("area",new GeometryMultiPolygon(geometryHelper.extractMultiPolygon(coord)));
            } else if (coord.matches(geometryHelper.getPolygonRegex())) {
                List<List<List<List<Double>>>> area = new Vector<>();
                area.add(geometryHelper.extractPolygon(coord));
                context.put("area",new GeometryMultiPolygon(area));
            }
        }
        return context;
    }

    // TODO support default?
    private Optional<String> getDefault(OgcApiDataV2 apiData, Optional<String> collectionId) {
        FeatureTypeConfigurationOgcApi featureType = collectionId.isPresent() ? apiData.getCollections().get(collectionId.get()) : null;
        Optional<ObservationProcessingConfiguration> config = featureType!=null ?
                featureType.getExtension(ObservationProcessingConfiguration.class) :
                apiData.getExtension(ObservationProcessingConfiguration.class);
        if (config.isPresent()) {
            return config.get().getDefaultCoordArea();
        }
        return Optional.empty();
    }

}
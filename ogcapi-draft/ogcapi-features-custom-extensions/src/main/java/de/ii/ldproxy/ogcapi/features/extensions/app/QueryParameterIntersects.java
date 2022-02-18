/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.extensions.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.extensions.domain.FeaturesExtensionsConfiguration;
import de.ii.ldproxy.ogcapi.features.extensions.domain.GeometryHelperWKT;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import java.util.HashMap;
import java.util.Map;

//TODO: http
@Singleton
@AutoBind
public class QueryParameterIntersects extends ApiExtensionCache implements OgcApiQueryParameter {

    private final FeaturesCoreProviders providers;
    private final GeometryHelperWKT geometryHelper;
    //private final HttpClient httpClient;

    @Inject
    public QueryParameterIntersects(FeaturesCoreProviders providers,
                                    GeometryHelperWKT geometryHelper
                                    /*Http http*/) {
        this.providers = providers;
        this.geometryHelper = geometryHelper;
        //this.httpClient = http.getDefaultClient();
    }

    @Override
    public String getId() {
        return "intersects";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                definitionPath.equals("/collections/{collectionId}/items") &&
                method== HttpMethods.GET);
    }

    @Override
    public String getName() {
        return "intersects";
    }

    @Override
    public String getDescription() {
        return "A Well Known Text representation of a geometry as defined in Simple Feature Access - Part 1: Common Architecture " +
            "or a URI that returns a GeoJSON feature with a geometry. Only features are returned that intersect the geometry.";
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return new StringSchema().pattern(geometryHelper.getRegex() + "|" + "^http(?:s)?://.*$");
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return super.isEnabledForApi(apiData, collectionId) &&
            apiData.getCollections().get(collectionId).getEnabled() &&
            apiData.getExtension(FeaturesExtensionsConfiguration.class, collectionId)
                .map(FeaturesExtensionsConfiguration::getIntersectsParameter)
                .orElse(false);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesExtensionsConfiguration.class;
    }

    @Override
    public Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureType,
                                                   Map<String, String> parameters,
                                                   OgcApiDataV2 apiData) {
        // validity against the schema has already been checked
        if (parameters.containsKey(getName())) {
            String intersects = parameters.get(getName());
            String wkt;
            if (intersects.startsWith("http")) {
                try {
                    wkt = getGeometry(intersects);
                } catch (Exception e) {
                    throw new IllegalArgumentException(String.format("HTTP URL '%s' in parameter 'intersects' must be a GeoJSON feature with a geometry. Failure to convert to a geometry: %s", intersects, e.getMessage()), e);
                }
                /* TODO validate WKT conversion? The regex can lead to a Stack overflow, so it has been disabled.
                if (!wkt.matches(geometryHelper.getRegex())) {
                    throw new IllegalStateException(String.format("Response to HTTP URL '%s' in parameter 'intersects' cannot be converted to a WKT geometry: '%s'", intersects, wkt));
                }
                */
            } else {
                wkt = intersects;
            }
            String spatialPropertyName = getPrimarySpatialProperty(apiData, featureType.getId());
            String filter = parameters.get("filter");
            filter = (filter==null? "" : filter+" AND ") + "(INTERSECTS("+spatialPropertyName+","+wkt+"))";

            Map<String, String> newParameters = new HashMap<>(parameters);
            newParameters.put("filter",filter);
            newParameters.remove(getName());
            return ImmutableMap.copyOf(newParameters);
        }

        return parameters;
    }

    private String getPrimarySpatialProperty(OgcApiDataV2 apiData, String collectionId) {
        return providers.getFeatureSchema(apiData, apiData.getCollections().get(collectionId))
            .flatMap(SchemaBase::getPrimaryGeometry)
            .map(FeatureSchema::getName)
            .orElseThrow(() -> new RuntimeException(String.format("Configuration for feature collection '%s' does not specify a primary geometry.", collectionId)));
    }

    private String getGeometry(String coordRef) {
        String response = null;//TODO httpClient.getAsString(coordRef);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try {
            jsonNode = mapper.readTree(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(String.format("Could not parse GeoJSON geometry object: %s", e.getMessage()), e);
        }

        return geometryHelper.convertGeoJsonToWkt(jsonNode);
    }

}
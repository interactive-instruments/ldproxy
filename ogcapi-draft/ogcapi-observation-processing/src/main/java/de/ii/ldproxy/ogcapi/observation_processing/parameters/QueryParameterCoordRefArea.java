/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryMultiPolygon;
import de.ii.xtraplatform.streams.domain.Http;
import de.ii.xtraplatform.streams.domain.HttpClient;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static de.ii.ldproxy.ogcapi.observation_processing.parameters.QueryParameterCoordPosition.BUFFER;
import static de.ii.ldproxy.ogcapi.observation_processing.parameters.QueryParameterCoordPosition.R;

@Component
@Provides
@Instantiate
public class QueryParameterCoordRefArea implements OgcApiQueryParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryParameterCoordRefArea.class);

    private final Schema baseSchema;
    private final GeometryHelperWKT geometryHelper;
    private final FeatureProcessInfo featureProcessInfo;
    private final HttpClient httpClient;

    public QueryParameterCoordRefArea(@Requires GeometryHelperWKT geometryHelper,
                                      @Requires FeatureProcessInfo featureProcessInfo,
                                      @Requires Http http) {
        this.geometryHelper = geometryHelper;
        this.featureProcessInfo = featureProcessInfo;
        this.httpClient = http.getDefaultClient();
        baseSchema = new StringSchema().format("uri");
    }

    @Override
    public String getId() { return "coordsRef-area"; }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"area");
    }

    @Override
    public String getName() {
        return "coordsRef";
    }

    @Override
    public String getDescription() {
        return "A URI that returns a GeoJSON feature. For a polygon or multi-polygon the geometry is used. For other geometries a buffer is added.";
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return baseSchema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class) ||
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
        if (parameters.containsKey("coords")) {
            if (parameters.containsKey(getName())) {
                throw new IllegalArgumentException("Only one of the parameters 'coords' and 'coordsRef' may be provided.");
            }
        }

        String coordRef = parameters.get(getName());
        if (coordRef==null)
            return parameters;

        String coord = geometryHelper.convertMultiPolygonToWkt(new GeometryMultiPolygon(getPolygon(coordRef)).asList());

        String spatialPropertyName = getSpatialProperty(apiData, featureType.getId());
        String filter = parameters.get("filter");
        filter = (filter==null? "" : filter+" AND ") + "(INTERSECTS("+spatialPropertyName+","+coord+"))";
        parameters.put("filter",filter);
        parameters.remove(getName());
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

    private Geometry getPolygon(String coordRef) {
        String response = httpClient.getAsString(coordRef);
        GeoJsonReader geoJsonReader = new GeoJsonReader();

        Geometry geometry = null;
        try {
            geometry = geoJsonReader.read(response);
        } catch (ParseException e) {
            // not a valid reference
        }
        if (geometry==null || geometry.isEmpty()) {
            throw new IllegalArgumentException("The value of the parameter 'coordsRef' (" + coordRef + ") is not a URI that resolves to a GeoJSON feature.");
        }

        if (geometry instanceof Polygon || geometry instanceof MultiPolygon)
            return geometry;

        return geometry.buffer(BUFFER / (R * Math.PI / 180.0));
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                Map<String, Object> context,
                                                Map<String, String> parameters,
                                                OgcApiDataV2 apiData) {
        String coordRef = parameters.get(getName());
        if (coordRef==null)
            return context;

        // coordRef has a higher priority than coord and bbox, so always set "area"
        context.put("area",new GeometryMultiPolygon(getPolygon(coordRef)));
        return context;
    }
}

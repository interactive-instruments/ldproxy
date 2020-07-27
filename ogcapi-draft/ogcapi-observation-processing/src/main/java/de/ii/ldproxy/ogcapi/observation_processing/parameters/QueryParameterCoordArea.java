package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessInfo;
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
public class QueryParameterCoordArea implements OgcApiQueryParameter {

    private final GeometryHelperWKT geometryHelper;
    final FeatureProcessInfo featureProcessInfo;

    public QueryParameterCoordArea(@Requires GeometryHelperWKT geometryHelper,
                                   @Requires FeatureProcessInfo featureProcessInfo) {
        this.geometryHelper = geometryHelper;
        this.featureProcessInfo = featureProcessInfo;
    }

    @Override
    public String getId() {
        return "coordArea";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET &&
                featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"area");
    }

    @Override
    public String getName() {
        return "coord";
    }

    @Override
    public String getDescription() {
        return "A Well Known Text representation of a (MULTI)POLYGON geometry as defined in Simple Feature Access - Part 1: Common Architecture.";
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        return new StringSchema().pattern(geometryHelper.getMultiPolygonRegex()+"|"+geometryHelper.getPolygonRegex());
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData, apiData.getCollections().get(collectionId), ObservationProcessingConfiguration.class);
    }

    @Override
    public Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureType,
                                                   Map<String, String> parameters,
                                                   OgcApiApiDataV2 apiData) {
        if (parameters.containsKey("coordRef") || parameters.getOrDefault("filter", "").contains("INTERSECTS")) {
            // ignore coord, if coordRef is provided; the parameter may be processed already, so check filter, too
            parameters.remove(getName());

        } else if (parameters.containsKey(getName())) {
            // TODO support other CRS
            String coord = parameters.get(getName());
            if (!coord.matches(geometryHelper.getMultiPolygonRegex()) && !coord.matches(geometryHelper.getPolygonRegex())) {
                throw new RuntimeException(String.format("The parameter '%s' has an invalid value '%s'.", "coord", coord));
            }

            String spatialPropertyName = getSpatialProperty(apiData, featureType.getId());
            String filter = parameters.get("filter");
            filter = (filter==null? "" : filter+" AND ") + "(INTERSECTS("+spatialPropertyName+","+coord+"))";
            parameters.put("filter",filter);
            parameters.remove(getName());
        }

        return parameters;
    }

    private String getSpatialProperty(OgcApiApiDataV2 apiData, String collectionId) {
        return apiData.getCollections()
                .get(collectionId)
                .getExtension(OgcApiFeaturesCoreConfiguration.class)
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
                                                OgcApiApiDataV2 serviceData) {
        if (parameters.containsKey("coordRef"))
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
    private Optional<String> getDefault(OgcApiApiDataV2 apiData, Optional<String> collectionId) {
        FeatureTypeConfigurationOgcApi featureType = collectionId.isPresent() ? apiData.getCollections().get(collectionId.get()) : null;
        Optional<ObservationProcessingConfiguration> config = featureType!=null ?
                this.getExtensionConfiguration(apiData, featureType, ObservationProcessingConfiguration.class) :
                this.getExtensionConfiguration(apiData, ObservationProcessingConfiguration.class);
        if (config.isPresent()) {
            return config.get().getDefaultCoordArea();
        }
        return Optional.empty();
    }

}
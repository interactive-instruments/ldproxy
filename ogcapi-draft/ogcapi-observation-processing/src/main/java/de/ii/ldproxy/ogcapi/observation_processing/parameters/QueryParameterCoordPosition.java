package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryPoint;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class QueryParameterCoordPosition implements OgcApiQueryParameter {

    static final double BUFFER = 75.0; // buffer in km
    static public final double R = 6378.1; // earth radius in km

    private final Schema baseSchema;
    private final GeometryHelperWKT geometryHelper;
    final FeatureProcessInfo featureProcessInfo;

    public QueryParameterCoordPosition(@Requires GeometryHelperWKT geometryHelper,
                                       @Requires FeatureProcessInfo featureProcessInfo) {
        this.geometryHelper = geometryHelper;
        this.featureProcessInfo = featureProcessInfo;
        baseSchema = new StringSchema().pattern(geometryHelper.getPointRegex());
    }

    @Override
    public String getId() { return "coordsPosition"; }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"position");
    }

    @Override
    public String getName() {
        return "coords";
    }

    @Override
    public String getDescription() {
        return "A Well Known Text representation of a POINT geometry as defined in Simple Feature Access - Part 1: Common Architecture.";
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        Optional<String> defValue = getDefault(apiData, Optional.empty());
        if (defValue.isPresent()) {
            Schema schema = baseSchema;
            schema.setDefault(defValue.get());
            return schema;
        }
        return baseSchema;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        Optional<String> defValue = getDefault(apiData, Optional.of(collectionId));
        if (defValue.isPresent()) {
            Schema schema = baseSchema;
            schema.setDefault(defValue.get());
            return schema;
        }
        return baseSchema;
    }

    @Override
    public boolean getRequired(OgcApiDataV2 apiData) {
        return !getDefault(apiData, Optional.empty()).isPresent();
    }

    @Override
    public boolean getRequired(OgcApiDataV2 apiData, String collectionId) {
        return !getDefault(apiData, Optional.of(collectionId)).isPresent();
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
        if (parameters.containsKey("coordsRef") || parameters.containsKey("bbox")) {
            // ignore coord, if coordsRef is provided; the parameter may be processed already, so check bbox, too
            parameters.remove(getName());

        } else {
            String coord = parameters.get(getName());
            if (coord == null) {
                if (parameters.get("coordsRef") != null)
                    return parameters;

                coord = getDefault(apiData, Optional.of(featureType.getId())).orElse(null);
                if (coord == null && parameters.get("coordsRef") == null)
                    throw new IllegalArgumentException("One of the 'coords' or 'coordsRef' has to be provided.");
            }

            // TODO support other CRS
            // add bbox and remove coord
            List<Double> point = geometryHelper.extractPosition(coord);
            double lonBuffer = BUFFER / (R * Math.cos(point.get(1) / 180.0 * Math.PI) * Math.PI / 180.0);
            double latBuffer = BUFFER / (R * Math.PI / 180.0);
            String bbox = (point.get(0) - lonBuffer) + "," + (point.get(1) - latBuffer) + "," +
                    (point.get(0) + lonBuffer) + "," + (point.get(1) + latBuffer);
            parameters.put("bbox", bbox);
            parameters.remove(getName());
        }

        return parameters;
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                Map<String, Object> context,
                                                Map<String, String> parameters,
                                                OgcApiDataV2 apiData) {
        if (parameters.containsKey("coordsRef"))
            // ignore coord
            return context;

        String coord = parameters.get(getName());
        if (coord==null) {
            coord = getDefault(apiData, Optional.of(featureType.getId())).orElse(null);
            if (coord == null)
                throw new IllegalArgumentException(String.format("The required parameter '%s' has no value.", getName()));
        }

        context.put("point",new GeometryPoint(geometryHelper.extractPosition(coord)));
        return context;
    }

    private Optional<String> getDefault(OgcApiDataV2 apiData, Optional<String> collectionId) {
        FeatureTypeConfigurationOgcApi featureType = collectionId.isPresent() ? apiData.getCollections().get(collectionId.get()) : null;
        Optional<ObservationProcessingConfiguration> config = featureType!=null ?
                featureType.getExtension(ObservationProcessingConfiguration.class) :
                apiData.getExtension(ObservationProcessingConfiguration.class);
        if (config.isPresent()) {
            return config.get().getDefaultCoordPosition();
        }
        return Optional.empty();
    }
}

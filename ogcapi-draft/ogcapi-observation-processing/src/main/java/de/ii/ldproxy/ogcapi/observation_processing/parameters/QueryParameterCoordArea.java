package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.feature_processing.api.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryMultiPolygon;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ServerErrorException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;

@Component
@Provides
@Instantiate
public class QueryParameterCoordArea extends GeometryHelper implements OgcApiQueryParameter {

    final OgcApiFeatureCoreProviders providers;
    final FeatureProcessInfo featureProcessInfo;

    public QueryParameterCoordArea(@Requires OgcApiFeatureCoreProviders providers,
                                   @Requires FeatureProcessInfo featureProcessInfo) {
        this.providers = providers;
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
                featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"area", "resample-to-grid");
    }

    @Override
    public String getName() {
        return "coord";
    }

    @Override
    public String getDescription() {
        return "A Well Known Text representation of a (MULTI)POLYGON or (MULTI)POLYGONZ geometry as defined in Simple Feature Access - Part 1: Common Architecture.";
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        return new StringSchema().pattern("^\\s*MULTIPOLYGON\\s*" + MULTIPOLYGON_REGEX + "\\s*$|^\\s*POLYGON\\s*" + POLYGON_REGEX + "\\s*$");
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class);
    }

    @Override
    public Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureType,
                                                   Map<String, String> parameters,
                                                   OgcApiApiDataV2 apiData) {
        // TODO support other CRS
        if (parameters.containsKey(getName())) {
            String coord = parameters.get(getName());
            List<List<List<List<Float>>>> area;
            if (coord.matches("^\\s*MULTIPOLYGON\\s*"+MULTIPOLYGON_REGEX+"\\s*$")) {
                area = extractMultiPolygon(coord);
            } else if (coord.matches("^\\s*POLYGON\\s*"+POLYGON_REGEX+"\\s*$")) {
                area = new Vector<>();
                area.add(extractPolygon(coord));
            } else {
                throw new BadRequestException(String.format("The parameter '%s' has an invalid value '%s'.", "coord", coord));
            }

            String spatialPropertyName = getProperties(apiData, featureType.getId(), providers)
                    .stream()
                    .filter(property -> property.isSpatial())
                    .findAny()
                    .map(property -> property.getName())
                    .orElseThrow(() -> new ServerErrorException(String.format("No spatial property found for collection '%s'.", featureType.getId()), 500));
            String filter = parameters.get("filter");
            filter = (filter==null? "" : filter+" AND ") + "(INTERSECTS("+spatialPropertyName+","+coord+"))";
            parameters.put("filter",filter);
            parameters.remove(getName());
        }

        return parameters;
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                Map<String, Object> context,
                                                Map<String, String> parameters,
                                                OgcApiApiDataV2 serviceData) {
        String coord = parameters.get(getName());
        if (coord!=null) {
            if (coord.matches("^\\s*MULTIPOLYGON\\s*"+MULTIPOLYGON_REGEX+"\\s*$")) {
                context.put("area",new GeometryMultiPolygon(extractMultiPolygon(coord)));
            } else if (coord.matches("^\\s*POLYGON\\s*"+POLYGON_REGEX+"\\s*$")) {
                List<List<List<List<Float>>>> area = new Vector<>();
                area.add(extractPolygon(coord));
                context.put("area",new GeometryMultiPolygon(area));
            }
        }
        return context;
    }

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
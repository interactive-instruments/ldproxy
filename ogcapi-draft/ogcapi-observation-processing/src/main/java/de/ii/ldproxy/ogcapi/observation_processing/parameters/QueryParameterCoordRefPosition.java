package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryPoint;
import de.ii.xtraplatform.streams.domain.Http;
import de.ii.xtraplatform.streams.domain.HttpClient;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static de.ii.ldproxy.ogcapi.observation_processing.parameters.QueryParameterCoordPosition.BUFFER;
import static de.ii.ldproxy.ogcapi.observation_processing.parameters.QueryParameterCoordPosition.R;

@Component
@Provides
@Instantiate
public class QueryParameterCoordRefPosition implements OgcApiQueryParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryParameterCoordRefPosition.class);

    private final Schema baseSchema;
    private final FeatureProcessInfo featureProcessInfo;
    private final HttpClient httpClient;

    public QueryParameterCoordRefPosition(@Requires FeatureProcessInfo featureProcessInfo,
                                          @Requires Http http) {
        this.featureProcessInfo = featureProcessInfo;
        this.httpClient = http.getDefaultClient();
        baseSchema = new StringSchema().format("uri");
    }

    @Override
    public String getId() { return "coordsRef-position"; }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"position");
    }

    @Override
    public String getName() {
        return "coordsRef";
    }

    @Override
    public String getDescription() {
        return "A URI that returns a GeoJSON feature. The centroid of the feature geometry is used as the sampling geometry.";
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

        Point point = getPoint(coordRef);

        // TODO support other CRS
        // add bbox and remove coordRef
        double lonBuffer = BUFFER / (R * Math.cos(point.getY() / 180.0 * Math.PI) * Math.PI / 180.0);
        double latBuffer = BUFFER / (R * Math.PI / 180.0);
        String bbox = (point.getX() - lonBuffer) + "," + (point.getY() - latBuffer) + "," +
                      (point.getX() + lonBuffer) + "," + (point.getY() + latBuffer);
        parameters.put("bbox",bbox);
        parameters.remove(getName());
        return parameters;
    }

    private Point getPoint(String coordRef) {
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

        return geometry.getCentroid();
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                Map<String, Object> context,
                                                Map<String, String> parameters,
                                                OgcApiDataV2 apiData) {
        String coordRef = parameters.get(getName());
        if (coordRef==null)
            return context;

        Point point = getPoint(coordRef);

        context.put("point",new GeometryPoint(ImmutableList.of(point.getX(), point.getY())));
        return context;
    }
}

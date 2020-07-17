package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryPoint;
import de.ii.xtraplatform.akka.http.Http;
import de.ii.xtraplatform.akka.http.HttpClient;
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

import javax.ws.rs.BadRequestException;
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
    public String getId() { return "coordRef-position"; }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET &&
                featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"position");
    }

    @Override
    public String getName() {
        return "coordRef";
    }

    @Override
    public String getDescription() {
        return "A URI that returns a GeoJSON feature. The centroid of the feature geometry is used as the sampling geometry.";
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        return baseSchema;
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
        if (parameters.containsKey("coord")) {
            if (parameters.containsKey(getName())) {
                throw new IllegalArgumentException("Only one of the parameters 'coord' and 'coordRef' may be provided.");
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
            throw new BadRequestException("The value of the parameter 'coordRef' ("+coordRef+") is not a URI that resolves to a GeoJSON feature.");
        }

        return geometry.getCentroid();
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                Map<String, Object> context,
                                                Map<String, String> parameters,
                                                OgcApiApiDataV2 apiData) {
        String coordRef = parameters.get(getName());
        if (coordRef==null)
            return context;

        Point point = getPoint(coordRef);

        context.put("point",new GeometryPoint(ImmutableList.of(point.getX(), point.getY())));
        return context;
    }
}

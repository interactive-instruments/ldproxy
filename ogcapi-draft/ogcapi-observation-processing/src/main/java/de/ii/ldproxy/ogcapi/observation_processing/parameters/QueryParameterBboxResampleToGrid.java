package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import com.google.common.base.Splitter;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryMultiPolygon;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.BadRequestException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

import static de.ii.ldproxy.ogcapi.observation_processing.parameters.QueryParameterCoordPosition.BUFFER;
import static de.ii.ldproxy.ogcapi.observation_processing.parameters.QueryParameterCoordPosition.R;

@Component
@Provides
@Instantiate
public class QueryParameterBboxResampleToGrid implements OgcApiQueryParameter {

    private final Schema baseSchema;
    private final GeometryHelperWKT geometryHelper;
    final FeatureProcessInfo featureProcessInfo;

    public QueryParameterBboxResampleToGrid(@Requires GeometryHelperWKT geometryHelper,
                                            @Requires FeatureProcessInfo featureProcessInfo) {
        this.geometryHelper = geometryHelper;
        this.featureProcessInfo = featureProcessInfo;

        // TODO support 6 coordinates
        baseSchema = new ArraySchema().items(new NumberSchema().format("double")).minItems(4).maxItems(4);
    }

    @Override
    public String getId() {
        return "bbox-resample-to-grid";
    }

    @Override
    public String getId(String collectionId) {
        return "bbox-resample-to-grid_"+collectionId;
    }

    @Override
    public String getName() {
        return "bbox";
    }

    @Override
    public String getDescription() {
        return "Only features that have a geometry that intersects the bounding box are selected. " +
                "The bounding box is provided as four numbers:\n\n" +
                "* Lower left corner, coordinate axis 1 \n" +
                "* Lower left corner, coordinate axis 2 \n" +
                "* Upper right corner, coordinate axis 1 \n" +
                "* Upper right corner, coordinate axis 2 \n\n" +
                "The coordinate reference system of the values is WGS 84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84).";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
               method==OgcApiContext.HttpMethods.GET &&
               featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"resample-to-grid");
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        List<Double> defValue = getDefault(apiData, Optional.empty());
        if (defValue!=null) {
            Schema schema = new ArraySchema().items(new NumberSchema().format("double")).minItems(4).maxItems(4);
            schema.setDefault(defValue);
            return schema;
        }
        return baseSchema;
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData, String collectionId) {
        List<Double> defValue = getDefault(apiData, Optional.empty());
        if (defValue!=null) {
            Schema schema = new ArraySchema().items(new NumberSchema().format("double")).minItems(4).maxItems(4);
            schema.setDefault(defValue);
            return schema;
        }
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
        // TODO support bbox-crs and other CRSs
        String bboxParam = parameters.get(getName());
        List<Double> bbox;
        if (bboxParam==null) {
            bbox = getDefault(apiData, Optional.of(featureType.getId()));
            if (bbox==null)
                throw new BadRequestException("Missing parameter 'bbox', no bounding box has been provided.");
        } else {
            bbox = Splitter.on(",").splitToList(bboxParam)
                    .stream()
                    .map(str -> Double.valueOf(str))
                    .collect(Collectors.toList());
        }

        double lonBuffer = BUFFER / (R * Math.cos(bbox.get(1) / 180.0 * Math.PI) * Math.PI / 180.0);
        double latBuffer = BUFFER / (R * Math.PI / 180.0);
        bboxParam = String.valueOf(bbox.get(0) - lonBuffer) + "," + String.valueOf(bbox.get(1) - latBuffer) + "," +
                String.valueOf(bbox.get(2) + lonBuffer) + "," + String.valueOf(bbox.get(3) + latBuffer);
        parameters.put(getName(),bboxParam);

        return parameters;
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType, Map<String, Object> context, Map<String, String> parameters, OgcApiApiDataV2 apiData) {
        if (parameters.containsKey("coordRef"))
            // ignore bbox
            return context;

        String bboxParam = parameters.get(getName());
        List<Double> bbox = null;
        if (bboxParam==null) {
            bbox = getDefault(apiData, Optional.of(featureType.getId()));
            if (bbox==null)
                throw new BadRequestException("Missing parameter 'bbox', no bounding box has been provided.");
        } else if (bboxParam!=null) {
            bbox = Splitter.on(",").splitToList(bboxParam)
                    .stream()
                    .map(str -> Double.valueOf(str))
                    .collect(Collectors.toList());
        }
        if (bbox!=null) {
            List<List<List<List<Double>>>> area = new Vector<>();
            area.add(geometryHelper.convertBboxToPolygon(bbox));
            context.put("area", new GeometryMultiPolygon(area));
        }
        return context;
    }

    private List<Double> getDefault(OgcApiApiDataV2 apiData, Optional<String> collectionId) {
        FeatureTypeConfigurationOgcApi featureType = collectionId.isPresent() ? apiData.getCollections().get(collectionId.get()) : null;
        Optional<ObservationProcessingConfiguration> config = featureType!=null ?
                this.getExtensionConfiguration(apiData, featureType, ObservationProcessingConfiguration.class) :
                this.getExtensionConfiguration(apiData, ObservationProcessingConfiguration.class);
        if (config.isPresent()) {
            return config.get().getDefaultBbox();
        }
        return null;
    }

}

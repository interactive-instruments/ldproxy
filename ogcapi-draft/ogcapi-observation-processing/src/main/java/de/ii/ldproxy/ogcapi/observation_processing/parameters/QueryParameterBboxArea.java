package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import com.google.common.base.Splitter;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessInfo;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class QueryParameterBboxArea implements OgcApiQueryParameter {

    private final Schema baseSchema;
    private final GeometryHelperWKT geometryHelper;
    final FeatureProcessInfo featureProcessInfo;

    public QueryParameterBboxArea(@Requires GeometryHelperWKT geometryHelper,
                                  @Requires FeatureProcessInfo featureProcessInfo) {
        this.geometryHelper = geometryHelper;
        this.featureProcessInfo = featureProcessInfo;

        // TODO support 6 coordinates
        baseSchema = new ArraySchema().items(new NumberSchema().format("double")).minItems(4).maxItems(4);
    }

    @Override
    public String getId() {
        return "bbox-observation-processing";
    }

    @Override
    public String getId(String collectionId) {
        return "bbox-observation-processing_"+collectionId;
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
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
               method== HttpMethods.GET &&
               featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"area");
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        List<Double> defValue = getDefault(apiData, Optional.empty());
        if (defValue!=null) {
            Schema schema = new ArraySchema().items(new NumberSchema().format("double")).minItems(4).maxItems(4);
            schema.setDefault(defValue);
            return schema;
        }
        return baseSchema;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        List<Double> defValue = getDefault(apiData, Optional.empty());
        if (defValue!=null) {
            Schema schema = new ArraySchema().items(new NumberSchema().format("double")).minItems(4).maxItems(4);
            schema.setDefault(defValue);
            return schema;
        }
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
        if (parameters.containsKey("coord") || parameters.containsKey("coordRef") || parameters.getOrDefault("filter", "").contains("INTERSECTS")) {
            // ignore bbox, if coord or coordRef are provided; these parameters may already been processed, so check filter, too
            parameters.remove(getName());

        } else if (!parameters.containsKey(getName())) {
            // TODO support bbox-crs and other CRSs
            List<Double> defValue = getDefault(apiData, Optional.of(featureType.getId()));
            if (defValue!=null)
                parameters.put(getName(), String.join(",", defValue.stream().map(d -> String.valueOf(d)).collect(Collectors.toList())));
        }
        return parameters;
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType, Map<String, Object> context, Map<String, String> parameters, OgcApiDataV2 apiData) {
        if (parameters.containsKey("coord") || parameters.containsKey("coordRef"))
            // ignore bbox
            return context;

        String bboxParam = parameters.get(getName());
        List<Double> bbox = null;
        if (bboxParam==null) {
            bbox = getDefault(apiData, Optional.of(featureType.getId()));
            if (bbox==null)
                throw new IllegalArgumentException("Missing parameter 'bbox', no bounding box has been provided.");
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

    private List<Double> getDefault(OgcApiDataV2 apiData, Optional<String> collectionId) {
        FeatureTypeConfigurationOgcApi featureType = collectionId.isPresent() ? apiData.getCollections().get(collectionId.get()) : null;
        Optional<ObservationProcessingConfiguration> config = featureType!=null ?
                featureType.getExtension(ObservationProcessingConfiguration.class) :
                apiData.getExtension(ObservationProcessingConfiguration.class);
        if (config.isPresent()) {
            return config.get().getDefaultBbox();
        }
        return null;
    }

}

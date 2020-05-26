package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.feature_processing.api.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryMultiPolygon;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
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

@Component
@Provides
@Instantiate
public class QueryParameterBbox extends QueryParameterGeometryHelper implements OgcApiQueryParameter {

    private final Schema baseSchema;
    final FeatureProcessInfo featureProcessInfo;

    public QueryParameterBbox(@Requires FeatureProcessInfo featureProcessInfo) {
        this.featureProcessInfo = featureProcessInfo;

        // TODO support 6 coordinates
        baseSchema = new ArraySchema().items(new NumberSchema().format("double")).minItems(4).maxItems(4);
    }

    @Override
    public String getName() {
        return "bbox";
    }

    @Override
    public String getDescription() {
        return "TODO";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
               method==OgcApiContext.HttpMethods.GET &&
               featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"area", "resample-to-grid");
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        Optional<String> defValue = getDefault(apiData, Optional.empty());
        if (defValue.isPresent()) {
            Schema schema = baseSchema;
            schema.setDefault(defValue.get());
            return schema;
        }
        return baseSchema;
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData, String collectionId) {
        Optional<String> defValue = getDefault(apiData, Optional.of(collectionId));
        if (defValue.isPresent()) {
            Schema schema = baseSchema;
            schema.setDefault(defValue.get());
            return schema;
        }
        return baseSchema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class);
    }

    @Override
    public Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureType,
                                                   Map<String, String> parameters,
                                                   OgcApiApiDataV2 apiData) {
        // TODO support bbox-crs and other CRSs
        if (parameters.containsKey("coord")) {
            if (parameters.containsKey(getName())) {
                throw new BadRequestException("Only one of the parameters 'bbox' and 'coord' may be provided.");
            }
        } else {
            if (!parameters.containsKey(getName())) {
                Optional<String> defValue = getDefault(apiData, Optional.of(featureType.getId()));
                if (defValue.isPresent())
                    parameters.put(getName(), defValue.get());
            }
        }
        return parameters;
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType, Map<String, Object> context, Map<String, String> parameters, OgcApiApiDataV2 apiData) {
        String bbox = parameters.get(getName());
        if (bbox==null)
            bbox = getDefault(apiData, Optional.of(featureType.getId())).orElse(null);
        if (bbox!=null) {
            List<List<List<List<Float>>>> area = new Vector<>();
            area.add(convertBboxToPolygon(bbox));
            context.put("area", new GeometryMultiPolygon(area));
        }
        return context;
    }

    private Optional<String> getDefault(OgcApiApiDataV2 apiData, Optional<String> collectionId) {
        FeatureTypeConfigurationOgcApi featureType = collectionId.isPresent() ? apiData.getCollections().get(collectionId.get()) : null;
        Optional<ObservationProcessingConfiguration> config = featureType!=null ?
                this.getExtensionConfiguration(apiData, featureType, ObservationProcessingConfiguration.class) :
                this.getExtensionConfiguration(apiData, ObservationProcessingConfiguration.class);
        if (config.isPresent()) {
            return config.get().getDefaultBbox();
        }
        return Optional.empty();
    }

}

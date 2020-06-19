package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

@Component
@Provides
@Instantiate
public class QueryParameterWidth implements OgcApiQueryParameter {

    private final Schema baseSchema;
    final FeatureProcessInfo featureProcessInfo;

    public QueryParameterWidth(@Requires FeatureProcessInfo featureProcessInfo) {
        this.featureProcessInfo = featureProcessInfo;
        baseSchema = new NumberSchema();
    }

    @Override
    public String getName() {
        return "width";
    }

    @Override
    public String getDescription() {
        return "The number of grid cells in the horizontal direction.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET &&
                featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"resample-to-grid");
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        OptionalInt defValue = getDefault(apiData, Optional.empty());
        if (defValue.isPresent()) {
            Schema schema = baseSchema;
            schema.setDefault(defValue.getAsInt());
            return schema;
        }
        return baseSchema;
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData, String collectionId) {
        OptionalInt defValue = getDefault(apiData, Optional.of(collectionId));
        if (defValue.isPresent()) {
            Schema schema = baseSchema;
            schema.setDefault(defValue.getAsInt());
            return schema;
        }
        return baseSchema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class);
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                Map<String, Object> context,
                                                Map<String, String> parameters,
                                                OgcApiApiDataV2 apiData) {
        OptionalInt gridWidth;
        if (parameters.containsKey(getName()))
            gridWidth = OptionalInt.of(Integer.valueOf(parameters.get(getName())));
        else
            gridWidth = getDefault(apiData, Optional.of(featureType.getId()));

        context.put(getName(),gridWidth);
        return context;
    }

    private OptionalInt getDefault(OgcApiApiDataV2 apiData, Optional<String> collectionId) {
        FeatureTypeConfigurationOgcApi featureType = collectionId.isPresent() ? apiData.getCollections().get(collectionId.get()) : null;
        Optional<ObservationProcessingConfiguration> config = featureType!=null ?
                this.getExtensionConfiguration(apiData, featureType, ObservationProcessingConfiguration.class) :
                this.getExtensionConfiguration(apiData, ObservationProcessingConfiguration.class);
        if (config.isPresent()) {
            return config.get().getDefaultWidth();
        }
        return OptionalInt.empty();
    }
}

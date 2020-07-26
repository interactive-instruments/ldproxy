package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingStatisticalFunction;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class QueryParameterFunctions implements OgcApiQueryParameter {

    final OgcApiExtensionRegistry extensionRegistry;
    final FeatureProcessInfo featureProcessInfo;

    public QueryParameterFunctions(@Requires OgcApiExtensionRegistry extensionRegistry,
                                   @Requires FeatureProcessInfo featureProcessInfo) {
        this.extensionRegistry = extensionRegistry;
        this.featureProcessInfo = featureProcessInfo;
    }

    @Override
    public String getName() {
        return "functions";
    }

    @Override
    public String getDescription() {
        return "The statistical function(s) to apply when aggregating multiple values to a single value.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET &&
                featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"aggregate-time", "aggregate-space", "aggregate-space-time");
    }

    private Schema schema = null;

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        if (schema==null) {
            List<String> functionsEnum = new ArrayList<>();
            List<String> defaultList = new ArrayList<>();
            getFunctions(apiData, false)
                    .stream()
                    .forEach(f -> {
                        functionsEnum.add(f.getName());
                    });
            getFunctions(apiData, true)
                    .stream()
                    .forEach(f -> {
                        defaultList.add(f.getName());
                    });
            schema = new ArraySchema().items(new StringSchema()._enum(functionsEnum));
            schema.setDefault(defaultList);
        }
        return schema;
    }

    private List<ObservationProcessingStatisticalFunction> getFunctions(OgcApiApiDataV2 apiData, boolean defaultOnly) {
        return extensionRegistry.getExtensionsForType(ObservationProcessingStatisticalFunction.class)
                .stream()
                .filter(function -> function.isEnabledForApi(apiData) && (!defaultOnly || function.isDefault()))
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                Map<String, Object> context,
                                                Map<String, String> parameters,
                                                OgcApiApiDataV2 apiData) {
        List<ObservationProcessingStatisticalFunction> functions;
        if (parameters.containsKey(getName())) {
            List<ObservationProcessingStatisticalFunction> knownFunctions = getFunctions(apiData, false);
            List<String> fs = Splitter.on(",")
                    .trimResults()
                    .omitEmptyStrings()
                    .splitToList(parameters.get(getName()));
            functions = fs.stream()
                    .map(fName -> knownFunctions.parallelStream()
                            .filter(f -> f.getName().equalsIgnoreCase(fName))
                            .findAny())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        } else {
            functions = getFunctions(apiData, true);
        }
        context.put(getName(),functions);
        return context;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class) ||
                apiData.getCollections()
                        .values()
                        .stream()
                        .filter(featureType -> featureType.getEnabled())
                        .filter(featureType -> isEnabledForApi(apiData, featureType.getId()))
                        .findAny()
                        .isPresent();
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData, apiData.getCollections().get(collectionId), ObservationProcessingConfiguration.class);
    }

}

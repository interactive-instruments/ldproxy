package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.application.Variable;
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
public class QueryParameterVariables implements OgcApiQueryParameter {

    final FeatureProcessInfo featureProcessInfo;

    public QueryParameterVariables(@Requires FeatureProcessInfo featureProcessInfo) {
        this.featureProcessInfo = featureProcessInfo;
    }

    @Override
    public String getName() {
        return "variables";
    }

    @Override
    public String getDescription() {
        return "A comma-separated list of values with names of observable properties that should be returned in the response.\n" +
               "\n" +
               "More information about the available properties can be retrieved from the `../variables` path.\n" +
               "\n" +
               "The default is to return all observed properties.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"position", "area", "grid");
    }

    private List<String> getValues(OgcApiDataV2 apiData, String collectionId) {
        Optional<FeatureTypeConfigurationOgcApi> collectionData = Optional.ofNullable(apiData
                .getCollections()
                .get(collectionId));

        return collectionData.flatMap(data -> data.getExtension(ObservationProcessingConfiguration.class))
                .map(ObservationProcessingConfiguration::getVariables)
                .orElse(ImmutableList.of())
                .stream()
                .map(Variable::getId)
                .collect(Collectors.toList());
    }

    private final Schema schema = new ArraySchema().items(new StringSchema());

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return schema;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        ArraySchema collectionSchema = new ArraySchema().items(new StringSchema()._enum(getValues(apiData, collectionId)));
        collectionSchema.setDefault(getValues(apiData, collectionId));
        return collectionSchema;
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
                                                   OgcApiDataV2 serviceData) {
        if (parameters.containsKey("variables")) {
            final Map<String, String> filterableFields = featureType.getExtension(FeaturesCoreConfiguration.class)
                    .map(FeaturesCoreConfiguration::getAllFilterParameters)
                    .orElse(ImmutableMap.of());
            if (!filterableFields.containsKey("observedProperty")) {
                throw new RuntimeException(String.format("The observation collection '%s' has no 'observedProperty' attribute.", featureType.getId()));
            }
            Set<String> variables = new TreeSet<>();
            List<String> vars = Splitter.on(",")
                    .trimResults()
                    .omitEmptyStrings()
                    .splitToList(parameters.get("variables"));
            variables.addAll(vars);
            if (!variables.isEmpty()) {
                String filter = parameters.get("filter");
                filter = (filter==null? "" : filter+" AND ") + "(observedProperty IN ('" + String.join("','", variables) + "'))";
                parameters.put("filter",filter);
            }
            // NOTE: don't remove the parameter, we still need it for the query builder
        }
        return parameters;
    }

    @Override
    public Set<String> getFilterParameters(Set<String> filterParameters, OgcApiDataV2 apiData, String collectionId) {
        return ImmutableSet.<String>builder().addAll(filterParameters).add("filter").build();
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType, Map<String, Object> context, Map<String, String> parameters, OgcApiDataV2 apiData) {
        if (parameters.containsKey("variables")) {
            // the variables have been validated already
            Set<String> variables = new TreeSet<>();
            List<String> vars = Splitter.on(",")
                    .trimResults()
                    .omitEmptyStrings()
                    .splitToList(parameters.get("variables"));
            context.put("variables",vars);
        } else {
            context.put("variables",getValues(apiData, featureType.getId()));
        }
        return context;
    }

}

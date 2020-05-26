package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.feature_processing.api.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
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

import javax.ws.rs.BadRequestException;
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
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET &&
                featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"position", "area", "resample-to-grid");
    }

    private List<String> getValues(OgcApiApiDataV2 apiData, String collectionId) {
        FeatureTypeConfigurationOgcApi collectionData = apiData
                .getCollections()
                .get(collectionId);

        return getExtensionConfiguration(apiData, collectionData, ObservationProcessingConfiguration.class)
                .map(ObservationProcessingConfiguration::getVariables)
                .orElse(ImmutableList.of())
                .stream()
                .map(Variable::getId)
                .collect(Collectors.toList());
    }

    private final Schema schema = new ArraySchema().items(new StringSchema());

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        return schema;
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData, String collectionId) {
        return new ArraySchema().items(new StringSchema()._enum(getValues(apiData, collectionId)));
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class);
    }

    @Override
    public Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureType,
                                                   Map<String, String> parameters,
                                                   OgcApiApiDataV2 serviceData) {
        if (parameters.containsKey("variables")) {
            final Map<String, String> filterableFields = featureType.getExtension(OgcApiFeaturesCoreConfiguration.class)
                    .map(OgcApiFeaturesCoreConfiguration::getAllFilterParameters)
                    .orElse(ImmutableMap.of());
            Set<String> variables = new TreeSet<>();
            List<String> vars = Splitter.on(",")
                    .trimResults()
                    .omitEmptyStrings()
                    .splitToList(parameters.get("variables"));
            for (String varName : vars) {
                if (filterableFields.containsKey("observedProperty")) {
                    variables.add(varName);
                } else {
                    throw new BadRequestException(String.format("The parameter '%s' includes an unknown variable '%s'.", "variables", varName));
                }
            }
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
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType, Map<String, Object> context, Map<String, String> parameters, OgcApiApiDataV2 serviceData) {
        if (parameters.containsKey("variables")) {
            // the variables have been validated already
            Set<String> variables = new TreeSet<>();
            List<String> vars = Splitter.on(",")
                    .trimResults()
                    .omitEmptyStrings()
                    .splitToList(parameters.get("variables"));
            context.put("variables",vars);
        }
        return context;
    }

}

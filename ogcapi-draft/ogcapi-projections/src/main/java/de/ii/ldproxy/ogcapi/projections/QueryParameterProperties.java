package de.ii.ldproxy.ogcapi.projections;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.geojson.domain.SchemaGeneratorFeature;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Provides
@Instantiate
public class QueryParameterProperties implements OgcApiQueryParameter {

    @Requires
    SchemaGeneratorFeature schemaGeneratorFeature;

    @Override
    public String getId(String collectionId) {
        return "properties_"+collectionId;
    }

    @Override
    public String getName() {
        return "properties";
    }

    @Override
    public String getDescription() {
        return "The properties that should be included for each feature. The parameter value is a comma-separated list of property names.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/collections/{collectionId}/items/{featureId}") ||
                 definitionPath.endsWith("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"));
    }

    private ConcurrentMap<Integer, ConcurrentMap<String,Schema>> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
            schemaMap.get(apiHashCode)
                     .put(collectionId, new ArraySchema().items(new StringSchema()._enum(schemaGeneratorFeature.getPropertyNames(apiData, collectionId))));
        }
        return schemaMap.get(apiHashCode).get(collectionId);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ProjectionsConfiguration.class;
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                        ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters, OgcApiDataV2 datasetData) {

        if (!isExtensionEnabled(datasetData, ProjectionsConfiguration.class)) {
            return queryBuilder;
        }
        List<String> propertiesList = getPropertiesList(parameters);

        return queryBuilder.fields(propertiesList);
    }

    private List<String> getPropertiesList(Map<String, String> parameters) {
        if (parameters.containsKey("properties")) {
            return Splitter.on(',')
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(parameters.get("properties"));
        } else {
            return ImmutableList.of("*");
        }
    }
}

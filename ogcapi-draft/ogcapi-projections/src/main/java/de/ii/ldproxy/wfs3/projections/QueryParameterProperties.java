package de.ii.ldproxy.wfs3.projections;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.target.geojson.SchemaGeneratorFeature;
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
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/collections/{collectionId}/items/{featureId}") ||
                 definitionPath.endsWith("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"));
    }

    private Map<String,Schema> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData, String collectionId) {
        String key = apiData.getId()+"__"+collectionId;
        if (!schemaMap.containsKey(key)) {
            schemaMap.put(key, new ArraySchema().items(new StringSchema()._enum(schemaGeneratorFeature.getPropertyNames(apiData, collectionId))));
        }
        return schemaMap.get(key);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ProjectionsConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        FeatureTypeConfigurationOgcApi featureType = apiData.getCollections().get(collectionId);
        return isExtensionEnabled(apiData, featureType, ProjectionsConfiguration.class);
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                        ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters, OgcApiApiDataV2 datasetData) {

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

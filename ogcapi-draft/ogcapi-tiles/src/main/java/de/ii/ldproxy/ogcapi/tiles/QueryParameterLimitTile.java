package de.ii.ldproxy.ogcapi.tiles;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.features.core.application.QueryParameterLimitFeatures;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Provides
@Instantiate
public class QueryParameterLimitTile extends QueryParameterLimitFeatures {

    @Override
    public String getId() {
        return "limitTile";
    }

    @Override
    public String getDescription() {
        return "The optional limit parameter limits the number of features that are included in the tile.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET &&
                definitionPath.endsWith("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}");
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, String collectionId, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData, collectionId) &&
                method==OgcApiContext.HttpMethods.GET &&
                definitionPath.endsWith("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}");
    }

    private Map<String,Schema> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        String key = apiData.getId()+"_*";
        if (!schemaMap.containsKey(key)) {
            Schema schema = new IntegerSchema().minimum(BigDecimal.valueOf(0));

            Optional<Integer> limit = getExtensionConfiguration(apiData, TilesConfiguration.class)
                    .map(TilesConfiguration::getLimit);
            if (limit.isPresent())
                schema.setDefault(BigDecimal.valueOf(limit.get()));

            schemaMap.put(key, schema);
        }
        return schemaMap.get(key);
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData, String collectionId) {
        String key = apiData.getId()+"_"+collectionId;
        if (!schemaMap.containsKey(key)) {
            Schema schema = new IntegerSchema().minimum(BigDecimal.valueOf(0));

            FeatureTypeConfigurationOgcApi featureType = apiData.getCollections().get(collectionId);
            Optional<Integer> limit = getExtensionConfiguration(apiData, featureType, TilesConfiguration.class)
                    .map(TilesConfiguration::getLimit);
            if (limit.isPresent())
                schema.setDefault(BigDecimal.valueOf(limit.get()));

            schemaMap.put(key, schema);
        }
        return schemaMap.get(key);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData, apiData.getCollections().get(collectionId), TilesConfiguration.class);
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureType,
                                                        ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters,
                                                        OgcApiApiDataV2 apiData) {
        if (parameters.containsKey(getName())) {
            queryBuilder.limit(Integer.parseInt(parameters.get(getName())));
        }

        return queryBuilder;
    }
}

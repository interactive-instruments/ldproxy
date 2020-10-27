package de.ii.ldproxy.ogcapi.tiles;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
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
import java.util.concurrent.ConcurrentMap;

@Component
@Provides
@Instantiate
public class QueryParameterLimitTile implements OgcApiQueryParameter {

    @Override
    public String getId() {
        return "limitTile";
    }

    @Override
    public String getName() {
        return "limit";
    }

    @Override
    public String getDescription() {
        return "The optional limit parameter limits the number of features that are included in the tile.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                definitionPath.endsWith("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}");
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId, HttpMethods method) {
        return isEnabledForApi(apiData, collectionId) &&
                method== HttpMethods.GET &&
                definitionPath.endsWith("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}");
    }

    private ConcurrentMap<Integer, Map<String,Schema>> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey("*")) {
            Schema schema = new IntegerSchema().minimum(BigDecimal.valueOf(0));

            Optional<Integer> limit = apiData.getExtension(TilesConfiguration.class)
                    .map(TilesConfiguration::getLimit);
            if (limit.isPresent())
                schema.setDefault(BigDecimal.valueOf(limit.get()));

            schemaMap.get(apiHashCode)
                     .put("*", schema);
        }
        return schemaMap.get(apiHashCode).get("*");
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
            Schema schema = new IntegerSchema().minimum(BigDecimal.valueOf(0));

            FeatureTypeConfigurationOgcApi featureType = apiData.getCollections().get(collectionId);
            Optional<Integer> limit = featureType.getExtension(TilesConfiguration.class)
                    .map(TilesConfiguration::getLimit);
            if (limit.isPresent())
                schema.setDefault(BigDecimal.valueOf(limit.get()));

            schemaMap.get(apiHashCode)
                     .put(collectionId, schema);
        }
        return schemaMap.get(apiHashCode)
                        .get(collectionId);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), TilesConfiguration.class);
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureType,
                                                        ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters,
                                                        OgcApiDataV2 apiData) {
        if (parameters.containsKey(getName())) {
            queryBuilder.limit(Integer.parseInt(parameters.get(getName())));
        }

        return queryBuilder;
    }
}

package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.target.geojson.SchemaGeneratorFeature;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class QueryParameterCollections implements OgcApiQueryParameter {

    @Override
    public String getName() {
        return "collections";
    }

    @Override
    public String getDescription() {
        return "The collections that should be included. The parameter value is a comma-separated list of collection names.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
               method==OgcApiContext.HttpMethods.GET &&
               definitionPath.equals("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}");
    }

    private Map<String,Schema> schemaMap = new ConcurrentHashMap<>();
    private Map<String,List<String>> collectionsMap = new ConcurrentHashMap<>();

    private List<String> getCollectionIds(OgcApiApiDataV2 apiData) {
        String key = apiData.getId();
        if (!collectionsMap.containsKey(key)) {
            collectionsMap.put(key, apiData.getCollections()
                    .values()
                    .stream()
                    .filter(collection -> apiData.isCollectionEnabled(collection.getId()))
                    .filter(collection -> collection.getExtension(TilesConfiguration.class).filter(ExtensionConfiguration::isEnabled).isPresent())
                    .map(FeatureTypeConfiguration::getId)
                    .collect(Collectors.toList()));
        }
        return collectionsMap.get(key);
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        String key = apiData.getId();
        if (!schemaMap.containsKey(key)) {
            schemaMap.put(key, new ArraySchema().items(new StringSchema()._enum(getCollectionIds(apiData))));
        }
        return schemaMap.get(key);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<TilesConfiguration> extension = apiData.getExtension(TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::isEnabled)
                .filter(TilesConfiguration::getMultiCollectionEnabled)
                .isPresent();
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                Map<String, Object> context,
                                                Map<String, String> parameters,
                                                OgcApiApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return context;

        List<String> availableCollections = getCollectionIds(apiData);
        List<String> requestedCollections = getCollectionsList(parameters);
        context.put("collections", requestedCollections.isEmpty() ? availableCollections :
                                                                    requestedCollections.stream()
                                                                                        .filter(collectionId -> availableCollections.contains(collectionId))
                                                                                        .collect(Collectors.toList()));

        return context;
    }

    private List<String> getCollectionsList(Map<String, String> parameters) {
        if (parameters.containsKey("collections")) {
            return Splitter.on(',')
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(parameters.get("collections"));
        } else {
            return ImmutableList.of();
        }
    }
}

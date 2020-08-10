package de.ii.ldproxy.ogcapi.domain;


import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Component
@Provides
@Instantiate
public class PathParameterCollectionIdCollections implements OgcApiPathParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterCollectionIdCollections.class);
    Map<String,Set<String>> apiCollectionMap;

    public PathParameterCollectionIdCollections() {
        apiCollectionMap = new HashMap<>();
    };

    @Override
    public String getPattern() {
        return "[\\w\\-]+";
    }

    @Override
    public boolean getExplodeInOpenApi() {
        return true;
    }

    @Override
    public Set<String> getValues(OgcApiApiDataV2 apiData) {
        if (!apiCollectionMap.containsKey(apiData.getId())) {
            apiCollectionMap.put(apiData.getId(), apiData.getCollections().keySet().stream()
                                                         .filter(collectionId -> apiData.isCollectionEnabled(collectionId))
                                                         .collect(Collectors.toSet()));
        }

        return apiCollectionMap.get(apiData.getId());
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        return new StringSchema()._enum(ImmutableList.copyOf(getValues(apiData)));
    }

    @Override
    public String getId() {
        return "collectionIdCollections";
    }

    @Override
    public String getName() {
        return "collectionId";
    }

    @Override
    public String getDescription() {
        return "The local identifier of a feature collection.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
                definitionPath.equals("/collections/{collectionId}");
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiCommonConfiguration.class);
    }
}

package de.ii.ldproxy.ogcapi.features.core.application;


import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
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
public class PathParameterCollectionIdFeatures implements OgcApiPathParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterCollectionIdFeatures.class);
    Map<String,Set<String>> apiCollectionMap;

    final OgcApiFeatureCoreProviders providers;

    public PathParameterCollectionIdFeatures(@Requires OgcApiFeatureCoreProviders providers) {
        this.providers = providers;
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
                (definitionPath.matches("/collections/\\{collectionId\\}/items(?:/\\{featureId\\})?") ||
                 definitionPath.matches("/collections/\\{collectionId\\}/context"));
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return true;
    }
}

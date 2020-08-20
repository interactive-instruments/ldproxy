package de.ii.ldproxy.ogcapi.features.core.application;


import de.ii.ldproxy.ogcapi.collections.domain.PathParameterCollectionIdCollections;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Component
@Provides
@Instantiate
public class PathParameterCollectionIdFeatures extends PathParameterCollectionIdCollections {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterCollectionIdFeatures.class);
    Map<String,Set<String>> apiCollectionMap;

    protected final OgcApiFeatureCoreProviders providers;

    public PathParameterCollectionIdFeatures(@Requires OgcApiFeatureCoreProviders providers) {
        this.providers = providers;
        apiCollectionMap = new HashMap<>();
    };

    @Override
    public String getId() {
        return "collectionIdFeatures";
    }

    @Override
    public String getDescription() {
        return "The local identifier of a feature collection.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/collections/{collectionId}/items/{featureId}") ||
                 definitionPath.equals("/collections/{collectionId}/context"));
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiFeaturesCoreConfiguration.class);
    }
}

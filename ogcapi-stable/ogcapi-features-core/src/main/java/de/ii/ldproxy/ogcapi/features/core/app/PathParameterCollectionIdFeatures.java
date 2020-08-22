package de.ii.ldproxy.ogcapi.features.core.app;


import de.ii.ldproxy.ogcapi.collections.domain.PathParameterCollectionIdCollections;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.OgcApiFeaturesCoreConfiguration;
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

    protected final FeaturesCoreProviders providers;

    public PathParameterCollectionIdFeatures(@Requires FeaturesCoreProviders providers) {
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
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/collections/{collectionId}/items/{featureId}") ||
                 definitionPath.equals("/collections/{collectionId}/context"));
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        boolean dbg = isExtensionEnabled(apiData, OgcApiFeaturesCoreConfiguration.class);
        return isExtensionEnabled(apiData, OgcApiFeaturesCoreConfiguration.class);
    }
}

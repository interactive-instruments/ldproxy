package de.ii.ldproxy.ogcapi.features.core.app;


import de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;


@Component
@Provides
@Instantiate
public class PathParameterCollectionIdFeatures extends AbstractPathParameterCollectionId {

    @Override
    public String getId() {
        return "collectionIdFeatures";
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
        return isExtensionEnabled(apiData, FeaturesCoreConfiguration.class);
    }
}

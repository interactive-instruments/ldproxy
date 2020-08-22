package de.ii.ldproxy.ogcapi.collections.queryables;


import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.app.PathParameterCollectionIdFeatures;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
@Provides
@Instantiate
public class PathParameterCollectionIdQueryables extends PathParameterCollectionIdFeatures {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterCollectionIdQueryables.class);

    public PathParameterCollectionIdQueryables(@Requires FeaturesCoreProviders providers) {
        super(providers);
    };

    @Override
    public String getId() {
        return "collectionIdQueryables";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) && definitionPath.matches("/collections/\\{collectionId\\}/queryables");
    }
}

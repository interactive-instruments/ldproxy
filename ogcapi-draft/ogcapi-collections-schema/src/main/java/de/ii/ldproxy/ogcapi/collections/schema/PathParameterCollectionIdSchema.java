package de.ii.ldproxy.ogcapi.collections.schema;


import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.application.PathParameterCollectionIdFeatures;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
@Provides
@Instantiate
public class PathParameterCollectionIdSchema extends PathParameterCollectionIdFeatures {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterCollectionIdSchema.class);

    public PathParameterCollectionIdSchema(@Requires OgcApiFeatureCoreProviders providers) {
        super(providers);
    };

    @Override
    public String getId() {
        return "collectionIdSchema";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) && definitionPath.matches("/collections/\\{collectionId\\}/schema");
    }
}

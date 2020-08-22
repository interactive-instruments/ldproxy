package de.ii.ldproxy.ogcapi.features.target.geojson.ld;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.api.FeaturesCoreProviders;
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
public class PathParameterCollectionIdGeoJsonLd extends PathParameterCollectionIdFeatures {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterCollectionIdGeoJsonLd.class);

    public PathParameterCollectionIdGeoJsonLd(@Requires FeaturesCoreProviders providers) {
        super(providers);
    };

    @Override
    public String getId() {
        return "collectionIdJsonLdContext";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) && definitionPath.equals("/collections/{collectionId}/context");
    }
}

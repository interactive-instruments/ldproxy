package de.ii.ldproxy.ogcapi.features.geojsonld.app;

import de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides
@Instantiate
public class PathParameterCollectionIdGeoJsonLd extends AbstractPathParameterCollectionId {

    @Override
    public String getId() {
        return "collectionIdJsonLdContext";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) && definitionPath.equals("/collections/{collectionId}/context");
    }
}

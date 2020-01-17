package de.ii.ldproxy.ogcapi.features.core.api;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.xtraplatform.feature.provider.api.FeatureProvider2;

public interface OgcApiFeatureCoreProviders {

    FeatureProvider2 getFeatureProvider(OgcApiApiDataV2 apiData);

    FeatureProvider2 getFeatureProvider(OgcApiApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureType);
}

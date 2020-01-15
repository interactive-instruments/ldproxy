package de.ii.ldproxy.ogcapi.features.core.api;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.xtraplatform.feature.provider.api.FeatureProvider2;

public interface OgcApiFeatureCoreProviders {

    FeatureProvider2 getFeatureProvider(OgcApiDatasetData apiData);

    FeatureProvider2 getFeatureProvider(OgcApiDatasetData apiData, FeatureTypeConfigurationOgcApi featureType);
}

package de.ii.ldproxy.ogcapi.features.core.application;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;

import java.util.Map;

public interface OgcApiFeaturesQuery {
    FeatureQuery requestToFeatureQuery(OgcApiDataset api, String collectionId, Map<String, String> parameters,
                                       String featureId);

    FeatureQuery requestToFeatureQuery(OgcApiDataset api, String collectionId, int minimumPageSize,
                                       int defaultPageSize, int maxPageSize, Map<String, String> parameters);
}

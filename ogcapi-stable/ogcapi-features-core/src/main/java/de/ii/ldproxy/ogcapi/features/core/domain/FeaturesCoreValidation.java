package de.ii.ldproxy.ogcapi.features.core.domain;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface FeaturesCoreValidation {
    List<String> getCollectionsWithoutType(OgcApiDataV2 apiData, Map<String, FeatureSchema> featureSchemas);

    List<String> getInvalidPropertyKeys(Collection<String> keys, FeatureSchema schema);

    Map<String, Collection<String>> getInvalidPropertyKeys(Map<String, Collection<String>> keyMap, Map<String, FeatureSchema> featureSchemas);
}

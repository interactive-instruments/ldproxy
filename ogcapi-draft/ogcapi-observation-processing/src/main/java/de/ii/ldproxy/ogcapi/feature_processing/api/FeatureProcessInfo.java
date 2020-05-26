package de.ii.ldproxy.ogcapi.feature_processing.api;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;

import java.util.List;

public interface FeatureProcessInfo {

    List<FeatureProcessChain> getProcessingChains(OgcApiApiDataV2 apiData, String collectionId,
                                                  Class<? extends FeatureProcess> processType);
    List<FeatureProcessChain> getProcessingChains(OgcApiApiDataV2 apiData,
                                                  Class<? extends FeatureProcess> processType);
    boolean matches(OgcApiApiDataV2 apiData, Class<? extends FeatureProcess> processType,
                    String definitionPath, String... processNames);
}

package de.ii.ldproxy.ogcapi.observation_processing.api;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcess;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;

public interface ObservationProcess extends FeatureProcess {

    @Override
    default boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class) ||
                apiData.getCollections()
                        .values()
                        .stream()
                        .filter(featureType -> featureType.getEnabled())
                        .filter(featureType -> isEnabledForApi(apiData, featureType.getId()))
                        .findAny()
                        .isPresent();
    }

    @Override
    default boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData, apiData.getCollections().get(collectionId), ObservationProcessingConfiguration.class);
    }

}

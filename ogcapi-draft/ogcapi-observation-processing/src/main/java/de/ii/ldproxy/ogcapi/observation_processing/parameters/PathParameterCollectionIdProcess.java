package de.ii.ldproxy.ogcapi.observation_processing.parameters;


import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Provides
@Instantiate
public class PathParameterCollectionIdProcess extends AbstractPathParameterCollectionId {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterCollectionIdProcess.class);
    private static final String DAPA_PATH_ELEMENT = "dapa";

    private final FeaturesCoreProviders providers;
    private final FeatureProcessInfo featureProcessInfo;

    public PathParameterCollectionIdProcess(@Requires FeaturesCoreProviders providers,
                                            @Requires FeatureProcessInfo featureProcessInfo) {
        this.providers = providers;
        this.featureProcessInfo = featureProcessInfo;
    };

    @Override
    public Set<String> getValues(OgcApiDataV2 apiData) {
        if (!apiCollectionMap.containsKey(apiData.hashCode()))
            apiCollectionMap.put(apiData.hashCode(),returnObservationCollections(apiData));

        return apiCollectionMap.get(apiData.hashCode());
    }

    @Override
    public String getId() {
        return "collectionIdObservationProcessing";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
                (featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"*") ||
                 definitionPath.equals("/collections/{collectionId}/"+DAPA_PATH_ELEMENT+"/variables") ||
                 definitionPath.equals("/collections/{collectionId}/"+DAPA_PATH_ELEMENT));
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class) ||
                apiData.getCollections()
                        .values()
                        .stream()
                        .filter(FeatureTypeConfigurationOgcApi::getEnabled)
                        .anyMatch(featureType -> isEnabledForApi(apiData, featureType.getId()));
}

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), ObservationProcessingConfiguration.class);
    }

    private Set<String> returnObservationCollections(OgcApiDataV2 apiData) {
        ImmutableSet.Builder<String> collections = ImmutableSet.builder();
        apiData.getCollections().entrySet().stream()
                .forEachOrdered(entry -> {
                    String collectionId = entry.getKey();

                    // check first, if the collection is enabled and also for this extension
                    if (!apiData.isCollectionEnabled(collectionId) ||
                            !apiData.getCollections()
                                    .get(collectionId)
                                    .getExtension(ObservationProcessingConfiguration.class)
                                    .map(Optional::of)
                                    .orElse(apiData.getExtension(ObservationProcessingConfiguration.class))
                                    .map(ObservationProcessingConfiguration::isEnabled)
                                    .orElse(false)) {
                        return;
                    }

                    String featureTypeId = apiData.getCollections()
                                                  .get(collectionId)
                                                  .getExtension(FeaturesCoreConfiguration.class)
                                                  .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                                  .orElse(collectionId);

                    FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, apiData.getCollections().get(collectionId));
                    FeatureSchema featureType = featureProvider.getData()
                            .getTypes()
                            .get(featureTypeId);
                    List<FeatureSchema> featureProperties = featureType.getProperties();

                    for (String requiredProperty: new String[]{"observedProperty", "result", "phenomenonTime"}) {
                        // note: this also checks implicitly that these have literal values (no object, no array)
                        if (!featureProperties.stream().anyMatch(property -> property.getName().equals(requiredProperty))) {
                            LOGGER.info("Building block OBSERVATION_PROCESSING deactivated for collection '{}'. Features with a property '{}' are required.", collectionId, requiredProperty);
                            return;
                        }
                        Optional<FeaturesCollectionQueryables> queryables = apiData.getCollections()
                                                                                   .get(collectionId)
                                                                                   .getExtension(FeaturesCoreConfiguration.class)
                                                                                   .flatMap(FeaturesCoreConfiguration::getQueryables);
                        if (queryables.isPresent()) {
                            if (requiredProperty.equals("phenomenonTime")) {
                                if (queryables.get().getTemporal().stream()
                                        .noneMatch(queryable -> queryable.equals(requiredProperty))) {
                                    LOGGER.info("Building block OBSERVATION_PROCESSING deactivated for collection '{}'. Features with a temporal queryable '{}' are required.", collectionId, requiredProperty);
                                    return;
                                }
                            } else {
                                if (queryables.get().getOther().stream()
                                        .noneMatch(queryable -> queryable.equals(requiredProperty))) {
                                    LOGGER.info("Building block OBSERVATION_PROCESSING deactivated for collection '{}'. Features with a queryable '{}' are required.", collectionId, requiredProperty);
                                    return;
                                }
                            }
                        } else {
                            LOGGER.info("Building block OBSERVATION_PROCESSING deactivated for collection '{}'. Queryable properties are required.", collectionId);
                            return;
                        }
                    }

                    if (!featureProperties.stream()
                                          // TODO check that geometry type is point
                                          .anyMatch(property -> property.isSpatial())) {
                        LOGGER.info("Building block OBSERVATION_PROCESSING deactivated for collection '{}'. Features with a point geometry are required, but no spatial property was found.", collectionId);
                        return;
                    }

                    collections.add(collectionId);
                });
        return collections.build();
    }
}

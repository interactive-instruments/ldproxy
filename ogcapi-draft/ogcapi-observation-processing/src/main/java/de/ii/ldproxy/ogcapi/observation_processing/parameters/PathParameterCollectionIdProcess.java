package de.ii.ldproxy.ogcapi.observation_processing.parameters;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.application.PathParameterCollectionIdFeatures;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.xtraplatform.features.domain.*;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component
@Provides
@Instantiate
public class PathParameterCollectionIdProcess extends PathParameterCollectionIdFeatures {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterCollectionIdProcess.class);
    private static final String DAPA_PATH_ELEMENT = "dapa";
    Map<String,Set<String>> apiCollectionMap;

    final FeatureProcessInfo featureProcessInfo;

    public PathParameterCollectionIdProcess(@Requires OgcApiFeatureCoreProviders providers,
                                            @Requires FeatureProcessInfo featureProcessInfo) {
        super(providers);
        this.featureProcessInfo = featureProcessInfo;
        apiCollectionMap = new HashMap<>();
    };

    @Override
    public Set<String> getValues(OgcApiApiDataV2 apiData) {
        if (!apiCollectionMap.containsKey(apiData.getId()))
            apiCollectionMap.put(apiData.getId(),returnObservationCollections(apiData));

        return apiCollectionMap.get(apiData.getId());
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        return new StringSchema()._enum(ImmutableList.copyOf(getValues(apiData)));
    }

    @Override
    public String getId() {
        return "collectionIdObservationProcessing";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
                (featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"*") ||
                 definitionPath.equals("/collections/{collectionId}/"+DAPA_PATH_ELEMENT+"/variables") ||
                 definitionPath.equals("/collections/{collectionId}/"+DAPA_PATH_ELEMENT));
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class);
    }

    private Set<String> returnObservationCollections(OgcApiApiDataV2 apiData) {
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
                                    .map(ObservationProcessingConfiguration::getEnabled)
                                    .orElse(false)) {
                        return;
                    }

                    FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, apiData.getCollections().get(collectionId));
                    FeatureSchema featureType = featureProvider.getData()
                            .getTypes()
                            .get(collectionId);
                    List<FeatureSchema> featureProperties = featureType.getProperties();

                    for (String requiredProperty: new String[]{"observedProperty", "result", "phenomenonTime"}) {
                        // note: this also checks implicitly that these have literal values (no object, no array)
                        if (!featureProperties.stream().anyMatch(property -> property.getName().equals(requiredProperty))) {
                            LOGGER.info("Building block OBSERVATION_PROCESSING deactivated for collection '{}'. Features with a property '{}' are required.", collectionId, requiredProperty);
                            return;
                        }
                        Optional<OgcApiFeaturesCollectionQueryables> queryables = apiData.getCollections()
                                .get(collectionId)
                                .getExtension(OgcApiFeaturesCoreConfiguration.class)
                                .flatMap(OgcApiFeaturesCoreConfiguration::getQueryables);
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
                            .filter(property -> property.isSpatial())
                            // TODO check that geometry type is point
                            .findAny()
                            .isPresent()) {
                        LOGGER.info("Building block OBSERVATION_PROCESSING deactivated for collection '{}'. Features with a point geometry are required, but no spatial property was found.", collectionId);
                        return;
                    }

                    collections.add(collectionId);
                });
        return collections.build();
    }
}

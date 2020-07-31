package de.ii.ldproxy.ogcapi.tiles;


import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.application.PathParameterCollectionIdFeatures;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Component
@Provides
@Instantiate
public class PathParameterCollectionIdTiles extends PathParameterCollectionIdFeatures {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterCollectionIdTiles.class);
    Map<String,Set<String>> apiCollectionMap;

    public PathParameterCollectionIdTiles(@Requires OgcApiFeatureCoreProviders providers) {
        super(providers);
        apiCollectionMap = new HashMap<>();
    };

    @Override
    public Set<String> getValues(OgcApiApiDataV2 apiData) {
        if (!apiCollectionMap.containsKey(apiData.getId())) {
            apiCollectionMap.put(apiData.getId(), apiData.getCollections().values()
                    .stream()
                    .filter(collection -> apiData.isCollectionEnabled(collection.getId()))
                    .filter(collection -> collection.getExtension(TilesConfiguration.class).filter(ExtensionConfiguration::isEnabled).isPresent())
                    .map(FeatureTypeConfiguration::getId)
                    .collect(Collectors.toSet()));
        }

        return apiCollectionMap.get(apiData.getId());
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        return new StringSchema()._enum(ImmutableList.copyOf(getValues(apiData)));
    }

    @Override
    public String getId() {
        return "collectionIdTiles";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
               definitionPath.startsWith("/collections/{collectionId}/tiles");
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, String collectionId) {
        final FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
        final TilesConfiguration tilesConfiguration = collectionData.getExtension(TilesConfiguration.class).orElseThrow(NotFoundException::new);

        return tilesConfiguration.isEnabled() &&
               definitionPath.startsWith("/collections/{collectionId}/tiles");
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }
}

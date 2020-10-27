package de.ii.ldproxy.ogcapi.tiles;


import de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.text.MessageFormat;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class PathParameterCollectionIdTiles extends AbstractPathParameterCollectionId {

    @Override
    public Set<String> getValues(OgcApiDataV2 apiData) {
        if (!apiCollectionMap.containsKey(apiData.hashCode())) {
            apiCollectionMap.put(apiData.hashCode(), apiData.getCollections().values()
                    .stream()
                    .filter(collection -> apiData.isCollectionEnabled(collection.getId()))
                    .filter(collection -> collection.getExtension(TilesConfiguration.class).filter(ExtensionConfiguration::isEnabled).isPresent())
                    .map(FeatureTypeConfiguration::getId)
                    .collect(Collectors.toSet()));
        }

        return apiCollectionMap.get(apiData.hashCode());
    }

    @Override
    public String getId() {
        return "collectionIdTiles";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
               definitionPath.startsWith("/collections/{collectionId}/tiles");
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId) {
        final FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
        final TilesConfiguration tilesConfiguration = collectionData.getExtension(TilesConfiguration.class)
                .orElseThrow(() -> new RuntimeException(MessageFormat.format("Could not access tiles configuration for API ''{0}'' and collection ''{1}''.", apiData.getId(), collectionId)));

        return tilesConfiguration.isEnabled() &&
               definitionPath.startsWith("/collections/{collectionId}/tiles");
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }
}

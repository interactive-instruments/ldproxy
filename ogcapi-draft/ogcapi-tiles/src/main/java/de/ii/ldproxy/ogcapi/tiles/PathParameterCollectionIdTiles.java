package de.ii.ldproxy.ogcapi.tiles;


import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.app.PathParameterCollectionIdFeatures;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class PathParameterCollectionIdTiles extends PathParameterCollectionIdFeatures {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterCollectionIdTiles.class);
    Map<String,Set<String>> apiCollectionMap;

    public PathParameterCollectionIdTiles(@Requires FeaturesCoreProviders providers) {
        super(providers);
        apiCollectionMap = new HashMap<>();
    };

    @Override
    public Set<String> getValues(OgcApiDataV2 apiData) {
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
    public Schema getSchema(OgcApiDataV2 apiData) {
        return new StringSchema()._enum(ImmutableList.copyOf(getValues(apiData)));
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

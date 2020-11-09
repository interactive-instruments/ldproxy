package de.ii.ldproxy.ogcapi.tiles.tileMatrixSet;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.tiles.TilesConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class PathParameterTileMatrixSetId implements OgcApiPathParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterTileMatrixSetId.class);
    public static final String TMS_REGEX = "\\w+";

    private final ExtensionRegistry extensionRegistry;
    final FeaturesCoreProviders providers;
    final FeatureProcessInfo featureProcessInfo;
    protected ConcurrentMap<Integer, Schema> schemaMap = new ConcurrentHashMap<>();

    public PathParameterTileMatrixSetId(@Requires ExtensionRegistry extensionRegistry,
                                        @Requires FeaturesCoreProviders providers,
                                        @Requires FeatureProcessInfo featureProcessInfo) {
        this.extensionRegistry = extensionRegistry;
        this.providers = providers;
        this.featureProcessInfo = featureProcessInfo;
    };

    @Override
    public String getPattern() {
        return TMS_REGEX;
    }

    @Override
    public Set<String> getValues(OgcApiDataV2 apiData) {
        Set<String> tmsSetMultiCollection = apiData.getExtension(TilesConfiguration.class)
                .filter(TilesConfiguration::isEnabled)
                .filter(TilesConfiguration::getMultiCollectionEnabled)
                .map(TilesConfiguration::getZoomLevels)
                .map(Map::keySet)
                .orElse(ImmutableSet.of());

        Set<String> tmsSet = apiData.getCollections()
                .values()
                .stream()
                .filter(collection -> apiData.isCollectionEnabled(collection.getId()))
                .map(collection -> collection.getExtension(TilesConfiguration.class))
                .filter(config -> config.filter(ExtensionConfiguration::isEnabled).isPresent())
                .map(config -> config.get().getZoomLevels().keySet())
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        tmsSet.addAll(tmsSetMultiCollection);

        return extensionRegistry.getExtensionsForType(TileMatrixSet.class).stream()
                                                                          .map(tms -> tms.getId())
                                                                          .filter(tms -> tmsSet.contains(tms))
                                                                          .collect(Collectors.toSet());
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        if (!schemaMap.containsKey(apiData.hashCode())) {
            schemaMap.put(apiData.hashCode(),new StringSchema()._enum(ImmutableList.copyOf(getValues(apiData))));
        }

        return schemaMap.get(apiData.hashCode());
    }

    @Override
    public String getName() {
        return "tileMatrixSetId";
    }

    @Override
    public String getDescription() {
        return "The local identifier of a tile matrix set, unique within the API.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId) {
        if (isApplicable(apiData, definitionPath))
            return false;

        Optional<TilesConfiguration> collectionConfig = apiData.getCollections().get(collectionId).getExtension(TilesConfiguration.class);
        if (collectionConfig.isPresent())
            return collectionConfig.get().isEnabled();

        return true;
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
                (definitionPath.startsWith("/tileMatrixSets/{tileMatrixSetId}") ||
                 definitionPath.startsWith("/collections/{collectionId}/tiles/{tileMatrixSetId}") ||
                 definitionPath.startsWith("/tiles/{tileMatrixSetId}"));
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }
}

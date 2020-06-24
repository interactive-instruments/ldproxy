package de.ii.ldproxy.ogcapi.tiles.tileMatrixSet;


import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.tiles.TilesConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class PathParameterTileMatrixSetId implements OgcApiPathParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterTileMatrixSetId.class);
    public static final String TMS_REGEX = "\\w+";

    private final OgcApiExtensionRegistry extensionRegistry;
    final OgcApiFeatureCoreProviders providers;
    final FeatureProcessInfo featureProcessInfo;

    public PathParameterTileMatrixSetId(@Requires OgcApiExtensionRegistry extensionRegistry,
                                        @Requires OgcApiFeatureCoreProviders providers,
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
    public Set<String> getValues(OgcApiApiDataV2 apiData) {
        return extensionRegistry.getExtensionsForType(TileMatrixSet.class).stream()
                                                                          .map(tms -> tms.getId())
                                                                          .collect(Collectors.toSet());
    }

    private Schema schema = null;

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        if (schema==null) {
            schema = new StringSchema()._enum(ImmutableList.copyOf(getValues(apiData)));
        }

        return schema;
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
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, String collectionId) {
        if (isApplicable(apiData, definitionPath))
            return false;

        Optional<TilesConfiguration> collectionConfig = apiData.getCollections().get(collectionId).getExtension(TilesConfiguration.class);
        if (collectionConfig.isPresent())
            return collectionConfig.get().getEnabled();

        return true;
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
                (definitionPath.startsWith("/tileMatrixSets/{tileMatrixSetId}") ||
                 definitionPath.startsWith("/collections/{collectionId}/tiles/{tileMatrixSetId}") ||
                 definitionPath.startsWith("/tiles/{tileMatrixSetId}"));
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }
}

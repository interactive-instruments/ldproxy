package de.ii.ldproxy.ogcapi.tiles;


import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessInfo;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class PathParameterTileMatrixSetId implements OgcApiPathParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterTileMatrixSetId.class);
    static final String TMS_REGEX = "\\w+";

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
    public boolean getExplodeInOpenApi() {
        return false;
    }

    @Override
    public Set<String> getValues(OgcApiApiDataV2 apiData) {
        return extensionRegistry.getExtensionsForType(TileMatrixSet.class).stream()
                                                                          .map(tms -> tms.getId())
                                                                          .collect(Collectors.toSet());
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        return new StringSchema()._enum(ImmutableList.copyOf(getValues(apiData)));
    }

    @Override
    public String getName() {
        return "tileMatrixSetId";
    }

    @Override
    public String getDescription() {
        return "The local identifier of a tiling scheme.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
                (definitionPath.equals("/tileMatrixSets/{tileMatrixSetId}") ||
                 definitionPath.equals("/tileMatrixSets"));
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return true;
    }
}

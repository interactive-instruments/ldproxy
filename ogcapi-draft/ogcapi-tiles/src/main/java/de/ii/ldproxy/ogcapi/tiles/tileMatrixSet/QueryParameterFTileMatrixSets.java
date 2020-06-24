package de.ii.ldproxy.ogcapi.tiles.tileMatrixSet;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.tiles.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSetsFormatExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
public class QueryParameterFTileMatrixSets extends QueryParameterF {

    public QueryParameterFTileMatrixSets(@Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "fTileMatrixSets";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return super.isApplicable(apiData, definitionPath, method) &&
                (definitionPath.equals("/tileMatrixSets") ||
                 definitionPath.equals("/tileMatrixSets/{tileMatrixSetId}"));
    }

    @Override
    protected Class<? extends FormatExtension> getFormatClass() {
        return TileMatrixSetsFormatExtension.class;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

}

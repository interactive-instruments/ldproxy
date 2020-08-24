package de.ii.ldproxy.ogcapi.tiles;

import de.ii.ldproxy.ogcapi.common.domain.QueryParameterF;
import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
public class QueryParameterFTileSets extends QueryParameterF {

    protected QueryParameterFTileSets(@Requires ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "fTileSets";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return super.isApplicable(apiData, definitionPath, method) &&
               definitionPath.endsWith("/tiles");
    }

    @Override
    protected Class<? extends FormatExtension> getFormatClass() {
        return TileSetsFormatExtension.class;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }
}

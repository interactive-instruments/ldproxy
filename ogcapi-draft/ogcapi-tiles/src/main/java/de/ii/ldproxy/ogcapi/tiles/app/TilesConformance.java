package de.ii.ldproxy.ogcapi.tiles.app;

import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;

import java.util.List;

public class TilesConformance implements ConformanceClass {

    @Override
    public List<String> getConformanceClassUris() {
        return null;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }
}

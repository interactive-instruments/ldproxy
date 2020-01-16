package de.ii.ldproxy.ogcapi.tiles;

import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Optional;

@Component
@Provides
@Instantiate
public class CollectionMultiTiles implements ConformanceClass {

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/req/multitiles";
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        Optional<TilesConfiguration> extension = getExtensionConfiguration(apiData, TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::getEnabled)
                .filter(TilesConfiguration::getMultiTilesEnabled)
                .isPresent();
    }
}

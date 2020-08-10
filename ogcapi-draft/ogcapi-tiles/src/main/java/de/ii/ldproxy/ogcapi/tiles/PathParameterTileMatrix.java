package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Component
@Provides
@Instantiate
public class PathParameterTileMatrix implements OgcApiPathParameter {

    @Requires
    OgcApiExtensionRegistry extensionRegistry;

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterTileMatrix.class);

    @Override
    public String getPattern() {
        return "\\d+";
    }

    @Override
    public Set<String> getValues(OgcApiApiDataV2 apiData) {
        return ImmutableSet.of();
    }

    private Schema schema = new StringSchema().pattern(getPattern());

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        return schema;
    }

    @Override
    public String getName() {
        return "tileMatrix";
    }

    @Override
    public String getDescription() {
        return "Zoom level of the tile. See http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ for more information about Level, Row and Column in the Google Maps tiling scheme (WebMercatorQuad). " +
                "Example: In the WebMercatorQuad tiling scheme Ireland is fully within the tile with the following values: Level 5, Row 10 and Col 15";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
               (definitionPath.equals("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}") ||
                definitionPath.equals("/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"));
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }
}

package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Set;

@Component
@Provides
@Instantiate
public class PathParameterTileRow implements OgcApiPathParameter {

    @Requires
    OgcApiExtensionRegistry extensionRegistry;

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterTileRow.class);

    @Override
    public String getPattern() {
        return "\\d+";
    }

    @Override
    public Set<String> getValues(OgcApiApiDataV2 apiData) {
        return ImmutableSet.of();
    }

    private Schema schema = new IntegerSchema().minimum(BigDecimal.ZERO);

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        return schema;
    }

    @Override
    public String getName() {
        return "tileRow";
    }

    @Override
    public String getDescription() {
        return "Row index of the tile on the selected zoom level. See http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ for more information about Level, Row and Column in the Google Maps tiling scheme (WebMercatorQuad). " +
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

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), TilesConfiguration.class);
    }
}

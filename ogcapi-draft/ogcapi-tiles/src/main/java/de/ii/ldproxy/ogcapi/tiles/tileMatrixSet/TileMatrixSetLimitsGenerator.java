package de.ii.ldproxy.ogcapi.tiles.tileMatrixSet;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.tiles.MinMax;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;

import java.util.List;

public interface TileMatrixSetLimitsGenerator {

    List<TileMatrixSetLimits> getCollectionTileMatrixSetLimits(OgcApiApiDataV2 data, String collectionId,
                                                               TileMatrixSet tileMatrixSet, MinMax tileMatrixRange,
                                                               CrsTransformerFactory crsTransformation);

    List<TileMatrixSetLimits> getTileMatrixSetLimits(OgcApiApiDataV2 data, TileMatrixSet tileMatrixSet,
                                                     MinMax tileMatrixRange, CrsTransformerFactory crsTransformerFactory);
}

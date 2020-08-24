package de.ii.ldproxy.ogcapi.tiles.tileMatrixSet;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.tiles.MinMax;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;

import java.util.List;

public interface TileMatrixSetLimitsGenerator {

    List<TileMatrixSetLimits> getCollectionTileMatrixSetLimits(OgcApiDataV2 data, String collectionId,
                                                               TileMatrixSet tileMatrixSet, MinMax tileMatrixRange,
                                                               CrsTransformerFactory crsTransformation);

    List<TileMatrixSetLimits> getTileMatrixSetLimits(OgcApiDataV2 data, TileMatrixSet tileMatrixSet,
                                                     MinMax tileMatrixRange, CrsTransformerFactory crsTransformerFactory);
}

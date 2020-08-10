/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiBuildingBlock;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides
@Instantiate
public class OgcApiCapabilityVectorTiles implements OgcApiBuildingBlock {

    private static final int LIMIT_DEFAULT = 100000;
    private static final int MAX_POLYGON_PER_TILE_DEFAULT = 10000;
    private static final int MAX_LINE_STRING_PER_TILE_DEFAULT = 10000;
    private static final int MAX_POINT_PER_TILE_DEFAULT = 10000;

    @Override
    public ExtensionConfiguration.Builder getConfigurationBuilder() {
        return new ImmutableTilesConfiguration.Builder();
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {

        return new ImmutableTilesConfiguration.Builder().enabled(false)
                                                        .limit(LIMIT_DEFAULT)
                                                        .maxPolygonPerTileDefault(MAX_POLYGON_PER_TILE_DEFAULT)
                                                        .maxLineStringPerTileDefault(MAX_LINE_STRING_PER_TILE_DEFAULT)
                                                        .maxPointPerTileDefault(MAX_POINT_PER_TILE_DEFAULT)
                                                        .multiCollectionEnabled(false)
                                                        //TODO: is this still correct?
                                                        .zoomLevels(ImmutableMap.of("WebMercatorQuad", new ImmutableMinMax.Builder()
                                                                .min(0)
                                                                .max(23)
                                                                .build()))
                                                        .build();
    }

}

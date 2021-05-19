/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import de.ii.ldproxy.ogcapi.common.domain.QueryParameterF;
import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
public class QueryParameterFTileSet extends QueryParameterF {

    protected QueryParameterFTileSet(@Requires ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "fTileSet";
    }

    @Override
    protected boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return definitionPath.endsWith("/tiles/{tileMatrixSetId}");
    }

    @Override
    protected Class<? extends FormatExtension> getFormatClass() {
        return TileSetFormatExtension.class;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }
}

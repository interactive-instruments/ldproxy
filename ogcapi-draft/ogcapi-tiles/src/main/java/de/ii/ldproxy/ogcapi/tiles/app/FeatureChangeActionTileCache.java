/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionDynamicMetadataRegistry;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionMetadataExtentSpatial;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionMetadataExtentTemporal;
import de.ii.ldproxy.ogcapi.common.domain.metadata.MetadataType;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.ChangeContext;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.FeatureChangeAction;
import de.ii.ldproxy.ogcapi.tiles.domain.TileCache;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Consumer;

@Component
@Provides
@Instantiate
public class FeatureChangeActionTileCache implements FeatureChangeAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureChangeActionTileCache.class);
    private final TileCache tileCache;

    public FeatureChangeActionTileCache(@Requires TileCache tileCache) {
        this.tileCache = tileCache;
    }

    @Override
    public FeatureChangeActionTileCache create() {
        return new FeatureChangeActionTileCache(tileCache);
    }

    @Override
    public int getSortPriority() {
        return 30;
    }

    @Override
    public void onInsert(ChangeContext changeContext, Consumer<ChangeContext> next) {
        changeContext = onChange(changeContext);

        // next chain for extensions
        next.accept(changeContext);
    }

    @Override
    public void onUpdate(ChangeContext changeContext, Consumer<ChangeContext> next) {
        changeContext = onChange(changeContext);

        // next chain for extensions
        next.accept(changeContext);
    }

    @Override
    public void onDelete(ChangeContext changeContext, Consumer<ChangeContext> next) {
        changeContext = onChange(changeContext);

        // next chain for extensions
        next.accept(changeContext);
    }

    private ChangeContext onChange(ChangeContext changeContext) {
        changeContext.getBoundingBox()
                     .ifPresent(bbox -> {
                         try {
                             tileCache.deleteTiles(changeContext.getApiData(),
                                                   Optional.of(changeContext.getCollectionId()),
                                                   Optional.empty(),
                                                   Optional.of(bbox));
                         } catch (IOException | SQLException e) {
                             LOGGER.debug("Could not delete tiles from the cache of API '{}', collection '{}', bounding box ({} {},{} {}): {}",
                                          changeContext.getApiData().getId(), changeContext.getCollectionId(),
                                          bbox.getXmin(), bbox.getYmin(), bbox.getXmax(), bbox.getYmax(), e.getMessage());
                             // ignore
                         }
                     });

        return changeContext;
    }
}

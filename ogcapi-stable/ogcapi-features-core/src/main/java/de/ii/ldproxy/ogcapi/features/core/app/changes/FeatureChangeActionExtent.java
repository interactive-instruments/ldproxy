/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app.changes;

import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionMetadataExtentSpatial;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionMetadataExtentTemporal;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.TemporalExtent;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.ChangeContext;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.FeatureChangeAction;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionDynamicMetadataRegistry;
import de.ii.ldproxy.ogcapi.common.domain.metadata.MetadataType;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.ImmutableChangeContext;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Consumer;

@Component
@Provides
@Instantiate
public class FeatureChangeActionExtent implements FeatureChangeAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureChangeActionExtent.class);
    private final CollectionDynamicMetadataRegistry metadataRegistry;

    public FeatureChangeActionExtent(@Requires CollectionDynamicMetadataRegistry collectionDynamicMetadataRegistry) {
        this.metadataRegistry = collectionDynamicMetadataRegistry;
    }

    @Override
    public FeatureChangeActionExtent create() {
        return new FeatureChangeActionExtent(metadataRegistry);
    }

    @Override
    public int getSortPriority() {
        return 10;
    }

    // TODO for SQL feature providers there is also a cached extent in xtraplatform;
    //      recomputing the extent for every change will often be too expensive;
    //      how to consolidate?

    @Override
    public void onInsert(ChangeContext changeContext, Consumer<ChangeContext> next) {
        changeContext = onInsertOrUpdate(changeContext);

        // next chain for extensions
        next.accept(changeContext);
    }

    @Override
    public void onUpdate(ChangeContext changeContext, Consumer<ChangeContext> next) {
        changeContext = onInsertOrUpdate(changeContext);

        // next chain for extensions
        next.accept(changeContext);
    }

    private ChangeContext onInsertOrUpdate(ChangeContext changeContext) {
        String apiId = changeContext.getApiData().getId();
        String collectionId = changeContext.getCollectionId();

        changeContext.getBoundingBox()
                     .ifPresent(bbox -> metadataRegistry.update(apiId, collectionId, MetadataType.spatialExtent, CollectionMetadataExtentSpatial.of(bbox)));

        changeContext.getInterval()
                     .ifPresent(interval -> metadataRegistry.update(apiId, collectionId, MetadataType.temporalExtent, CollectionMetadataExtentTemporal.of(interval)));

        return changeContext;
    }
}

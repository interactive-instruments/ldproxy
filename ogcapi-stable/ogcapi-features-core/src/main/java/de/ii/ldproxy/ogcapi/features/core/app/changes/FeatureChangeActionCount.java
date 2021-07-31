/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app.changes;

import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionDynamicMetadataRegistry;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionMetadataCount;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionMetadataLastModified;
import de.ii.ldproxy.ogcapi.common.domain.metadata.MetadataType;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.ChangeContext;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.FeatureChangeAction;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

@Component
@Provides
@Instantiate
public class FeatureChangeActionCount implements FeatureChangeAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureChangeActionCount.class);
    private final CollectionDynamicMetadataRegistry metadataRegistry;

    public FeatureChangeActionCount(@Requires CollectionDynamicMetadataRegistry collectionDynamicMetadataRegistry) {
        this.metadataRegistry = collectionDynamicMetadataRegistry;
    }

    @Override
    public FeatureChangeActionCount create() {
        return new FeatureChangeActionCount(metadataRegistry);
    }

    @Override
    public int getSortPriority() {
        return 40;
    }

    @Override
    public void onInsert(ChangeContext changeContext, Consumer<ChangeContext> next) {
        metadataRegistry.update(changeContext.getApiData().getId(),
                                changeContext.getCollectionId(),
                                MetadataType.count, CollectionMetadataCount.of(+1));

        // next chain for extensions
        next.accept(changeContext);
    }

    @Override
    public void onDelete(ChangeContext changeContext, Consumer<ChangeContext> next) {
        metadataRegistry.update(changeContext.getApiData().getId(),
                                changeContext.getCollectionId(),
                                MetadataType.count, CollectionMetadataCount.of(-1));

        // next chain for extensions
        next.accept(changeContext);
    }
}

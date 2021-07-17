/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app.changes;

import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionMetadataLastModified;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.ChangeContext;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.FeatureChangeAction;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionDynamicMetadataRegistry;
import de.ii.ldproxy.ogcapi.common.domain.metadata.MetadataType;
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
public class FeatureChangeActionLastModified implements FeatureChangeAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureChangeActionLastModified.class);
    private final CollectionDynamicMetadataRegistry metadataRegistry;

    public FeatureChangeActionLastModified(@Requires CollectionDynamicMetadataRegistry collectionDynamicMetadataRegistry) {
        this.metadataRegistry = collectionDynamicMetadataRegistry;
    }

    @Override
    public FeatureChangeActionLastModified create() {
        return new FeatureChangeActionLastModified(metadataRegistry);
    }

    @Override
    public int getSortPriority() {
        return 20;
    }

    @Override
    public void onInsert(ChangeContext changeContext, Consumer<ChangeContext> next) {
        onChange(changeContext);

        // next chain for extensions
        next.accept(changeContext);
    }

    @Override
    public void onUpdate(ChangeContext changeContext, Consumer<ChangeContext> next) {
        onChange(changeContext);

        // next chain for extensions
        next.accept(changeContext);
    }

    @Override
    public void onDelete(ChangeContext changeContext, Consumer<ChangeContext> next) {
        onChange(changeContext);

        // next chain for extensions
        next.accept(changeContext);
    }

    private void onChange(ChangeContext changeContext) {
        String apiId = changeContext.getApiData().getId();
        String collectionId = changeContext.getCollectionId();
        metadataRegistry.update(apiId, collectionId, MetadataType.lastModified, CollectionMetadataLastModified.of(changeContext.getModified()));
    }
}
